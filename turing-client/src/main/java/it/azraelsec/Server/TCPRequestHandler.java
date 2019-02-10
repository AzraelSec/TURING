package it.azraelsec.Server;

import it.azraelsec.Protocol.Commands;
import it.azraelsec.Protocol.Communication;
import it.azraelsec.Protocol.Execution;
import it.azraelsec.Protocol.Result;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TCPRequestHandler implements Runnable {
    OnlineUsersDB onlineUsersDB;
    UsersDB usersDB;
    Socket socket;
    DataInputStream scanner;
    DataOutputStream printer;
    boolean sessionAlive;

    Map<Commands, Execution> handlers;

    public TCPRequestHandler(OnlineUsersDB onlineUsersDB, UsersDB usersDB, Socket socket) throws IOException {
        this.onlineUsersDB = onlineUsersDB;
        this.usersDB = usersDB;
        this.socket = socket;
        scanner = new DataInputStream(socket.getInputStream());
        printer = new DataOutputStream(socket.getOutputStream());
        handlers = new HashMap<>();
        handlers.put(Commands.LOGIN, this::onLogin);
        handlers.put(Commands.LOGOUT, this::onLogout);
        handlers.put(Commands.EDIT, this::onEdit);
        handlers.put(Commands.EDIT_END, this::onEditEnd);
        sessionAlive = false;
    }

    @Override
    public void run() {
        try {
            System.out.println("Receiving...");
            do Communication.receive(scanner, printer, handlers); while (sessionAlive);
        }
        finally {
            try {
                scanner.close();
                printer.close();
                socket.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    private void onLogin(Object[] args, Result sendBack) {
        sessionAlive = true;
        System.out.println("Clients: " + args[1]);
        sendBack.send(Commands.SUCCESS, "token001");
    }

    private void onLogout(Object[] args, Result sendBack) {
        sessionAlive = false;
        System.out.println("Client's gone out");
        sendBack.send(Commands.SUCCESS, "Good-bye");
    }

    private void onEdit(Object[] args, Result sendBack) {
        if(sessionAlive) {
            sendBack.send(Commands.SUCCESS, "You're editing");
            try {
                InputStream stream = new ByteArrayInputStream("Yeeehhhhh maybe it is working but probably not".getBytes(StandardCharsets.UTF_8));
                Communication.receiveAndSendStream(scanner, printer, stream);
            }
            catch (IOException ex) {
                sendBack.send(Commands.FAILURE, ex.getMessage());
            }
        }
        else throw new IllegalStateException();
    }

    private void onEditEnd(Object[] args, Result sendback) {
        if(sessionAlive) {
            sendback.send(Commands.SUCCESS, "Send me new version");
            try {
                Communication.readFileFromSocket(scanner, System.out);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
