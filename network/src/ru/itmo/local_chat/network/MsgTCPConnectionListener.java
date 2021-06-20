package ru.itmo.local_chat.network;

public interface MsgTCPConnectionListener extends TCPConnectionListener {

    void onConnectionReady(MsgTCPConnection tcpConnection);

    void onReceiveString(MsgTCPConnection tcpConnection, String value);
}
