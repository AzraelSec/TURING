package it.azraelsec.Notification;

import it.azraelsec.Client.Client;
import it.azraelsec.Protocol.Commands;
import it.azraelsec.Protocol.Communication;
import it.azraelsec.Protocol.Execution;
import it.azraelsec.Protocol.Result;
import it.azraelsec.Server.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class NotificationClientThread extends Thread {
    private final List<String> localNotificationQueue;
    private final HashMap<Commands, Execution> handlers;
    private boolean closing;

    public NotificationClientThread(List<String> localNotificationQueue) {
        handlers = new HashMap<>();
        handlers.put(Commands.NEW_NOTIFICATIONS, this::onNews);
        handlers.put(Commands.EXIT, this::onClosing);
        this.localNotificationQueue = localNotificationQueue;
        closing = false;
    }

    @Override
    public void run() {
        Socket notificationSocket = null;
        DataInputStream inputStream = null;
        DataOutputStream outputStream = null;
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(Client.NOTIFICATION_PORT);
            while(!Thread.currentThread().isInterrupted()) {
                notificationSocket = socket.accept();
                outputStream = new DataOutputStream(notificationSocket.getOutputStream());
                inputStream = new DataInputStream(notificationSocket.getInputStream());
                closing = false;
                while(!closing)
                    Communication.receive(inputStream, outputStream, handlers);
            }
        } catch (IOException ignored) {
            System.out.println("NotificationClientThread is dead");
            ignored.printStackTrace();
        }
        finally {
            try {
                if(socket != null) socket.close();
                if(notificationSocket != null) notificationSocket.close();
                if(outputStream != null) outputStream.close();
                if(inputStream != null) inputStream.close();
            } catch (IOException ignore) {}
        }
    }

    private void onNews(Object[] args, Result sendback) {
        synchronized (localNotificationQueue) {
            localNotificationQueue.add((String)args[0]);
        }
        sendback.send(Commands.SUCCESS, "Notification has been added to client's notifications queue");
    }

    private void onClosing(Object[] args, Result sendback) {
        closing = true;
        sendback.send(Commands.SUCCESS, "Notification server is closing");
    }
}