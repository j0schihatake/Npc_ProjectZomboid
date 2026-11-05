"""Wait for one verified game process to exit, then arm a fresh test copy; never stop/restart it."""
import argparse
from pathlib import Path
import time

from arm_probe import arm


def identity(pid):
    proc=Path('/proc')/str(pid)
    try:
        command=(proc/'cmdline').read_bytes().split(b'\0')[0].decode()
        start=(proc/'stat').read_text().rsplit(')',1)[1].split()[19]
    except (FileNotFoundError, ProcessLookupError):
        return None
    if Path(command).name!='ProjectZomboid64':
        return None
    return start


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('pid',type=int)
    parser.add_argument('source')
    parser.add_argument('--userdata',type=Path,default=Path('/home/j0schi/Zomboid'))
    parser.add_argument('--timeout',type=int,default=600)
    parser.add_argument('--seconds',type=int,default=180)
    parser.add_argument('--quit-when-done',action='store_true')
    args=parser.parse_args()
    if args.pid<=0 or not 1<=args.timeout<=3600:
        parser.error('Pass a positive PID and timeout of 1..3600 seconds')
    token=identity(args.pid)
    if token is None:
        raise SystemExit('The specified game process is not live; revalidate its state before arming')
    print(f'Waiting for verified ProjectZomboid64 PID {args.pid}; no process will be stopped.',flush=True)
    deadline=time.monotonic()+args.timeout
    while identity(args.pid)==token:
        if time.monotonic()>=deadline:
            raise SystemExit('Wait expired with the same game still live. No copy was armed.')
        time.sleep(1)
    # arm() independently refuses a replacement game process or an uninstalled adapter.
    record=arm(args.userdata.resolve(),args.source,args.seconds,args.quit_when_done)
    print('Closed-game preparation completed. Record: '+str(record),flush=True)


if __name__=='__main__':
    main()
