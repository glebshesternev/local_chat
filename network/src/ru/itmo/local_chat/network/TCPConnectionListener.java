package ru.itmo.local_chat.network;

public interface TCPConnectionListener {

    void onDisconnect(TCPConnection tcpConnection);

    void onException(TCPConnection tcpConnection, Exception e);
}
