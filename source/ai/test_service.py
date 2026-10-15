import json
from pathlib import Path
import tempfile
import threading
import unittest
import urllib.request
import urllib.error
import uuid
from service import Planner, Server, validate_plan, provider_opener

class PlannerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.path = Path(self.temp.name) / 'memory.db'
        self.planner = Planner(self.path)
        self.request = dict(world=str(uuid.uuid4()), owner=str(uuid.uuid4()), entity=str(uuid.uuid4()), text='follow me')
    def tearDown(self):
        self.temp.cleanup()
    def test_personal_goal_survives_restart(self):
        self.request.update(text='[autonomy]', observations={'items':{}, 'has_axe':True, 'has_pickaxe':False, 'crafting_table':False})
        self.assertEqual(self.planner.plan(self.request)['action'], 'oak_logs:2')
        other=Planner(self.path)
        self.request['text']='what are your goals?'
        self.assertIn('gather tool materials',other.plan(self.request)['reply'])
        with other.connect() as db:
            self.assertEqual(db.execute('SELECT COUNT(*) FROM personal_goals').fetchone()[0],1)
    def test_no_autonomy_without_observation(self):
        self.request['text']='[autonomy]'
        self.assertEqual(self.planner.plan(self.request)['action'],'none')
    def test_quantity_gather_and_bounds(self):
        for text in ('go get me 15 oak logs', 'get me 15 oak logs, here is an axe', 'gather 15 oak logs'):
            self.request['text']=text
            self.assertEqual(self.planner.plan(self.request)['action'],'oak_logs:15')
        for n in (0,65,999):
            self.request['text']='get me '+str(n)+' oak logs'
            self.assertEqual(self.planner.plan(self.request)['action'],'none')
        for action in ('oak_logs:0','oak_logs:65','oak_logs:15;op @a'):
            with self.assertRaises(ValueError):validate_plan({'action':action,'reply':'bad'})
    def test_personal_crafting_and_readiness(self):
        self.request.update(text='[autonomy]',observations={'items':{'minecraft:oak_log':2},'has_axe':False,'has_pickaxe':False,'crafting_table':True})
        self.assertEqual(self.planner.plan(self.request)['action'],'craft_kit')
        self.request['observations'].update(has_axe=True,has_pickaxe=True,items={'minecraft:oak_log':8})
        self.assertEqual(self.planner.plan(self.request)['action'],'none')
        self.assertIn('ready',self.planner.plan(self.request)['reply'])
    def test_acknowledgements_are_scoped_idempotent_and_reserve_player_logs(self):
        job={'id':str(uuid.uuid4()),'state':'completed','summary':'Oak logs 15/15 completed','personal':False}
        self.request.update(text='[autonomy]',observations={'job':job,'items':{'minecraft:oak_log':15},'crafting_table':True})
        for _ in range(2):self.assertEqual(self.planner.plan(self.request)['action'],'none')
        with self.planner.connect() as db:
            self.assertEqual(db.execute('SELECT COUNT(*) FROM action_results').fetchone()[0],1)
        job.update(id=str(uuid.uuid4()),state='blocked',summary='Need an axe')
        self.assertIn('Need an axe',self.planner.plan(self.request)['reply'])
    def test_bad_observations_and_low_health(self):
        self.request.update(text='[autonomy]',observations={'items':{'minecraft:oak_log':-2}})
        with self.assertRaises(ValueError):self.planner.plan(self.request)
        self.request['observations']={'health':3,'has_axe':True,'items':{}}
        self.assertEqual(self.planner.plan(self.request)['action'],'none')
    def test_intents(self):
        for text, action in [('follow me','follow'), ('stay here','stay'), ('set home','home'), ('collect items','collect'), ('explore','explore')]:
            self.request['text'] = text
            self.assertEqual(self.planner.plan(self.request)['action'], action)
    def test_memory_survives_restart(self):
        self.request['text'] = 'remember Jacob likes building'
        self.planner.plan(self.request)
        self.request['text'] = 'what do you remember?'
        self.assertIn('Jacob likes building', Planner(self.path).plan(self.request)['reply'])
    def test_world_isolation(self):
        self.request['text'] = 'remember private fact'
        self.planner.plan(self.request)
        self.request.update(world=str(uuid.uuid4()), text='memories')
        self.assertNotIn('private fact', self.planner.plan(self.request)['reply'])
    def test_conversation_topics_do_not_start_goals(self):
        for text in ('Hello, Socrates.', 'What can you help with?', 'What should we do next?', 'Why are you called Socrates?'):
            self.request['text'] = text
            result = self.planner.plan(self.request)
            self.assertEqual(result['action'], 'none')
            self.assertNotIn('Offline conversation:', result['reply'])
        with self.planner.connect() as db:
            self.assertEqual(db.execute('SELECT COUNT(*) FROM goals').fetchone()[0], 0)
        self.request['text'] = 'What can you help with?'
        self.assertIn('General mining', self.planner.plan(self.request)['reply'])
    def test_unknown_action_rejected(self):
        with self.assertRaises(ValueError): validate_plan({'action':'op @a','reply':'okay'})
    def test_extra_parameters_rejected(self):
        with self.assertRaises(ValueError): validate_plan({'action':'stay','reply':'okay','command':'kill'})
    def test_long_reply_rejected(self):
        with self.assertRaises(ValueError): validate_plan({'action':'none','reply':'a'*601})
    def test_invalid_scope(self):
        self.request['world'] = '../../path'
        with self.assertRaises(ValueError): self.planner.plan(self.request)
    def test_unsupported_task_is_not_claimed(self):
        self.request['text'] = 'build me a mansion'
        self.assertEqual(self.planner.plan(self.request)['action'], 'none')
    def test_cloud_output_allowlist(self):
        class Response:
            def __enter__(self):return self
            def __exit__(self,*args):pass
            def read(self,n):return json.dumps({'output':[{'content':[{'type':'output_text','text':'{"action":"execute","reply":"bad"}'}]}]}).encode()
        from unittest.mock import patch
        with patch.dict('os.environ',{'OPENAI_API_KEY':'test-not-real'}), patch('service.provider_opener') as opener:
            opener.return_value.open.return_value = Response()
            with self.assertRaises(ValueError):Planner(self.path,'openai','test-model').cloud('test',[])

class HttpTests(unittest.TestCase):
    def setUp(self):
        PlannerTests.setUp(self)
        self.server = Server(('127.0.0.1', 0), 'a'*64, self.planner)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.url = 'http://127.0.0.1:' + str(self.server.server_port) + '/v1/plan'
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    def tearDown(self):
        self.server.shutdown();self.server.server_close();self.thread.join()
        PlannerTests.tearDown(self)
    def send(self, token='a'*64, extra=None, raw=None):
        headers={'Content-Type':'application/json','Authorization':'Bearer '+token}
        headers.update(extra or {})
        req=urllib.request.Request(self.url,raw if raw is not None else json.dumps(self.request).encode(),headers)
        return self.opener.open(req, timeout=3)
    def test_http_valid(self):
        with self.send() as r:self.assertEqual(json.load(r)['action'],'follow')
    def test_http_wrong_token(self):
        with self.assertRaises(urllib.error.HTTPError) as c:self.send('wrong')
        self.assertEqual(c.exception.code,403)
    def test_http_non_ascii_authorization(self):
        with self.assertRaises(urllib.error.HTTPError) as c:self.send('wrong-\u00e9')
        self.assertEqual(c.exception.code,403)
        with self.send() as r:self.assertEqual(json.load(r)['action'],'follow')
    def test_http_browser_origin(self):
        with self.assertRaises(urllib.error.HTTPError) as c:self.send(extra={'Origin':'http://example.com'})
        self.assertEqual(c.exception.code,403)
    def test_http_oversize(self):
        with self.assertRaises(urllib.error.HTTPError) as c:self.send(raw=b'x'*20000)
        self.assertEqual(c.exception.code,413)
    def test_http_malformed(self):
        with self.assertRaises(urllib.error.HTTPError) as c:self.send(raw=b'{bad')
        self.assertEqual(c.exception.code,400)

class ProviderTransportTests(unittest.TestCase):
    def test_redirect_does_not_forward_credentials(self):
        from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
        from unittest.mock import patch
        for code in (301, 302, 303, 307, 308):
            with self.subTest(code=code):
                received = []
                class Redirect(BaseHTTPRequestHandler):
                    def log_message(self, *_):pass
                    def do_POST(self):
                        received.append((self.path, self.headers.get('Authorization')))
                        self.send_response(code)
                        self.send_header('Location', '/credential-sink')
                        self.send_header('Content-Length', '0')
                        self.end_headers()
                    def do_GET(self):
                        received.append((self.path, self.headers.get('Authorization')))
                        self.send_response(200);self.end_headers()
                server = ThreadingHTTPServer(('127.0.0.1', 0), Redirect)
                thread = threading.Thread(target=server.serve_forever, daemon=True)
                thread.start()
                try:
                    request = urllib.request.Request('http://127.0.0.1:' + str(server.server_port) + '/provider', b'{}', {'Authorization':'Bearer fake-regression-token'})
                    with patch.dict('os.environ', {'no_proxy':'127.0.0.1','NO_PROXY':'127.0.0.1'}):
                        with self.assertRaises(urllib.error.HTTPError) as error:
                            provider_opener().open(request, timeout=3)
                    self.assertEqual(error.exception.code, code)
                    self.assertEqual(received, [('/provider', 'Bearer fake-regression-token')])
                finally:
                    server.shutdown();server.server_close();thread.join()

if __name__=='__main__':unittest.main()
