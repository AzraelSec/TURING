package it.azraelsec;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server
{
    public static final int DATA_BLOCK_SIZE = 512;
    private static final int DEFAULT_TCP_PORT = 1337;
    private static final int DEFAULT_UDP_PORT = 1338;
    private static final int DEFAULT_RMI_PORT = 3400;
    private static final String DEFAULT_DATA_DIR = "./";

    private final ExecutorService TCPConnectionDispatcher;
    public final UsersDB usersDB;
    public final OnlineUsersDB onlineUsersDB;

    public Server() {
        TCPConnectionDispatcher = Executors.newCachedThreadPool();
        onlineUsersDB = new OnlineUsersDB();
        usersDB = new UsersDB();
    }

    /***
     * Main method
     */
    public static void main( String[] args )
    {
        
    }

    private void storeUserDB() {
    }

    /**
     * HELPERS
     */

}
