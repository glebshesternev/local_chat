package ru.itmo.local_chat.server;

import ru.itmo.local_chat.network.*;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class ChatServer implements FileTCPConnectionListener, MsgTCPConnectionListener {

    private final int MSG_PORT = 8189;
    private final int FILE_PORT = 8089;
    private byte[] local_network_addr;
    private short mask;

    private final ArrayList<MsgTCPConnection> msgConnections = new ArrayList<>();
    private final ArrayList<FileTCPConnection> fileConnections = new ArrayList<>();

    private static String homeDir = "C:\\testDir\\Server\\";

    public static void main(String[] args) {
        new ChatServer();
    }

    private ChatServer() {

        try {
            InetAddress localHost = Inet4Address.getLocalHost();
            byte[] addr = Inet4Address.getLocalHost().getAddress();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
            mask = networkInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength();
            local_network_addr = getNetworkAddr(addr);
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }

        System.out.println("Server running...");
        System.out.println("Enter path to home dir (default: C:\\testDir\\Server\\):");

        String s = new Scanner(System.in).nextLine();
        if (!s.equals("") && !s.equals("\n"))
            homeDir = s;
        System.out.println("Home dir: " + homeDir);
        try {
            ServerSocket msgSocket = new ServerSocket(MSG_PORT);
            ServerSocket fileSocket = new ServerSocket(FILE_PORT);
            while (true) {
                try {
                    new MsgTCPConnection(this, msgSocket.accept());
                    new FileTCPConnection(this, fileSocket.accept());
                } catch (IOException e) {
                    System.out.println("TCPConnection exception: " + e);
                }
            }
        } catch (IOException e) {
            System.out.println("TCPConnection exception: " + e);
        }
    }

    @Override
    public synchronized void onConnectionReady(MsgTCPConnection tcpConnection) {
        if (Arrays.equals(local_network_addr, getNetworkAddr(tcpConnection.getAddr()))) {
            msgConnections.add(tcpConnection);
            sendStringToAllConnections("Client connected: " + tcpConnection);
        }
    }

    @Override
    public synchronized void onConnectionReady(FileTCPConnection tcpConnection) {

        if (Arrays.equals(local_network_addr, getNetworkAddr(tcpConnection.getAddr())))
            fileConnections.add(tcpConnection);
    }

    @Override
    public synchronized void onReceiveString(MsgTCPConnection tcpConnection, String value) {

        sendStringToAllConnections(value);

    }

    @Override
    public synchronized void onReceiveFile(FileTCPConnection tcpConnection, int nameSize) {

        String fileName = tcpConnection.receiveFile(homeDir, nameSize);
        sendFileToAllConnections(fileName);

    }


    @Override
    public synchronized void onDisconnect(TCPConnection tcpConnection) {

        if (tcpConnection.getPort() == MSG_PORT)
            msgConnections.remove(tcpConnection);
        else fileConnections.remove(tcpConnection);

        sendStringToAllConnections("Client disconnected: " + tcpConnection);

    }

    @Override
    public synchronized void onException(TCPConnection tcpConnection, Exception e) {
        System.out.println("TCPConnection exception: " + e);
    }

    private synchronized void sendStringToAllConnections(String value) {
        System.out.println(value);
        for (MsgTCPConnection msgConnection : msgConnections) {
            msgConnection.sendString(value);
        }
    }

    private synchronized void sendFileToAllConnections(String fileName) {
        System.out.println(fileName);
        File f = new File(homeDir + fileName);
        for (FileTCPConnection fileConnection : fileConnections) {
            fileConnection.sendFile(f);
        }
    }

    private byte[] getNetworkAddr(byte[] addr) {
        int addrSize = 32;
        int a = (addrSize - mask) / 8;
        int b = (addrSize - mask) % 8;
        for (int i = 0; i < a; i++) {
            addr[3-i] = 0;
        }
        addr[3-a] = (byte) (addr[3-a] >> b);
        return addr;
    }
}
