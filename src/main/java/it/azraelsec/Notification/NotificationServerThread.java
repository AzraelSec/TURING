package it.azraelsec.Notification;

import it.azraelsec.Protocol.Commands;
import it.azraelsec.Protocol.Communication;
import it.azraelsec.Protocol.Execution;
import it.azraelsec.Protocol.Result;
import it.azraelsec.Server.User;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;

/**
 * Belongs to the Client object
 */
public class NotificationServerThread extends Thread {
    private final String hostname;
    private final int port;
    private final User user;
    private boolean closing;

    public NotificationServerThread(User user, String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.user = user;
        closing = false;
        super.setDaemon(true);
    }

    @Override
    public void run() {
        SocketAddress serverAddress = new InetSocketAddress(hostname, port);
        Socket socket = new Socket();
        DataOutputStream outputStream;
        DataInputStream inputStream;
        try {
            socket.connect(serverAddress);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            while(!closing) {
                List<String> notificationsQueue = user.getUnreadNotifications();
                if(!notificationsQueue.isEmpty())
                    Communication.send(outputStream, inputStream, ignore -> {}, ignore -> {}, Commands.NEW_NOTIFICATIONS, String.join(",", notificationsQueue));
                try { Thread.sleep(1000 * 5); } catch (InterruptedException ignore) {}
            }
            Communication.send(outputStream, inputStream, ignore -> {}, ignore -> {}, Commands.EXIT);
        }
        catch (IOException ignore) {
            ignore.printStackTrace();
        }
    }

    public void close() {
        closing = true;
    }
}

