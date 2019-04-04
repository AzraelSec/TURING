package it.azraelsec.Notification;

import it.azraelsec.Protocol.Commands;
import it.azraelsec.Protocol.Communication;
import it.azraelsec.Protocol.Execution;
import it.azraelsec.Protocol.Result;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.*;

public class NotificationClientThread extends Thread {
    private final List<String> localNotificationQueue;
    private final HashMap<Commands, Execution> handlers;
    private boolean closing;
    private int servingPort;

    public NotificationClientThread() {
        servingPort = 0;
        handlers = new HashMap<>();
        handlers.put(Commands.NEW_NOTIFICATIONS, this::onNews);
        handlers.put(Commands.EXIT, this::onClosing);
        this.localNotificationQueue = new ArrayList<>();
    }

    @Override
    public void run() {
        Socket notificationSocket = null;
        DataInputStream inputStream = null;
        DataOutputStream outputStream = null;
        ServerSocketChannel socketChannel = null;
        Selector selector;
        try {
            ServerSocket socket;
            socketChannel = ServerSocketChannel.open();
            socket = socketChannel.socket();
            socket.bind(new InetSocketAddress(servingPort));
            servingPort = socket.getLocalPort();
            socketChannel.configureBlocking(false);
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    try {
                        if (key.isAcceptable()) {
                            notificationSocket = socketChannel.accept().socket();
                            outputStream = new DataOutputStream(notificationSocket.getOutputStream());
                            inputStream = new DataInputStream(notificationSocket.getInputStream());
                            closing = false;
                            while (!closing)
                                Communication.receive(inputStream, outputStream, handlers);
                        }
                    } catch (IOException ex) {
                        key.cancel();
                        try {
                            key.channel().close();
                        } catch (IOException ignore) {
                        }
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("NotificationClientThread is dead");
            ex.printStackTrace();
        } finally {
            try {
                if (socketChannel != null) socketChannel.close();
                if (notificationSocket != null) notificationSocket.close();
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException ignore) {
            }
        }
    }

    private void onNews(Object[] args, Result sendback) {
        synchronized (localNotificationQueue) {
            localNotificationQueue.add((String) args[0]);
        }
        sendback.send(Commands.SUCCESS, "Notification has been added to client's notifications queue");
    }

    private void onClosing(Object[] args, Result sendback) {
        closing = true;
        sendback.send(Commands.SUCCESS, "Notification server is closing");
    }

    public void clearNotificationList() {
        synchronized (localNotificationQueue) {
            localNotificationQueue.clear();
        }
    }

    public ArrayList<String> getAllNotifications() {
        ArrayList<String> notifications = new ArrayList<>();
        synchronized (localNotificationQueue) {
            if(!localNotificationQueue.isEmpty()) {
                notifications.addAll(localNotificationQueue);
                localNotificationQueue.clear();
            }
        }
        return notifications;
    }

    public int getNotificationLocalPort() {
        return servingPort;
    }
}
