package ru.itmo.local_chat.network;

public interface FileTCPConnectionListener extends TCPConnectionListener{

    void onConnectionReady(FileTCPConnection tcpConnection);

    void onReceiveFile(FileTCPConnection tcpConnection, int nameLength);
}
