package io.arex.standalone.cli.server;

import io.arex.standalone.cli.util.LogUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerListener extends Thread {
    private int bindPort;

    public ServerListener(int port) {
        this.bindPort = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(bindPort)) {
            while (true) {
                Socket socket = serverSocket.accept();
                HttpServer.handle(socket);
                socket.close();
            }
        } catch (IOException e) {
            LogUtil.warn(e);
        }
    }
}
