import logging
import os
import socket

from ServerManager import *

rootlogger = logging.Logger('log')
rootlogger.setLevel(logging.DEBUG)

def get_current_session():
    if os.system('which loginctl') == 0 and os.system('which awk') == 0:
        return os.popen("loginctl list-sessions --no-legend | awk '{ print $1 }'").read().strip()
    else:
        raise OSError('The necessary binaries awk and/or loginctl are not present on this machine')

if __name__ == '__main__':
    if len(sys.argv) < 4:
        print(f'Usage: {sys.argv[0]} <passphrase> <host> <port>')
    sharedSecret = blake2kdf(sys.argv[1].encode('utf-8'))
    current_session = get_current_session()
    bindsocket = socket.socket()
    bindsocket.bind((sys.argv[2], int(sys.argv[3])))
    bindsocket.listen(5)
    while True:
        newsocket, fromaddr = bindsocket.accept()
        sm = ServerManager(rootlogger, newsocket, sharedSecret)
        if sm.authenticate():
            print('Unlocking session...')
            os.system(f'loginctl unlock-session {current_session}')
