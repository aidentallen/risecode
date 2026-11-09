#!/usr/bin/env python3
"""Record only the PID-matched disposable UX client's X11 window, never the desktop."""
import argparse
import ctypes as c
import ctypes.util
import datetime
import json
import os
from pathlib import Path
import subprocess
import time


def find_window(pid):
    command = Path(f'/proc/{pid}/cmdline').read_bytes().split(b'\0')
    expected = str(Path(__file__).resolve().parents[1]).encode()
    if not any(expected in arg for arg in command) or not any(
        arg in (b'-Driserecode.uxPerformanceOnly=true', b'-Driserecode.uxInteractiveOnly=true', b'-Driserecode.uxFollowupOnly=true') for arg in command
    ):
        raise RuntimeError('PID is not this checkout\'s disposable UX test client')
    x = c.CDLL(ctypes.util.find_library('X11'))
    window, atom = c.c_ulong, c.c_ulong
    x.XOpenDisplay.argtypes = [c.c_char_p]; x.XOpenDisplay.restype = c.c_void_p
    x.XDefaultRootWindow.argtypes = [c.c_void_p]; x.XDefaultRootWindow.restype = window
    x.XInternAtom.argtypes = [c.c_void_p, c.c_char_p, c.c_int]; x.XInternAtom.restype = atom
    x.XQueryTree.argtypes = [c.c_void_p, window, c.POINTER(window), c.POINTER(window), c.POINTER(c.POINTER(window)), c.POINTER(c.c_uint)]
    x.XGetWindowProperty.argtypes = [c.c_void_p, window, atom, c.c_long, c.c_long, c.c_int, atom, c.POINTER(atom), c.POINTER(c.c_int), c.POINTER(c.c_ulong), c.POINTER(c.c_ulong), c.POINTER(c.POINTER(c.c_ubyte))]
    x.XGetGeometry.argtypes = [c.c_void_p, window, c.POINTER(window), c.POINTER(c.c_int), c.POINTER(c.c_int), c.POINTER(c.c_uint), c.POINTER(c.c_uint), c.POINTER(c.c_uint), c.POINTER(c.c_uint)]
    x.XFree.argtypes = [c.c_void_p]; x.XCloseDisplay.argtypes = [c.c_void_p]
    display = x.XOpenDisplay(None)
    if not display:
        raise RuntimeError('No X11 display; desktop capture is not a fallback')
    matches = []
    try:
        pid_atom = x.XInternAtom(display, b'_NET_WM_PID', False)
        pending = [x.XDefaultRootWindow(display)]
        visited = 0
        while pending and visited < 10000:
            wid = pending.pop(); visited += 1
            actual, fmt, count, remaining, data = atom(), c.c_int(), c.c_ulong(), c.c_ulong(), c.POINTER(c.c_ubyte)()
            result = x.XGetWindowProperty(display, wid, pid_atom, 0, 1, False, 0, c.byref(actual), c.byref(fmt), c.byref(count), c.byref(remaining), c.byref(data))
            owner = None
            if result == 0 and fmt.value == 32 and count.value == 1:
                owner = c.cast(data, c.POINTER(c.c_ulong))[0] & 0xFFFFFFFF
            if data: x.XFree(data)
            if owner == pid:
                root, left, top, width, height, border, depth = window(), c.c_int(), c.c_int(), c.c_uint(), c.c_uint(), c.c_uint(), c.c_uint()
                if x.XGetGeometry(display, wid, c.byref(root), c.byref(left), c.byref(top), c.byref(width), c.byref(height), c.byref(border), c.byref(depth)) and width.value > 100 and height.value > 100:
                    matches.append({'windowId': wid, 'width': width.value, 'height': height.value})
            root, parent, children, count = window(), window(), c.POINTER(window)(), c.c_uint()
            if x.XQueryTree(display, wid, c.byref(root), c.byref(parent), c.byref(children), c.byref(count)):
                pending.extend(children[i] for i in range(count.value))
                if children: x.XFree(children)
    finally:
        x.XCloseDisplay(display)
    if not matches:
        raise RuntimeError('No PID-matched X11 client window; desktop capture is not a fallback')
    return max(matches, key=lambda value: value['width'] * value['height'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pid', type=int, required=True)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--seconds', type=int, default=370)
    args = parser.parse_args()
    if not 1 <= args.seconds <= 600:
        parser.error('--seconds must be between 1 and 600')
    info = find_window(args.pid)
    if args.output is None:
        print(json.dumps(info)); return
    if args.output.exists() or args.output.with_suffix('.json').exists():
        raise RuntimeError('Refusing to overwrite an existing recording or receipt')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    info.update(pid=args.pid, display=os.environ.get('DISPLAY'), requestedSamplesPerSecond=60,
                startedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                boundary='X11 sampling of only the PID-matched disposable window; capture may duplicate/miss frames and adds encoder/system load. No unique-render FPS, GPU, physical latency, human usability or improvement claim. Fabric barriers remain present.')
    start = time.monotonic()
    command = ['ffmpeg', '-nostdin', '-hide_banner', '-loglevel', 'warning', '-n', '-f', 'x11grab', '-framerate', '60', '-window_id', str(info['windowId']), '-video_size', f"{info['width']}x{info['height']}", '-i', os.environ['DISPLAY'], '-t', str(args.seconds), '-an', '-vf', 'pad=ceil(iw/2)*2:ceil(ih/2)*2:color=black', '-c:v', 'libx264', '-preset', 'ultrafast', '-threads', '2', '-crf', '20', '-pix_fmt', 'yuv420p', str(args.output)]
    info['command'] = command
    info['encodingPadding'] = 'H264 yuv420p needs even dimensions; at most one black row/column is added, without cropping the window.'
    try:
        subprocess.run(command, check=True)
        info['probe'] = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_streams', '-show_format', '-of', 'json', str(args.output)], text=True))
        info['completed'] = True
    finally:
        info['elapsedSeconds'] = time.monotonic() - start
        args.output.with_suffix('.json').write_text(json.dumps(info, indent=2) + '\n')
    print(json.dumps({'recording': str(args.output), 'seconds': info['elapsedSeconds'], 'window': info['windowId']}))


if __name__ == '__main__':
    main()
