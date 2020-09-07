import paramiko
import socks

import os

class PySftpConnection:
    @staticmethod
    def _getSocks5Proxy():
        host = os.getenv("PROXY_HOSTNAME")
        user = os.getenv("PROXY_USERNAME")
        psw = os.getenv("PROXY_PASSWORD")
        port = int(os.getenv("PROXY_PORT"))

        sock = None

        if host != None:
            sock = socks.socksocket()
            sock.set_proxy(
                proxy_type=socks.SOCKS5,
                addr=host,
                port=int(port),
                username=user,
                password=psw,
            )

        return sock

    @staticmethod
    def getSftpConnection(host, username, pwd, port):
        proxysock = PySftpConnection._getSocks5Proxy()
        if proxysock:
            # Connect the socket
            proxysock.connect((host, port))
            transport = paramiko.Transport(proxysock)
            transport.connect(username=username, password=pwd)
            return paramiko.SFTPClient.from_transport(transport)
        else:
            transport = paramiko.Transport((host, port))
            transport.connect(username=username, password=pwd)
            return paramiko.SFTPClient.from_transport(transport)

