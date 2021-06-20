package ru.itmo.local_chat.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class MsgTCPConnection extends TCPConnection {

    private final BufferedReader in;
    private final BufferedWriter out;

    public MsgTCPConnection(MsgTCPConnectionListener eventListener, String ipAddr, int port) throws IOException {
        this(eventListener, new Socket(ipAddr, port));
    }

    public MsgTCPConnection(MsgTCPConnectionListener eventListener, Socket socket) throws IOException {
        super(eventListener, socket);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventListener.onConnectionReady(MsgTCPConnection.this);
                    while (!rxThread.isInterrupted()) {
                        eventListener.onReceiveString(MsgTCPConnection.this, in.readLine());
                        Thread.sleep(500);
                    }
                } catch (IOException e) {
                    eventListener.onException(MsgTCPConnection.this, e);
                } catch (InterruptedException e) {
                    eventListener.onException(MsgTCPConnection.this, e);
                } finally {
                    eventListener.onDisconnect(MsgTCPConnection.this);
                }
            }
        });
        rxThread.start();
    }

    public synchronized void sendString(String value) {
        try {
            out.write(value + "\r\n");
            out.flush();
        } catch (IOException e) {
            eventListener.onException(MsgTCPConnection.this, e);
            disconnect();
        }
    }

}
