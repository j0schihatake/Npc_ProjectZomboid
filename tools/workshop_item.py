"""Use the installed game's official Steamworks API for explicitly selected items.

Does not enable mods, edit Steam manifests, or change game/save configuration.
"""
import argparse
import ctypes as C
import json
import os
from pathlib import Path
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['status', 'download', 'subscribe', 'unsubscribe'])
    parser.add_argument('items', type=int, nargs='+')
    parser.add_argument('--wait', type=int, default=0, help='Wait up to 45 seconds for confirmation')
    args = parser.parse_args()
    if not 0 <= args.wait <= 45 or any(item <= 0 or item >= 2**64 for item in args.items):
        parser.error('Invalid item id or wait duration')
    library = Path('/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid/natives/libsteam_api.so')
    os.environ['SteamAppId'] = '108600'
    os.environ['SteamGameId'] = '108600'
    sdk = C.CDLL(str(library))

    def function(name, result, *arguments):
        fn = getattr(sdk, name)
        fn.restype = result
        fn.argtypes = list(arguments)
        return fn

    error = C.create_string_buffer(1024)
    # Avoid logging the user's Steam identity from the SDK initialization banner.
    saved = [os.dup(1), os.dup(2)]
    with open(os.devnull, 'w') as sink:
        try:
            os.dup2(sink.fileno(), 1)
            os.dup2(sink.fileno(), 2)
            initialized = function('SteamAPI_InitFlat', C.c_int, C.c_void_p)(error)
            C.CDLL(None).fflush(None)
        finally:
            for target, descriptor in zip([1, 2], saved):
                os.dup2(descriptor, target)
                os.close(descriptor)
    if initialized != 0:
        raise SystemExit('Steam API initialization failed: ' + error.value.decode(errors='replace'))
    try:
        ugc = function('SteamAPI_SteamUGC_v021', C.c_void_p)()
        utils = function('SteamAPI_SteamUtils_v010', C.c_void_p)()
        if not ugc or not utils or function('SteamAPI_ISteamUtils_GetAppID', C.c_uint32, C.c_void_p)(utils) != 108600:
            raise RuntimeError('Unexpected Steam application context')
        state = function('SteamAPI_ISteamUGC_GetItemState', C.c_uint32, C.c_void_p, C.c_uint64)
        callbacks = function('SteamAPI_RunCallbacks', None)
        if args.action != 'status':
            if args.action == 'download':
                request = function('SteamAPI_ISteamUGC_DownloadItem', C.c_bool, C.c_void_p, C.c_uint64, C.c_bool)
            else:
                name = 'SubscribeItem' if args.action == 'subscribe' else 'UnsubscribeItem'
                request = function('SteamAPI_ISteamUGC_' + name, C.c_uint64, C.c_void_p, C.c_uint64)
            for item in args.items:
                accepted = request(ugc, item, False) if args.action == 'download' else request(ugc, item)
                if not accepted:
                    raise RuntimeError(f'{args.action} rejected for {item}')
        deadline = time.monotonic() + args.wait
        while True:
            callbacks()
            states = {item: state(ugc, item) for item in args.items}
            complete = all((flags & 4 and not flags & 56) if args.action == 'download' else
                           bool(flags & 1) if args.action == 'subscribe' else
                           not flags & 1 if args.action == 'unsubscribe' else True
                           for flags in states.values())
            if complete or time.monotonic() >= deadline:
                break
            time.sleep(.25)
        print(json.dumps({'action': args.action, 'confirmed': bool(complete), 'items': {
            str(item): {'state': flags, 'subscribed': bool(flags & 1), 'installed': bool(flags & 4),
                        'needsUpdate': bool(flags & 8), 'downloading': bool(flags & 16), 'pending': bool(flags & 32)}
            for item, flags in states.items()}}, indent=2))
    finally:
        function('SteamAPI_Shutdown', None)()


if __name__ == '__main__':
    main()
