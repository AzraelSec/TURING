package it.azraelsec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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
    private static final String DEFAULT_DATA_DIR = "./data/";

    private final UsersDB usersDB;
    private final OnlineUsersDB onlineUsersDB;
    private final ExecutorService TCPConnectionDispatcher;

    public Server() {
        TCPConnectionDispatcher = Executors.newCachedThreadPool();
        onlineUsersDB = new OnlineUsersDB();
        usersDB = new UsersDB();
    }

    public void bootstrap() {
        checkConfigDirectory();
    }

    /***
     * Main method
     */
    public static void main( String[] args )
    {
        Server s = new Server();
        s.bootstrap();
    }

    private boolean storeUsersDB() {
        try(ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(DEFAULT_DATA_DIR + "db.dat"))) {
            output.writeObject(usersDB);
            return true;
        }
        catch(IOException ex) {
            return false;
        }
    }

    private UsersDB loadUsersDB() {
        try(ObjectInputStream input = new ObjectInputStream(new FileInputStream(DEFAULT_DATA_DIR + "db.dat"))) {
            return (UsersDB) input.readObject();
        }
        catch(IOException | ClassNotFoundException ex) {
            return null;
        }
    }

    private void checkConfigDirectory() {
        File configDir = new File(DEFAULT_DATA_DIR);
        if(!configDir.isDirectory() || !configDir.exists()) configDir.mkdirs();
    }

    /**
     * HELPERS
     */

}
