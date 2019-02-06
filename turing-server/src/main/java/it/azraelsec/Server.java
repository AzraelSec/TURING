package it.azraelsec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Server
{
    public static final int DATA_BLOCK_SIZE = 512;
    private static final int DEFAULT_TCP_PORT = 1337;
    private static final int DEFAULT_UDP_PORT = 1338;
    private static final int DEFAULT_RMI_PORT = 3400;
    private static final String DEFAULT_DATA_DIR = "./data/";

    private UsersDB usersDB;
    private final OnlineUsersDB onlineUsersDB;
    private final ExecutorService TCPConnectionDispatcher;

    public Server() {
        TCPConnectionDispatcher = Executors.newCachedThreadPool();
        onlineUsersDB = new OnlineUsersDB();
    }

    public void bootstrap() {
        checkConfigDirectory();
        usersDB = loadUsersDB();
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

    private UsersDB initUsersDB() {
        UsersDB loadedUsersDB = loadUsersDB();
        return loadedUsersDB == null ? new UsersDB() : loadedUsersDB;
    }

    private void checkConfigDirectory() {
        File configDir = new File(DEFAULT_DATA_DIR);
        if(!configDir.isDirectory() || !configDir.exists()) configDir.mkdirs();
    }

    /***
     * Main method
     */
    public static void main( String[] args )
    {
        ArgumentParser argpars = ArgumentParsers.newFor("TURING Server")
            .build()
            .defaultHelp(true)
            .description("TURING distributed program server");
        argpars.addArgument("-c", "--config-dir").help("server JSON configuration file");
        argpars.addArgument("-t", "--tcp-command-port").help("TCP commands port");
        argpars.addArgument("-u", "--tcp-multicast-port").help("UDP multicast port");
        argpars.addArgument("-r", "--rmi-port").help("RMI communication port");
        argpars.addArgument("-d", "--data-dir").help("server data directory");

        Namespace ns = null;

        try {
            ns = argpars.parseArgs(args);
        }
        catch(ArgumentParserException ex) {
            argpars.printHelp();
            System.exit(1);
        }
        System.out.println(ns);
        /*
        Server s = new Server();
        s.bootstrap();
        */
    }
}
