package it.azraelsec.Client;

import it.azraelsec.Protocol.Commands;
import it.azraelsec.Protocol.Communication;
import it.azraelsec.Protocol.Execution;
import it.azraelsec.Protocol.Result;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class NotificationThread extends Thread {
    private ServerSocket socket;
    private final Client client;
    private final HashMap<Commands, Execution> handlers;
    private boolean closing;

    public NotificationThread(Client client) {
        closing = false;
        this.socket = null;
        this.client = client;
        handlers = new HashMap<>();
        handlers.put(Commands.NEW_NOTIFICATION, this::onNews);
        handlers.put(Commands.EXIT, this::onClosing);
        super.setDaemon(true);
    }

    @Override
    public void run() {
        Socket notificationSocket;
        DataInputStream inputStream;
        DataOutputStream outputStream;
        try {
            socket = new ServerSocket(Client.NOTIFICATION_PORT);
            notificationSocket = socket.accept();
            outputStream = new DataOutputStream(notificationSocket.getOutputStream());
            inputStream = new DataInputStream(notificationSocket.getInputStream());
            while(!closing) Communication.receive(inputStream, outputStream, handlers);
        }
        catch (IOException ex) {
        }
    }

    private void onNews(Object[] args, Result sendback) {
        //todo: to complete
    }

    private void onClosing(Object[] args, Result sendback) {
        //todo: to complete
    }
}
