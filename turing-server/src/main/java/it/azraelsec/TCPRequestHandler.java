package it.azraelsec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TCPRequestHandler implements Runnable {

    OnlineUsersDB onlineUsersDB;
    UsersDB usersDB;
    SocketChannel socket;

    public TCPRequestHandler(OnlineUsersDB onlineUsersDB, UsersDB usersDB, SocketChannel socket) {
        this.onlineUsersDB = onlineUsersDB;
        this.usersDB = usersDB;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Server.BLOCK_SIZE);
            socket.configureBlocking(true);
            socket.read(buffer);
            buffer.flip();
            int code = buffer.getInt();
            switch (Commands.getCommand(code)) {
                case LOGIN:
                    String username = new String(new byte[buffer.get(buffer.getInt())]);
                    String password = new String(new byte[buffer.get(buffer.getInt())]);
                    System.out.println(username + password);
                    User user = usersDB.doLogin(username, password);
                    if(user != null) {
                        String token = onlineUsersDB.login(user, socket.getRemoteAddress());
                        System.out.println("New token for user " + user.getUsername() + ": " + token);
                    }
                    else {
                        System.out.println("Authentication Error from client:" + socket.getRemoteAddress());
                    }
                    break;
                default:
                    System.out.println("Command " + code + " not found");
            }
        }
        catch (IOException ex) {
            System.out.println("IOException:" + ex.getMessage());
        }
    }
}
