package it.azraelsec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.Optional;
import java.util.stream.Collectors;

import org.json.*;

public class Server {
    public static final int BLOCK_SIZE = 512;
    private static int TCP_PORT = 1337;
    private static int UDP_PORT = 1338;
    private static int RMI_PORT = 3400;
    private static String DATA_DIR = "./data/";


    private UsersDB usersDB;
    private final OnlineUsersDB onlineUsersDB;
    private final ExecutorService TCPConnectionDispatcher;

    public Server() throws RemoteException {
        super();
        TCPConnectionDispatcher = Executors.newCachedThreadPool();
        onlineUsersDB = new OnlineUsersDB();
    }

    public void bootstrap(Namespace cmdOptions) throws RemoteException, AlreadyBoundException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("TURING Server is shutting down...");
            TCPConnectionDispatcher.shutdown();
            storeUsersDB();
        }));
        Optional.ofNullable(cmdOptions.getString("config_file")).ifPresent(this::loadConfig);
        TCP_PORT = Optional.ofNullable( cmdOptions.getInt("tcp_command_port") ).orElseGet( () -> TCP_PORT );
        UDP_PORT = Optional.ofNullable( cmdOptions.getInt("udp_port") ).orElseGet( () -> UDP_PORT );
        RMI_PORT = Optional.ofNullable( cmdOptions.getInt("rmi_port") ).orElseGet( () -> RMI_PORT );
        DATA_DIR = Optional.ofNullable( cmdOptions.getString("data_dir") ).orElseGet( () -> DATA_DIR );
        checkDataDirectory();
        usersDB = initUsersDB();
        RMIInit();
        System.out.println(String.format("TCP_PORT: %s\nUDP_PORT: %s\nRMI_PORT: %s\nDATA_DIR: %s", TCP_PORT, UDP_PORT, RMI_PORT, DATA_DIR));
    }
    public void serve() {
        try(ServerSocketChannel TCPServer = ServerSocketChannel.open(); 
            DatagramChannel UDPServer = DatagramChannel.open()) {
            TCPServer.bind(new InetSocketAddress(TCP_PORT));
            UDPServer.bind(new InetSocketAddress(UDP_PORT));
            Selector selector = Selector.open();
            TCPServer.configureBlocking(false);
            UDPServer.configureBlocking(false);
            TCPServer.register(selector, SelectionKey.OP_ACCEPT);
            UDPServer.register(selector, 0);
            ByteBuffer buffer = ByteBuffer.allocate(Server.BLOCK_SIZE);

            System.out.println("ADDRESS: " + InetAddress.getLocalHost().toString());

            while(true) {
                selector.selectedKeys().clear();
                selector.select();
                buffer.clear();

                for(SelectionKey key : selector.selectedKeys()) {
                    if(key.isAcceptable()) {
                        try {
                            SocketChannel socket = TCPServer.accept();
                            System.out.println("New TCP command connection enstablished");
                            // Command dispatching and fetching directed to new Thread...
                            TCPConnectionDispatcher.submit(new TCPRequestHandler(onlineUsersDB, usersDB, socket));
                        }
                        catch (Exception ex) {
                            System.out.println("Error accepting client: " + ex.getMessage());
                            key.cancel();
                        }
                    }
                }
            }
        }
        catch(Exception e) {
            //System.out.println("Generic fatal error: " + e);
            e.printStackTrace();
        }
    }
    private boolean storeUsersDB() {
        try(ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(DATA_DIR + "db.dat"))) {
            output.writeObject(usersDB);
            return true;
        }
        catch(IOException ex) {
            return false;
        }
    }
    private UsersDB loadUsersDB() {
        try(ObjectInputStream input = new ObjectInputStream(new FileInputStream(DATA_DIR + "db.dat"))) {
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

    private void checkDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if(!dataDir.isDirectory() || !dataDir.exists()) dataDir.mkdirs();
    }

    private void loadConfig(String filePath) {
        if(checkConfigFile(filePath))
        {
            try{
                JSONObject configs = new JSONObject(Files.lines(new File(filePath).toPath()).collect(Collectors.joining("\n")));
                
                TCP_PORT = configs.has("TCP_PORT") ? configs.getInt("TCP_PORT") : TCP_PORT;
                UDP_PORT = configs.has("UDP_PORT") ? configs.getInt("UDP_PORT") : UDP_PORT;
                RMI_PORT = configs.has("RMI_PORT") ? configs.getInt("RMI_PORT") : RMI_PORT;
                DATA_DIR = configs.has("DATA_DIR") ? configs.getString("DATA_DIR") : DATA_DIR;
            }
            catch(Exception ex) {
                System.out.println("JSON parsing error for file:" + filePath);
                System.out.println("That's the reason:" + ex.getMessage());
            } 
        }
        else System.out.println("Configuration file not found");
    }

    private boolean checkConfigFile(String filePath) {
        File configFile = new File(filePath);
        return configFile.isFile() && configFile.exists();
    }
    private void RMIInit() throws RemoteException, AlreadyBoundException {
        RegistrationStub remoteObject = new RegistrationStub(usersDB, onlineUsersDB);
        LocateRegistry .createRegistry(RMI_PORT);
        Registry registry = LocateRegistry.getRegistry(RMI_PORT);
        registry.bind(RegistrationStub.NAME, remoteObject);
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
        argpars.addArgument("-t", "--tcp-command-port").help("TCP commands port").type(Integer.class);
        argpars.addArgument("-u", "--udp-multicast-port").help("UDP multicast port").type(Integer.class);
        argpars.addArgument("-r", "--rmi-port").help("RMI communication port").type(Integer.class);
        argpars.addArgument("-d", "--data-dir").help("server data directory").type(String.class);
        argpars.addArgument("-c", "--config-file").help("server configuration file path").type(String.class);

        Namespace ns = null;

        try {
            ns = argpars.parseArgs(args);
            Server s = new Server();
            s.bootstrap(ns);
            s.serve();
        }
        catch(ArgumentParserException ex) {
            argpars.printHelp();
            System.exit(1);
        }
        catch (RemoteException | AlreadyBoundException ex) {
            System.out.println("RMI Exception:" + ex.getMessage());
        }
    }
}
