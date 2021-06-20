package ru.itmo.local_chat.network;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class FileTCPConnection extends TCPConnection {

    protected final BufferedInputStream bis;
    protected final BufferedOutputStream bos;

    public FileTCPConnection(FileTCPConnectionListener eventListener, String ipAddr, int port) throws IOException {
        this(eventListener, new Socket(ipAddr, port));
    }

    public FileTCPConnection(FileTCPConnectionListener eventListener, Socket socket) throws IOException {
        super(eventListener, socket);
        bis = new BufferedInputStream(socket.getInputStream());
        bos = new BufferedOutputStream(socket.getOutputStream());
        rxThread = new Thread(() -> {
            try {
                eventListener.onConnectionReady(FileTCPConnection.this);
                while (!rxThread.isInterrupted()) {
                    eventListener.onReceiveFile(FileTCPConnection.this, bis.read());
                    Thread.sleep(500);
                }
            } catch (IOException | InterruptedException e) {
                eventListener.onException(FileTCPConnection.this, e);
            } finally {
                eventListener.onDisconnect(FileTCPConnection.this);

            }
        });
        rxThread.start();
    }

    public synchronized void sendFile(final File f) {
        try {
            String fileName = f.getName();
            byte[] name = fileName.getBytes(StandardCharsets.UTF_8);
            int bufferSize = 1024 * 1024;
            bos.write(name.length);
            bos.write(name);
            bos.write(ByteBuffer.allocate(Long.BYTES).putLong(f.length()).array());
            byte[] buffer = new byte[bufferSize];
            FileInputStream fis = new FileInputStream(f.getPath());
            int i;
            do {
                i = fis.read(buffer);
                bos.write(buffer, 0, i);
            } while (i == bufferSize);
            bos.flush();
            fis.close();
        } catch (IOException e) {
            eventListener.onException(FileTCPConnection.this, e);
        }
    }

    public synchronized String receiveFile(String path, int nameSize) {
        String fileName = null;
        try {
            byte[] name = new byte[nameSize + 1];
            bis.read(name, 0, nameSize);
            fileName = new String(name, StandardCharsets.UTF_8).trim();
            File f = new File(path + fileName);
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            byte[] fileSizeBuf = new byte[8];
            bis.read(fileSizeBuf, 0, 8);
            ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
            long fileSize = buf.put(fileSizeBuf).flip().getLong();
            int bufferSize = 1024 * 1024;
            //while (bis.available() < fileSize && bis.available() < bufferSize);
            byte[] buffer = new byte[bufferSize];
            int i;
            do {
                i = bis.read(buffer);
                fos.write(buffer, 0, i);
                fileSize -= i;
            } while (fileSize != 0);
            fos.close();
            return fileName;
        } catch (IOException e) {
            eventListener.onException(FileTCPConnection.this, e);
            return fileName;
        }
    }

}
