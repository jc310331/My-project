"""Disposable loopback SOCKS adapter for Minecraft's SOCKS-only proxy option.

Routes only Mojang skin hosts through the environment's existing HTTP CONNECT
proxy, retaining its allowlist and TLS verification. No credentials are accepted.
"""
import asyncio
import os
import ssl
import urllib.parse

HOSTS = {'api.mojang.com', 'sessionserver.mojang.com', 'textures.minecraft.net',
         'api.minecraftservices.com'}

async def handle(reader, writer):
    upstream = None
    try:
        version, count = await asyncio.wait_for(reader.readexactly(2), 5)
        methods = await reader.readexactly(count)
        if version != 5 or 0 not in methods:
            return
        writer.write(b'\x05\x00');await writer.drain()
        version, command, reserved, kind = await reader.readexactly(4)
        if (version, command, reserved, kind) != (5, 1, 0, 3):
            raise ValueError('Only hostname CONNECT is supported')
        length = (await reader.readexactly(1))[0]
        host = (await reader.readexactly(length)).decode('ascii').lower()
        port = int.from_bytes(await reader.readexactly(2), 'big')
        if host not in HOSTS or port not in (80, 443) or (port == 80 and host != 'textures.minecraft.net'):
            raise ValueError('Destination outside disposable skin test scope')
        proxy = urllib.parse.urlsplit(os.environ['HTTPS_PROXY'])
        if proxy.username or proxy.password:
            raise ValueError('Use platform proxy configuration')
        remote, upstream = await asyncio.wait_for(asyncio.open_connection(proxy.hostname, proxy.port or 8080), 10)
        # Legacy signed profiles advertise HTTP texture URLs. Upgrade only that
        # fixed asset host to verified HTTPS; the signed URL/profile is untouched.
        upstream_port = 443
        upstream.write(f'CONNECT {host}:{upstream_port} HTTP/1.1\r\nHost: {host}:{upstream_port}\r\n\r\n'.encode('ascii'))
        await upstream.drain()
        response = await asyncio.wait_for(remote.readuntil(b'\r\n\r\n'), 15)
        if response.split(b' ', 2)[1] != b'200':
            raise ValueError('Platform proxy denied destination')
        if port == 80:
            await upstream.start_tls(ssl.create_default_context(), server_hostname=host)
        writer.write(b'\x05\x00\x00\x01\x7f\x00\x00\x01\x00\x00');await writer.drain()
        async def relay(source, target):
            while data := await source.read(65536):
                target.write(data);await target.drain()
        tasks = [asyncio.create_task(relay(reader, upstream)), asyncio.create_task(relay(remote, writer))]
        _, pending = await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
        for task in pending:task.cancel()
        await asyncio.gather(*tasks, return_exceptions=True)
    except (OSError, ValueError, asyncio.TimeoutError, asyncio.IncompleteReadError):
        try:
            writer.write(b'\x05\x01\x00\x01' + b'\x00' * 6);await writer.drain()
        except OSError:pass
    finally:
        writer.close()
        if upstream:upstream.close()

async def main():
    server = await asyncio.start_server(handle, '127.0.0.1', 1080)
    print('Disposable Minecraft proxy on 127.0.0.1:1080; upstream policy preserved', flush=True)
    async with server:await server.serve_forever()

if __name__ == '__main__':asyncio.run(main())
