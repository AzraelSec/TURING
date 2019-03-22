package it.azraelsec.Client;

import it.azraelsec.Protocol.Commands;
import it.azraelsec.Protocol.Communication;
import it.azraelsec.Protocol.RemoteRegistration;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Client {
    private static int TCP_PORT = 1337;
    private static int UDP_PORT = 1338;
    private static int RMI_PORT = 3400;
    private static String SERVER_ADDRESS = "127.0.0.1";
    private static String DATA_DIR = "./client_data/";
    private String authenticationToken;
    private NotificationThread notificationThread;
    private Socket clientSocket;
    private DataOutputStream clientOutputStream;
    private DataInputStream clientInputStream;
    private String onEditingFilename = null;

    public Client() {
        authenticationToken = null;
    }

    private boolean isLogged() {
        return authenticationToken != null;
    }

    private void setup(Namespace cmdOptions) {
        Optional.ofNullable(cmdOptions.getString("config_file")).ifPresent(this::loadConfig);
        TCP_PORT = Optional.ofNullable(cmdOptions.getInt("tcp_command_port")).orElseGet(() -> TCP_PORT);
        UDP_PORT = Optional.ofNullable(cmdOptions.getInt("udp_port")).orElseGet(() -> UDP_PORT);
        RMI_PORT = Optional.ofNullable(cmdOptions.getInt("rmi_port")).orElseGet(() -> RMI_PORT);
        DATA_DIR = Optional.ofNullable(cmdOptions.getString("data_dir")).orElseGet(() -> DATA_DIR);
        SERVER_ADDRESS = Optional.ofNullable(cmdOptions.getString("server_address")).orElseGet(() -> SERVER_ADDRESS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("TURING Client is shutting down...");
        }));
        checkDataDirectory();
        System.out.println(String.format("TCP_PORT: %s\nUDP_PORT: %s\nRMI_PORT: %s\nSERVER_ADDRESS: %s", TCP_PORT, UDP_PORT, RMI_PORT, SERVER_ADDRESS));
    }

    private void loadConfig(String filePath) {
        if (checkConfigFile(filePath)) {
            try {
                JSONObject configs = new JSONObject(Files.lines(new File(filePath).toPath()).collect(Collectors.joining("\n")));
                TCP_PORT = configs.has("TCP_PORT") ? configs.getInt("TCP_PORT") : TCP_PORT;
                UDP_PORT = configs.has("UDP_PORT") ? configs.getInt("UDP_PORT") : UDP_PORT;
                RMI_PORT = configs.has("RMI_PORT") ? configs.getInt("RMI_PORT") : RMI_PORT;
                DATA_DIR = configs.has("DATA_DIR") ? configs.getString("DATA_DIR") : DATA_DIR;
                SERVER_ADDRESS = configs.has("SERVER_ADDRESS") ? configs.getString("SERVER_ADDRESS") : SERVER_ADDRESS;
            } catch (Exception ex) {
                System.out.println("JSON parsing error for file:" + filePath);
                System.out.println("That's the reason:" + ex.getMessage());
            }
        } else System.out.println("Configuration file not found");
    }

    private boolean checkConfigFile(String filePath) {
        File configFile = new File(filePath);
        return configFile.isFile() && configFile.exists();
    }

    private void checkDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.isDirectory() || !dataDir.exists()) dataDir.mkdirs();
    }

    /*
     * MAIN METHOD
     * */

    public static void main(String[] args) {
        ArgumentParser argpars = ArgumentParsers.newFor("TURING Client")
                .build()
                .defaultHelp(true)
                .description("TURING distributed program client");
        argpars.addArgument("-t", "--tcp-command-port").help("TCP commands port").type(Integer.class);
        argpars.addArgument("-u", "--udp-multicast-port").help("UDP multicast port").type(Integer.class);
        argpars.addArgument("-r", "--rmi-port").help("RMI communication port").type(Integer.class);
        argpars.addArgument("-d", "--data-dir").help("client data directory").type(String.class);
        argpars.addArgument("-c", "--config-file").help("server configuration file path").type(String.class);
        argpars.addArgument("-s", "--server-address").help("server IP address").type(String.class);

        Namespace ns = null;

        try {
            ns = argpars.parseArgs(args);
        } catch (ArgumentParserException ex) {
            argpars.printHelp();
            System.exit(1);
        }

        Client client = new Client();
        client.setup(ns);

        try {
            client.commandDispatchingLoop();
        } catch (IOException | NotBoundException ex) {
            client.printExeption(ex);
        }

        /*
        try {
            client.register("prova", "prova");
            client.login("prova", "prova");
            client.create("Documento",2);
            client.edit("Documento", 1, null);
            try {
                Thread.sleep(40000);
            } catch (InterruptedException e) {
            }
            client.editEnd();
            client.showSection("Documento", 1, null);
            client.logout();
        } catch (NotBoundException | IOException ex) {
            System.out.println("Remote Exception:" + ex.getMessage());
        }*/
    }

    private void commandDispatchingLoop() throws NotBoundException, RemoteException, IOException {
        String command = null;
        Scanner input = new Scanner(System.in);
        do {
            System.out.print("turing@127.0.0.1# ");
            String argsLine = input.nextLine();
            String[] args = argsLine.split(" ");
            if(argsLine.length() > 0 && args.length > 0) {
                command = args[0];
                try {
                    switch(command) {
                        case "exit": break;
                        case "register":
                            if(args.length > 2) {
                                String username = args[1];
                                String password = args[2];
                                if(this.register(username, password))
                                    System.out.println("User " + username + " correctly registered!");
                                else
                                    System.out.println("Error in user registration! Probably user already exists!");
                            }
                            else throw new CommandDispatchingException();
                            break;
                        case "login":
                            if(args.length > 2) {
                                String username = args[1];
                                String password = args[2];
                                this.login(username, password);
                            }
                            break;
                        case "create":
                            if(args.length > 1){
                                try {
                                    String docName = args[1];
                                    int secNum = Integer.valueOf(args[2]);
                                    this.create(docName, secNum);
                                } catch (NumberFormatException ex) {
                                    throw new CommandDispatchingException();
                                }
                            }
                            break;
                        case "edit":
                            if(args.length > 2) {
                                String tmpFile = null;
                                if(args.length > 3) tmpFile = args[3];
                                try {
                                    String docName = args[1];
                                    int secNum = Integer.valueOf(args[2]);
                                    this.edit(docName, secNum, tmpFile);
                                } catch (NumberFormatException ex) {
                                    throw new CommandDispatchingException();
                                }
                            }
                            break;
                        case "stopedit":
                            this.editEnd();
                            break;
                        case "showsec":
                            if(args.length > 2) {
                                String outputFile = null;
                                if(args.length > 3) outputFile = args[3];
                                try {
                                    String docName = args[1];
                                    int secNum = Integer.valueOf(args[2]);
                                    this.showSection(docName, secNum, outputFile);
                                } catch (NumberFormatException ex) {
                                    throw new CommandDispatchingException();
                                }
                            }
                            break;
                        case "logout":
                            this.logout();
                            break;
                        case "list":
                            this.documentsList();
                            break;
                        case "help":
                            this.printCommandsHelp();
                            break;
                    }
                }
                catch (CommandDispatchingException ex) {
                    System.out.println("Error in command arguments dispatching");
                }
            }
        } while(command.compareTo("exit") != 0);
    }

    private boolean register(String username, String password) throws RemoteException, NotBoundException {
        RemoteRegistration registrationService;
        Registry registry = LocateRegistry.getRegistry(SERVER_ADDRESS, RMI_PORT);
        registrationService = (RemoteRegistration) registry.lookup(RemoteRegistration.NAME);
        return registrationService.register(username, password);
    }

    private void edit(String docName, int secNumber, String chosenFilename) {
        if (isLogged()) {
            onEditingFilename = chosenFilename != null ? chosenFilename : DATA_DIR + docName + "_" + secNumber;
            try (FileChannel fileChannel = FileChannel.open(Paths.get(onEditingFilename), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 OutputStream fileStream = Channels.newOutputStream(fileChannel)) {
                Communication.sendAndReceiveStream(clientOutputStream, clientInputStream, System.out::println, fileStream, System.err::println, Commands.EDIT, docName, secNumber);
            } catch (IOException ex) {
                printExeption(ex);
            }
        }
        else System.out.println("You're not logged in");
    }

    private void login(String username, String password) throws IOException {
        if (!isLogged()) {
            ServerSocket socket = new ServerSocket(0);
            notificationThread = new NotificationThread(socket, this);
            int notificationPort = socket.getLocalPort();
            notificationThread.start();
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(SERVER_ADDRESS, TCP_PORT));
            clientOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            clientInputStream = new DataInputStream(clientSocket.getInputStream());
            Communication.send(clientOutputStream, clientInputStream, token -> authenticationToken = token, System.err::println, Commands.LOGIN, notificationPort, username, password);
        }
        else System.out.println("You're already logged in");
    }

    private void logout() {
        if (isLogged()) {
            Communication.send(clientOutputStream, clientInputStream, null, null, Commands.LOGOUT);
            try {
                notificationThread.getSocket().close();
                notificationThread.interrupt();
                clientInputStream.close();
                clientOutputStream.close();
                clientSocket.close();
                authenticationToken = null;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        else System.out.println("You're not logged in");
    }

    private void editEnd() {
        if (isLogged()) {
            Communication.send(clientOutputStream, clientInputStream, s -> {
                try (FileChannel fileChannel = FileChannel.open(Paths.get(onEditingFilename), StandardOpenOption.READ);
                     InputStream stream = Channels.newInputStream(fileChannel)) {
                    Communication.receiveAndSendStream(clientInputStream, clientOutputStream, stream);
                    onEditingFilename = null;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }, System.err::println, Commands.EDIT_END);
        }
        else System.out.println("You're not logged in");
    }

    private void create(String docName, int secNumber) {
        if (isLogged())
            Communication.send(clientOutputStream, clientInputStream, System.out::println, System.err::println, Commands.CREATE, docName, secNumber);
        else System.out.println("You're not logged in");
    }

    private void showSection(String docName, int secNumber, String chosenFilename) {
        if (isLogged()) {
            String filename = chosenFilename != null ? chosenFilename : DATA_DIR + docName + "_" + secNumber;
            try (FileChannel fileChannel = FileChannel.open(Paths.get(filename), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 OutputStream fileStream = Channels.newOutputStream(fileChannel)) {
                Communication.sendAndReceiveStream(clientOutputStream, clientInputStream, editor -> {
                    if(editor.compareTo("None") != 0)
                        System.out.println(String.format("%s is editing the section right now", editor));
                    else System.out.println("None is editing this section");
                }, fileStream, System.err::println, Commands.EDIT, docName, secNumber);
            } catch (IOException ex) {
                printExeption(ex);
            }
        } else System.out.println("You're not logged in");
    }

    private void documentsList() {
        if(isLogged()) {
            Communication.send(clientOutputStream, clientInputStream, System.out::println, System.err::println, Commands.LIST);
        } else System.out.println("You're not logged in");
    }

    /**
    * todo: to implement
    * @deprecated
    * */
    private void showDocument(String docName, String outputName) {
        if (isLogged()) {
            String filename = DATA_DIR + outputName;
            try (FileChannel fileChannel = FileChannel.open(Paths.get(filename), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 OutputStream fileStream = Channels.newOutputStream(fileChannel)) {
                Communication.sendAndReceiveStream(clientOutputStream, clientInputStream, onEditingSections -> {
                    if(onEditingSections.compareTo("None") != 0)
                        System.out.println(String.format("These are the on editing sections: %s", onEditingSections));
                    else System.out.println("None is editing this document");
                }, fileStream, System.err::println, Commands.EDIT, docName);
            } catch (IOException ex) {
                printExeption(ex);
            }
        }
        else System.out.println("You're not logged in");
    }

    private void printExeption(Exception ex) {
        System.out.println("Error:" + ex.getMessage());
    }

    private void printCommandsHelp() {
        String message =
                "The following commands are available:\n" +
                "  help: to show this help message\n" +
                "  register USER PASS: to register a new account with username USER and password PASS\n" +
                "  login USER PASS: to login using USER and PASS credentials\n"+
                "  create DOC SEC: to create a new document named DOC and containing SEC sections\n" +
                "  edit DOC SEC (TMP): to edit the section SEC of DOC document (using TMP temporary filename)\n" +
                "  stopedit: to stop the current editing session\n" +
                "  showsec DOC SEC (OUT): to download the content of the SEC section of DOC document (using OUT output filename)\n" +
                "  logout: to logout\n" +
                "  list: to list all the documents you are able to see and edit";
        System.out.println(message);
    }
}
