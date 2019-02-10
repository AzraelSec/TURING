package it.azraelsec.Client;

import java.io.IOException;
import java.net.ServerSocket;

public class NotificationThread extends Thread {
    private ServerSocket socket;
    private Client client;

    public NotificationThread(ServerSocket socket, Client client) {
        this.socket = socket;
        this.client = client;
        super.setDaemon(true);
    }

    @Override
    public void run() {
        while(!isInterrupted())
            try {
                socket.accept();
            }
            catch (IOException ex) {
            }
    }

    public ServerSocket getSocket() {
        return this.socket;
    }
}
