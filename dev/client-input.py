"""Send test input only to the disposable Minecraft window on X display :99."""
import argparse
import ctypes as c
import os
import re
import subprocess
import time

parser = argparse.ArgumentParser()
parser.add_argument('--command')
parser.add_argument('--key')
parser.add_argument('--click', nargs=2, type=int, metavar=('X', 'Y'))
args = parser.parse_args()
os.environ['DISPLAY'] = ':99'
tree = subprocess.check_output(['xwininfo', '-display', ':99', '-root', '-tree'], text=True)
match = re.search(r'(0x[0-9a-f]+) "Minecraft[^"\n]*"', tree)
if not match:
    raise SystemExit('Disposable Minecraft window not found')
x = c.CDLL('libX11.so.6'); xt = c.CDLL('libXtst.so.6')
x.XOpenDisplay.argtypes = [c.c_char_p]; x.XOpenDisplay.restype = c.c_void_p
x.XSetInputFocus.argtypes = [c.c_void_p, c.c_ulong, c.c_int, c.c_ulong]
x.XStringToKeysym.argtypes = [c.c_char_p]; x.XStringToKeysym.restype = c.c_ulong
x.XKeysymToKeycode.argtypes = [c.c_void_p, c.c_ulong]; x.XKeysymToKeycode.restype = c.c_uint
x.XFlush.argtypes = [c.c_void_p]; x.XCloseDisplay.argtypes = [c.c_void_p]
xt.XTestFakeKeyEvent.argtypes = [c.c_void_p, c.c_uint, c.c_int, c.c_ulong]
xt.XTestFakeMotionEvent.argtypes = [c.c_void_p, c.c_int, c.c_int, c.c_int, c.c_ulong]
xt.XTestFakeButtonEvent.argtypes = [c.c_void_p, c.c_uint, c.c_int, c.c_ulong]
d = x.XOpenDisplay(b':99')
if not d:raise SystemExit('Display unavailable')
x.XSetInputFocus(d, int(match[1], 16), 1, 0);x.XFlush(d)
time.sleep(.1)
names = {' ':'space', '/':'slash', '-':'minus', '_':'underscore', ':':'colon',
         '.':'period', ',':'comma', '=':'equal', '@':'at', '[':'bracketleft',
         ']':'bracketright', '{':'braceleft', '}':'braceright', '"':'quotedbl',
         "'":'apostrophe', '~':'asciitilde', '+':'plus', '!':'exclam', '?':'question',
         '(':'parenleft', ')':'parenright', '\\':'backslash', '|':'bar', '#':'numbersign'}
def key(name, down):
    code = x.XKeysymToKeycode(d, x.XStringToKeysym(name.encode()))
    if not code:raise ValueError('Unknown key ' + name)
    xt.XTestFakeKeyEvent(d, code, down, 0);x.XFlush(d)
def press(name):
    key(name, 1);time.sleep(.035);key(name, 0);time.sleep(.035)
try:
    if args.click:
        xt.XTestFakeMotionEvent(d, -1, *args.click, 0);x.XFlush(d);time.sleep(.1)
        xt.XTestFakeButtonEvent(d, 1, 1, 0);x.XFlush(d);time.sleep(.1)
        xt.XTestFakeButtonEvent(d, 1, 0, 0);x.XFlush(d)
    elif args.command:
        press('slash');time.sleep(.25)
        for char in args.command.removeprefix('/'):
            shift = char.isupper() or char in '~!@#$%^&*()_+{}|:"<>?'
            if shift:key('Shift_L', 1)
            press(names.get(char, char.lower()))
            if shift:key('Shift_L', 0)
        press('Return')
    elif args.key:
        press(args.key)
    else:parser.error('Provide --command or --key')
finally:
    x.XCloseDisplay(d)
