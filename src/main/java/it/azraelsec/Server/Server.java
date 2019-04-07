package it.azraelsec.Server;

import it.azraelsec.Chat.CDAManager;
import it.azraelsec.Documents.Document;
import it.azraelsec.Documents.DocumentsDatabase;
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

public class Server {
    private static int TCP_PORT = 1337;
    private static int RMI_PORT = 3400;
    private static String DATA_DIR = "./server_data/";


    private UsersDB usersDB;
    private DocumentsDatabase documentDatabase;
    private final OnlineUsersDB onlineUsersDB;
    private final ExecutorService TCPConnectionDispatcher;
    private final CDAManager cdaManager;

    public Server() {
        usersDB = null;
        documentDatabase = null;
        TCPConnectionDispatcher = Executors.newCachedThreadPool();
        onlineUsersDB = new OnlineUsersDB();
        cdaManager = new CDAManager();
    }

    public static String getDataDirectoryPath() {
        return DATA_DIR;
    }

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

    private boolean storeDocumentsDB() {
        try(ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(DATA_DIR + "docs.dat"))) {
            output.writeObject(documentDatabase);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private DocumentsDatabase loadDocumentsDB() {
        try(ObjectInputStream input = new ObjectInputStream(new FileInputStream(DATA_DIR + "docs.dat"))) {
            return (DocumentsDatabase) input.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            return null;
        }
    }

    private DocumentsDatabase initDocumentsDB() {
        DocumentsDatabase loadedDocumentsDB = loadDocumentsDB();
        return loadedDocumentsDB == null ? new DocumentsDatabase() : loadedDocumentsDB;
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
    private void RMIInit() throws RemoteException {
        RegistrationStub remoteObject = new RegistrationStub(usersDB, onlineUsersDB);
        LocateRegistry.createRegistry(RMI_PORT);
        Registry registry = LocateRegistry.getRegistry(RMI_PORT);
        registry.rebind(RemoteRegistration.NAME, remoteObject);
    }

    /***
     * Main method
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
