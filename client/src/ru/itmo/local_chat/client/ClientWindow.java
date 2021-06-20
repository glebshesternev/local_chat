package ru.itmo.local_chat.client;

import ru.itmo.local_chat.network.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class ClientWindow extends JFrame implements MsgTCPConnectionListener, FileTCPConnectionListener {

    private static String IP_ADDR;

    static {
        try {
            IP_ADDR = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private final int MSG_PORT = 8189;
    private final int FILE_PORT = 8089;

    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    private static final int NAME_WIDTH = 60;
    private static final int NAME_HEIGHT = 20;

    private static final String SAVE_FILE = "save.txt";

    private final JPanel topPanel = new JPanel();
    private final JTextArea log = new JTextArea();
    private final JScrollPane scrollPane = new JScrollPane(log);
    private final JTextField fieldNickName = new JTextField("Gleb");
    private final JTextField fieldHomeDir = new JTextField("");
    private final JTextField fieldInput = new JTextField();
    private final JButton selectBut = new JButton("Select");
    private final JButton sendBut = new JButton("Send");
    private final JButton saveBut = new JButton("Save and disconnect");

    private MsgTCPConnection msgConnection;
    private FileTCPConnection fileConnection;

    private static String homeDir = "C:\\testDir\\Client\\";

    private File selectedFile;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientWindow::new);
    }

    private ClientWindow() {

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setAutoscrolls(true);

        add(scrollPane, BorderLayout.CENTER);


        add(fieldInput, BorderLayout.SOUTH);
        fieldInput.addActionListener(e -> {
            String msg = fieldInput.getText();
            if (!msg.equals("")) {
                fieldInput.setText(null);
                msgConnection.sendString(fieldNickName.getText() + ": " + msg);
            }
        });

        add(selectBut, BorderLayout.WEST);
        selectBut.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setCurrentDirectory(new File(homeDir));
            chooser.setMultiSelectionEnabled(false);
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedFile = chooser.getSelectedFile();
                sendBut.setEnabled(true);
            }
        });

        add(sendBut, BorderLayout.EAST);
        sendBut.setEnabled(false);
        sendBut.addActionListener(e -> {
            msgConnection.sendString(
                    fieldNickName.getText() + " send file: " + selectedFile.getName() +
                    " File size: " + (int) (selectedFile.length() / 1024) + " KB");
            fileConnection.sendFile(selectedFile);
        });

        add(topPanel, BorderLayout.NORTH);
        fieldNickName.setSize(NAME_WIDTH, NAME_HEIGHT);
        fieldHomeDir.setText(homeDir);
        fieldHomeDir.setEditable(false);
        topPanel.add(fieldHomeDir, BorderLayout.EAST);
        topPanel.add(saveBut, BorderLayout.CENTER);
        topPanel.add(fieldNickName, BorderLayout.WEST);

        saveBut.addActionListener(e -> {
            onDisconnect(msgConnection);
            msgConnection.disconnect();
            fileConnection.disconnect();
        });

        setVisible(true);
        String ipAddr = JOptionPane.showInputDialog(
                this,
                "IP Server:",
                "Enter IP address of the Server",
                JOptionPane.QUESTION_MESSAGE);

        String s = JOptionPane.showInputDialog(
                this,
                "Home folder:",
                "Enter path to home folder",
                JOptionPane.QUESTION_MESSAGE);

        homeDir = s.equals("") ? homeDir : s;
        ipAddr = ipAddr.equals("") ? IP_ADDR : ipAddr;
        fieldHomeDir.setText(homeDir);

        try {
            msgConnection = new MsgTCPConnection(this, ipAddr, MSG_PORT);
            fileConnection = new FileTCPConnection(this, ipAddr, FILE_PORT);
        } catch (IOException e) {
            printMsg("Connection exception: " + e);
        }
    }


    @Override
    public void onConnectionReady(FileTCPConnection tcpConnection) {
        printMsg("Connection ready...");
        try {
            FileInputStream save = new FileInputStream(homeDir + SAVE_FILE);
            byte[] buffer = new byte[1024];
            int i;
            do {
                i = save.read(buffer);
                log.append(new String(buffer, StandardCharsets.UTF_8));
            } while (i == 1024);
            printMsg("Log file loaded.");
        } catch (FileNotFoundException e) {
            printMsg("Log file not found.");
        } catch (IOException e) {
            printMsg("Connection exception: " + e);
        }
    }

    @Override
    public void onConnectionReady(MsgTCPConnection tcpConnection) {
    }

    @Override
    public void onReceiveString(MsgTCPConnection tcpConnection, String value) {
        printMsg(value);
    }

    @Override
    public void onReceiveFile(FileTCPConnection tcpConnection, int nameSize) {
        tcpConnection.receiveFile(homeDir, nameSize);
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        try {
            FileOutputStream save = new FileOutputStream(homeDir + SAVE_FILE);
            byte[] buffer = log.getText().getBytes(StandardCharsets.UTF_8);
            save.write(buffer, 0, buffer.length);
            printMsg("Log saved.");
        } catch (IOException e) {
            printMsg("Connection exception: " + e);
        }
        printMsg("Connection close...");
    }

    @Override
    public void onException(TCPConnection tcpConnection, Exception e) {
        printMsg("Connection exception: " + e);
    }

    private synchronized void printMsg(String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg + '\n');
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

}
