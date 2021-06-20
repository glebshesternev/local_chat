package ru.itmo.local_chat.network;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class TCPConnection {
    protected final Socket socket;
    protected final TCPConnectionListener eventListener;
    protected Thread rxThread;

    public TCPConnection(TCPConnectionListener eventListener, Socket socket) {
        this.eventListener = eventListener;
        this.socket = socket;
    }

    public synchronized void disconnect() {
        rxThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection: " + socket.getInetAddress() + ":" + socket.getPort();
    }

    public int getPort() {
        return socket.getPort();
    }
}
