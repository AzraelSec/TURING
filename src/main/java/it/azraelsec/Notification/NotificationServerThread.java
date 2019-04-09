package it.azraelsec.Notification;

import it.azraelsec.Protocol.Commands;
import it.azraelsec.Protocol.Communication;
import it.azraelsec.Server.User;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;

/**
 * The {@code NotificationServerThread} class is a Thread implementation that acts like a
 * reverse TCP client: it connects back to the {@code NotificationClientThread} and sends commands
 * to signal new notifications or to make it shutdown.
 * <p>
 * Every 5 seconds it checks for new notifications related to the target {@code User} and, if
 * it finds any of them, just fire a {@code NEW_NOTIFICATIONS} {@code Commands}.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class NotificationServerThread extends Thread {
    private final String hostname;
    private final int port;
    private final User user;
    private boolean closing;

    /**
     * Initializes the {@code NotificationServerThread}.
     *
     * @param user  session user
     * @param hostname  client hostname
     * @param port  client notification listening port
     */
    public NotificationServerThread(User user, String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.user = user;
        closing = false;
        super.setDaemon(true);
    }

    /**
     * Activates the Thread, making it to fetch for new notifications.
     */
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

    /**
     * Imposes the {@code NotificationServerThread} to shutdown.
     */
    public void close() {
        closing = true;
    }
}

