package it.azraelsec.Server;

import it.azraelsec.Chat.CDAManager;
import it.azraelsec.Document.DocumentsDatabase;
import it.azraelsec.Protocol.RemoteRegistration;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The {@code Server} class represents the process that serves the {@code Client} connection and spawn new
 * {@code TCPConnectionDispatcher} threads to handler them.
 * <p>
 * The server uses a simple approach waiting new incoming connection through a blocking {@code accept} method
 * invocation.
 *
 * @see DocumentsDatabase
 * @see CDAManager
 * @see ExecutorService
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class Server {
    private static int TCP_PORT = 1337;
    private static int RMI_PORT = 3400;
    private static String DATA_DIR = "./server_data/";


    private UsersDB usersDB;
    private DocumentsDatabase documentDatabase;
    private final OnlineUsersDB onlineUsersDB;
    private final ExecutorService TCPConnectionDispatcher;
    private final CDAManager cdaManager;

    /**
     * Initializes the {@code Server}.
     */
    public Server() {
        usersDB = null;
        documentDatabase = null;
        TCPConnectionDispatcher = Executors.newCachedThreadPool();
        onlineUsersDB = new OnlineUsersDB();
        cdaManager = new CDAManager();
    }

    /**
     * Gets the static {@code DATA_DIR} {@code String}.
     *
     * @return  DATA_DIR string
     */
    static String getDataDirectoryPath() {
        return DATA_DIR;
    }

    /**
     * Acquires the command line arguments and compute the options based on these values, that ones found
     * in the JSON configuration file and tha default ones.
     *
     * @param cmdOptions    command line options
     * @throws RemoteException  if RMI connection error occurs
     */
    private void bootstrap(Namespace cmdOptions) throws RemoteException {
        Optional.ofNullable(cmdOptions.getString("config_file")).ifPresent(this::loadConfig);
        TCP_PORT = Optional.ofNullable( cmdOptions.getInt("tcp_command_port") ).orElseGet( () -> TCP_PORT );
        RMI_PORT = Optional.ofNullable( cmdOptions.getInt("rmi_port") ).orElseGet( () -> RMI_PORT );
        DATA_DIR = Optional.ofNullable( cmdOptions.getString("data_dir") ).orElseGet( () -> DATA_DIR );
        checkDataDirectory();
        usersDB = initUsersDB();
        documentDatabase = initDocumentsDB();
        RMIInit();
        System.out.println(String.format("TCP_PORT: %s\nRMI_PORT: %s\nDATA_DIR: %s", TCP_PORT, RMI_PORT, DATA_DIR));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("TURING Server is shutting down...");
            TCPConnectionDispatcher.shutdown();
            storeUsersDB();
            storeDocumentsDB();
        }));
    }

    /**
     * Makes the underlining TCP {@code ServerSocket} waiting for new incoming connections and spawns new
     * {@code TCPRequestHandler} to handle them in a new {@code Thread}.
     * <p>
     * This method just does not end up because a server is, for definition, a process that always serves.
     */
    private void serve() {
        try(ServerSocket TCPServer = new ServerSocket()) {
            TCPServer.bind(new InetSocketAddress(TCP_PORT));
            System.out.println("ADDRESS: " + InetAddress.getLocalHost().toString());

            while(true) {
                Socket socket = TCPServer.accept();
                System.out.println("New TCP connection: " + socket.getRemoteSocketAddress().toString());
                TCPConnectionDispatcher.submit(new TCPRequestHandler(onlineUsersDB, usersDB, documentDatabase, cdaManager, socket));
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Stores {@code UsersDB} object through serialization.
     *
     * @return  true if file has been serialized, false otherwise
     */
    private boolean storeUsersDB() {
        try(ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(DATA_DIR + "db.dat"))) {
            output.writeObject(usersDB);
            return true;
        }
        catch(IOException ex) {
            return false;
        }
    }

    /**
     * Loads {@code UsersDB} object though deserialization.
     *
     * @return  {@code UsersDB} if it exists, null otherwise
     */
    private UsersDB loadUsersDB() {
        try(ObjectInputStream input = new ObjectInputStream(new FileInputStream(DATA_DIR + "db.dat"))) {
            return (UsersDB) input.readObject();
        }
        catch(IOException | ClassNotFoundException ex) {
            return null;
        }
    }

    /**
     * Stores {@code DocumentsDatabase} object through serialization.
     *
     * @return  true if file has been serialized, false otherwise
     */
    private boolean storeDocumentsDB() {
        try(ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(DATA_DIR + "docs.dat"))) {
            output.writeObject(documentDatabase);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Loads {@code DocumentsDatabase} object through deserialization.
     *
     * @return {@code DocumentsDatabase} if it exists, null otherwise
     */
    private DocumentsDatabase loadDocumentsDB() {
        try(ObjectInputStream input = new ObjectInputStream(new FileInputStream(DATA_DIR + "docs.dat"))) {
            return (DocumentsDatabase) input.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            return null;
        }
    }

    /**
     * Returns a pre-existent {@code DocumentsDatabase} if it exists, a new one otherwise.
     *
     * @return  valid {@code DocumentsDatabase}
     */
    private DocumentsDatabase initDocumentsDB() {
        DocumentsDatabase loadedDocumentsDB = loadDocumentsDB();
        return loadedDocumentsDB == null ? new DocumentsDatabase() : loadedDocumentsDB;
    }

    /**
     * Returns a pre-existent {@code UsersDB} if it exists, a new one otherwise.
     *
     * @return valide {@code UsersDB}
     */
    private UsersDB initUsersDB() {
        UsersDB loadedUsersDB = loadUsersDB();
        return loadedUsersDB == null ? new UsersDB() : loadedUsersDB;
    }

    /**
     * Checks if the {@code DATA_DIR} exists, and creates it it not.
     */
    private void checkDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if(!dataDir.isDirectory() || !dataDir.exists()) dataDir.mkdirs();
    }

    /**
     * Tries to parse the JSON configuration file, collecting all the configurations.
     *
     * @param filePath  configuration file path
     */
    private void loadConfig(String filePath) {
        if(checkConfigFile(filePath))
        {
            try{
                JSONObject configs = new JSONObject(Files.lines(new File(filePath).toPath()).collect(Collectors.joining("\n")));
                TCP_PORT = configs.has("TCP_PORT") ? configs.getInt("TCP_PORT") : TCP_PORT;
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

    /**
     * Checks if the JSON configuration file exists and is a valid file type.
     *
     * @param filePath  configuration file path
     * @return  true if {@code filePath} exists and is a valid file type, false otherwise
     */
    private boolean checkConfigFile(String filePath) {
        File configFile = new File(filePath);
        return configFile.isFile() && configFile.exists();
    }
    private void RMIInit() throws RemoteException {
        RegistrationStub remoteObject = new RegistrationStub(usersDB);
        LocateRegistry.createRegistry(RMI_PORT);
        Registry registry = LocateRegistry.getRegistry(RMI_PORT);
        registry.rebind(RemoteRegistration.NAME, remoteObject);
    }

    /**
     * Entry Point
     * @param args command line arguments
     */
    public static void main( String[] args )
    {
        ArgumentParser argpars = ArgumentParsers.newFor("TURING it.azraelsec.Server")
            .build()
            .defaultHelp(true)
            .description("TURING distributed program server");
        argpars.addArgument("-t", "--tcp-command-port").help("TCP commands port").type(Integer.class);
        argpars.addArgument("-r", "--rmi-port").help("RMI communication port").type(Integer.class);
        argpars.addArgument("-d", "--data-dir").help("server data directory").type(String.class);
        argpars.addArgument("-c", "--config-file").help("server configuration file path").type(String.class);

        Namespace ns;

        try {
            ns = argpars.parseArgs(args);
            Server s = new Server();
            s.bootstrap(ns);
            s.serve();
        }
        catch(ArgumentParserException ex) {
            argpars.handleError(ex);
            System.exit(1);
        }
        catch (RemoteException ex) {
            System.out.println("RMI Exception:" + ex.getMessage());
        }
    }
}
