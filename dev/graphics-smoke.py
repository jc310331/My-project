"""Check a real software GLX context on a disposable X display (not Minecraft)."""
import ctypes as c
import os
x = c.CDLL('libX11.so.6')
gl = c.CDLL('libGL.so.1')
x.XOpenDisplay.argtypes = [c.c_char_p]; x.XOpenDisplay.restype = c.c_void_p
x.XDefaultScreen.argtypes = [c.c_void_p]; x.XDefaultScreen.restype = c.c_int
x.XRootWindow.argtypes = [c.c_void_p, c.c_int]; x.XRootWindow.restype = c.c_ulong
x.XCloseDisplay.argtypes = [c.c_void_p]
x.XFree.argtypes = [c.c_void_p]
gl.glXChooseVisual.argtypes = [c.c_void_p, c.c_int, c.POINTER(c.c_int)]; gl.glXChooseVisual.restype = c.c_void_p
gl.glXCreateContext.argtypes = [c.c_void_p, c.c_void_p, c.c_void_p, c.c_int]; gl.glXCreateContext.restype = c.c_void_p
gl.glXCreateGLXPixmap.argtypes = [c.c_void_p, c.c_void_p, c.c_ulong]; gl.glXCreateGLXPixmap.restype = c.c_ulong
gl.glXMakeCurrent.argtypes = [c.c_void_p, c.c_ulong, c.c_void_p]; gl.glXMakeCurrent.restype = c.c_int
gl.glXDestroyContext.argtypes = [c.c_void_p, c.c_void_p]
gl.glXDestroyGLXPixmap.argtypes = [c.c_void_p, c.c_ulong]
gl.glGetString.argtypes = [c.c_uint]; gl.glGetString.restype = c.c_char_p
x.XCreatePixmap.argtypes = [c.c_void_p, c.c_ulong, c.c_uint, c.c_uint, c.c_uint]; x.XCreatePixmap.restype = c.c_ulong
x.XFreePixmap.argtypes = [c.c_void_p, c.c_ulong]
display = x.XOpenDisplay(os.environ.get('DISPLAY', ':99').encode())
if not display: raise RuntimeError('Cannot open X display')
visual = context = pixmap = drawable = None
try:
    screen = x.XDefaultScreen(display)
    # GLX_RGBA, GLX_RED_SIZE, GLX_GREEN_SIZE, GLX_BLUE_SIZE, terminator.
    visual = gl.glXChooseVisual(display, screen, (c.c_int * 8)(4, 8, 8, 9, 8, 10, 8, 0))
    if not visual: raise RuntimeError('No RGBA GLX visual')
    context = gl.glXCreateContext(display, visual, None, 1)
    if not context: raise RuntimeError('No GLX context')
    pixmap = x.XCreatePixmap(display, x.XRootWindow(display, screen), 64, 64, 24)
    drawable = gl.glXCreateGLXPixmap(display, visual, pixmap)
    if not gl.glXMakeCurrent(display, drawable, context): raise RuntimeError('Cannot activate GLX context')
    for label, enum in [('vendor', 0x1F00), ('renderer', 0x1F01), ('version', 0x1F02)]:
        value = gl.glGetString(enum)
        if not value: raise RuntimeError('Missing OpenGL ' + label)
        print(label + ': ' + value.decode())
finally:
    gl.glXMakeCurrent(display, 0, None)
    if drawable: gl.glXDestroyGLXPixmap(display, drawable)
    if pixmap: x.XFreePixmap(display, pixmap)
    if context: gl.glXDestroyContext(display, context)
    if visual: x.XFree(visual)
    x.XCloseDisplay(display)
