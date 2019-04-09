package it.azraelsec.Client;

import it.azraelsec.Chat.ChatMessage;
import it.azraelsec.Chat.MessageReceiver;
import it.azraelsec.Chat.MessageSender;
import it.azraelsec.Notification.NotificationClientThread;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * The {@code Client} class represents the TURING Client execution and provides all the methods
 * which allows the user to interact with the {@code Server}.
 * <p>
 * It smoothly manages the {@code NotificationClientThread}, {@code MessageReceiver}, {@code MessageSender}
 * user interaction (CLI) life-cycle.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class Client {
    private static int TCP_PORT = 1337;
    public static int UDP_PORT = 1338;
    private static int RMI_PORT = 3400;
    private static String SERVER_ADDRESS = "127.0.0.1";
    private static String DATA_DIR = "./client_data/";
    private Socket clientSocket;
    private DataOutputStream clientOutputStream;
    private DataInputStream clientInputStream;
    private NotificationClientThread notificationThread;
    private final MessageReceiver messageReceiver;
    private MessageSender messageSender;
    private LocalSession session;

    /**
     * Initializes {@code NotificationClientThread}, {@code MessageReceiver} and {@code LocalSession} objects.
     */
    public Client() {
        notificationThread = new NotificationClientThread();
        messageReceiver = new MessageReceiver();
        session = null;
    }

    /**
     * Routine that setups the {@code Client} configuration based on the command line arguments passed during
     * the program startup.
     * <p>
     * It needs to be called before the {@code connect} method.
     *
     * @see LocalSession
     * @param cmdOptions    the command line arguments
     */
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

    /**
     * Tries to connect to the {@code Server} running instance, create references to the
     * streams for {@code Socket} I/O operations and starts the {@code NotificationClientThread}.
     * <p>
     * This method needs to be called after {@code setup} method execution.
     *
     * @see DataInputStream
     * @see DataOutputStream
     * @throws IOException  if I/O errors occur
     */
    private void connect() throws IOException {
        notificationThread.start();
        clientSocket = new Socket();
        clientSocket.connect(new InetSocketAddress(SERVER_ADDRESS, TCP_PORT));
        clientOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        clientInputStream = new DataInputStream(clientSocket.getInputStream());
        messageReceiver.start();
        messageSender = MessageSender.create();
        if(messageSender == null) throw new IOException();
    }

    /**
     * Loads the JSON configuration file passed as argument.
     *
     * @param filePath  JSON configuration file path
     */
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

    /**
     * Checks if the JSON configuration file exists and the path is a valid one.
     *
     * @param filePath  file path
     * @return  true if the the file exists and is a valid file, false otherwise
     */
    private boolean checkConfigFile(String filePath) {
        File configFile = new File(filePath);
        return configFile.isFile() && configFile.exists();
    }

    /**
     * Check if the data directory exists and is a valid directory, otherwise it creates it.
     */
    private void checkDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.isDirectory() || !dataDir.exists()) dataDir.mkdirs();
    }


    /**
     * Program Entry Point.
     *
     * @param args  command arguments
     */
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack" , "true");
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
            argpars.handleError(ex);
            System.exit(1);
        }

        Client client = new Client();
        client.setup(ns);

        try {
            client.connect();
            client.commandDispatchingLoop();
        } catch (IOException | NotBoundException ex) {
            client.printException(ex);
        } finally {
            try {
                client.notificationThread.interrupt();
                client.messageReceiver.interrupt();
                client.clientSocket.close();
                client.clientInputStream.close();
                client.clientOutputStream.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Register a new {@code User} into {@code UserDB} via RMI invocation.
     *
     * @param username  user's username
     * @param password  user's password
     * @return  true if the register succeeded, false otherwise
     * @throws RemoteException  if RMI exception occurs
     * @throws NotBoundException if registry lookup fault occurs
     */
    private boolean register(String username, String password) throws RemoteException, NotBoundException {
        RemoteRegistration registrationService;
        Registry registry = LocateRegistry.getRegistry(SERVER_ADDRESS, RMI_PORT);
        registrationService = (RemoteRegistration) registry.lookup(RemoteRegistration.NAME);
        return registrationService.register(username, password);
    }

    /**
     * Starts an edit session for a specific {@code Document}'s {@code Section}.
     * <p>
     * Initializes a new multicast group for a {@code MessageReceiver} object too.
     *
     * @param docName   document filename
     * @param secNumber section index
     * @param chosenFilename    output filename
     */
    private void edit(String docName, int secNumber, String chosenFilename) {
        if (session != null) {
            String filepath = chosenFilename != null ? chosenFilename : DATA_DIR + docName + "_" + secNumber;
            try (FileChannel fileChannel = FileChannel.open(Paths.get(filepath), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 OutputStream fileStream = Channels.newOutputStream(fileChannel)) {
                Communication.sendAndReceiveStream(clientOutputStream, clientInputStream, address -> {
                    session.setOnEdit(filepath);
                    long dAddress = Long.parseLong(address);
                    try {
                        messageReceiver.setNewGroup(dAddress);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }, fileStream, System.err::println, Commands.EDIT, docName, secNumber);
            } catch (IOException ex) {
                printException(ex);
            }
        } else System.err.println("You're not logged in");
    }

    /**
     * Authenticates the user into system trying to validate the user's password and informs the
     * server about its notification's port used by its {@code NotificationClientThread}.
     * <p>
     * It starts a new {@code LocalSession} object that collects all the session's information.
     *
     * @param username  user username
     * @param password  user password
     * @param notificationPort
     */
    private void login(String username, String password, int notificationPort) {
        if (session == null)
            Communication.send(clientOutputStream, clientInputStream, token -> {
                session = new LocalSession(token, username);
                System.out.println("Correctly logged in as " + username);
            }, System.err::println, Commands.LOGIN, username, password, notificationPort);
        else System.err.println("You're already logged in");
    }

    /**
     * Kills the {@code LocalSession}, stops the {@code NotificationClientThread} and clears the
     * notifications' list.
     */
    private void logout() {
        if (session != null) {
            if(!session.isEditing()) {
                session = null;
                notificationThread.clearNotificationList();
                Communication.send(clientOutputStream, clientInputStream, null, null, Commands.LOGOUT);
            } else System.err.println("You should 'stopedit' before logging out");
        } else System.err.println("You're not logged in");
    }

    /**
     * Stops a {@code Document}'s {@code Section} editing and imposes the {@code MessageReceiver} to
     * leave the actual multicast group.
     */
    private void editEnd() {
        if (session != null) {
            if (session.isEditing()) {
                Communication.send(clientOutputStream, clientInputStream, s -> {
                    try (FileChannel fileChannel = FileChannel.open(Paths.get(session.getOnEditing()), StandardOpenOption.READ);
                         InputStream stream = Channels.newInputStream(fileChannel)) {
                        Communication.receiveAndSendStream(clientInputStream, clientOutputStream, stream);
                        session.setOnEdit(null);
                        messageReceiver.setNewGroup(0L);
                    } catch (IOException ex) {
                        printException(ex);
                    }
                }, System.err::println, Commands.EDIT_END);
            } else System.err.println("You're not editing any section");
        } else System.err.println("You're not logged in");
    }

    /**
     * Creates a new {@code Document} remote object.
     *
     * @param docName   new document's name
     * @param secNumber number of sections the new document has got
     */
    private void create(String docName, int secNumber) {
        if (session != null)
            Communication.send(clientOutputStream, clientInputStream, System.out::println, System.err::println, Commands.CREATE, docName, secNumber);
        else System.err.println("You're not logged in");
    }

    /**
     * Reads the content of the requested {@code Section} and, if somebody is editing it, returns
     * the editor's name.
     * <p>
     * If the {@code chosenFilename} is null, the default name is used: {@code docName}_{@code secNumber}.
     *
     * @param docName   document's name
     * @param secNumber target section
     * @param chosenFilename    output filename or null
     */
    private void showSection(String docName, int secNumber, String chosenFilename) {
        if (session != null) {
            String filename = chosenFilename != null ? chosenFilename : DATA_DIR + docName + "_" + secNumber;
            try (FileChannel fileChannel = FileChannel.open(Paths.get(filename), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 OutputStream fileStream = Channels.newOutputStream(fileChannel)) {
                Communication.sendAndReceiveStream(clientOutputStream, clientInputStream, editor -> {
                    if (editor.compareTo("None") != 0)
                        System.out.println(String.format("%s is editing the section right now", editor));
                    else System.out.println("None is editing this section");
                }, fileStream, System.err::println, Commands.SHOW_SECTION, docName, secNumber);
            } catch (IOException ex) {
                printException(ex);
            }
        } else System.err.println("You're not logged in");
    }

    /**
     * Gets the list of {@code Document}s on which the {@code User} has permissions.
     */
    private void documentsList() {
        if (session != null)
            Communication.send(clientOutputStream, clientInputStream, System.out::println, System.err::println, Commands.LIST);
        else System.err.println("You're not logged in");
    }

    /**
     * Shares a document with another {@code User}, giving him the permission to modify and see it.
     * <p>
     * When a {@code User} receives new permissions, a notification will be delivered to him.
     *
     * @param user  user's username
     * @param docName   document's name
     */
    private void share(String user, String docName) {
        Communication.send(clientOutputStream, clientInputStream, System.out::println, System.err::println, Commands.SHARE, user, docName);
    }

    /**
     * Gets the entire requested {@code Document} concatenating all its {@code Section}s together.
     * <p>
     * If the {@code outputName} is null, the {@code docName} value is used.
     *
     * @param docName   document's name
     * @param outputName    output filename
     */
    private void showDocument(String docName, String outputName) {
        if (session != null) {
            String filename = DATA_DIR + (outputName == null ? docName : outputName);
            try (FileChannel fileChannel = FileChannel.open(Paths.get(filename), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 OutputStream fileStream = Channels.newOutputStream(fileChannel)) {
                Communication.sendAndReceiveStream(clientOutputStream, clientInputStream, onEditingSections -> {
                    if (onEditingSections.compareTo("None") != 0)
                        System.out.println(String.format("These are the on editing sections: %s", onEditingSections));
                    else System.out.println("None is editing this document");
                }, fileStream, System.err::println, Commands.SHOW_DOCUMENT, docName);
            } catch (IOException ex) {
                printException(ex);
            }
        } else System.err.println("You're not logged in");
    }


    /**
     * Prints out all the notifications collected since the last method invocation.
     */
    private void printNews() {
        if (session != null) {
            List<String> notifications = notificationThread.getAllNotifications();
            if (!notifications.isEmpty())
                System.out.println("You have permission on these new documents: " + String.join(",", notifications));
            else System.err.println("No news available");
        } else System.err.println("You're not logged in");
    }

    /**
     * Shows all the received {@code ChatMessage}s received since the last method invocation.
     */
    private void showMessages() {
        if(session != null) {
            if(session.isEditing()) {
                ChatMessage[] unreadMessages = messageReceiver.getMessages();
                for(ChatMessage message : unreadMessages)
                    System.out.println(message);
            } else System.err.println("You're not editing any document");
        } else System.err.println("You're not logged in");
    }

    /**
     * Sends a new {@code ChatMessage} UDP multicast packet to every listening {@code MessageReceiver}.
     *
     * @param text  message text
     */
    private void sendMessage(String text) {
        if(session != null) {
            if(session.isEditing()) {
                InetAddress multicastAddress;
                if((multicastAddress = messageReceiver.getActiveGroup()) != null) {
                    try {
                        ChatMessage message = new ChatMessage(session.getUsername(), text);
                        InetSocketAddress groupAddress = new InetSocketAddress(multicastAddress, UDP_PORT);
                        messageSender.sendMessage(message, groupAddress);
                    } catch (IOException ex) {
                        printException(ex);
                    }
                } else System.err.println("Generic message sending error");
            } else System.err.println("You're not editing any document");
        } else System.err.println("You're not logged in");
    }

    /**
     * Prints out a generic {@code Exception} in a friendly format.
     *
     * @param ex
     */
    private void printException(Exception ex) {
        System.err.println(ex.getMessage());
    }

    /**
     * Prints out and help message that sums up the {@code Client} commands list.
     */
    private void printCommandsHelp() {
        String message =
                "The following commands are available:\n" +
                        "  help: to show this help message\n\n" +
                        "  register USER PASS: to register a new account with username USER and password PASS\n" +
                        "  login USER PASS: to login using USER and PASS credentials\n" +
                        "  create DOC SEC: to create a new document named DOC and containing SEC sections\n" +
                        "  edit DOC SEC (TMP): to edit the section SEC of DOC document (using TMP temporary filename)\n" +
                        "  stopedit: to stop the current editing session\n" +
                        "  showsec DOC SEC (OUT): to download the content of the SEC section of DOC document (using OUT output filename)\n" +
                        "  showdoc DOC (OUT): to download the content concatenation of all the document's sections (using OUT output filename)\n" +
                        "  logout: to logout\n" +
                        "  list: to list all the documents you are able to see and edit\n" +
                        "  share USER DOC: to share a document with someone\n" +
                        "  news: to get all the news\n\n" +
                        "  receive: to get all the unread chat messages\n" +
                        "  send TEXT: to send the TEXT message into the document chat";
        System.out.println(message);
    }

    /**
     * Manages the commands dispatching loop that iterates over the {@code String} given to the
     * prompt, interprets the corresponding command and executes the respective action.
     *
     * @throws NotBoundException    if a RMI registration error occurs
     * @throws IOException  if a registration I/O error occurs
     */
    private void commandDispatchingLoop() throws NotBoundException, IOException {
        String command = null;
        Scanner input = new Scanner(System.in);
        boolean dispatchingShutdown = false;
        do {
            System.out.print("turing@127.0.0.1# ");
            String argsLine = input.nextLine();
            String[] args = argsLine.split(" ");
            if (argsLine.length() > 0 && args.length > 0) {
                command = args[0];
                try {
                    switch (command) {
                        case "exit":
                        case "quit":
                            if(session != null) System.err.println("You need to logout before");
                            else dispatchingShutdown = true;
                            break;
                        case "register":
                            if (args.length > 2) {
                                String username = args[1];
                                String password = args[2];
                                if (register(username, password))
                                    System.out.println("User " + username + " correctly registered!");
                                else
                                    System.out.println("Error in user registration: user probably already exists");
                            } else throw new CommandDispatchingException();
                            break;
                        case "login":
                            if (args.length > 2) {
                                String username = args[1];
                                String password = args[2];
                                login(username, password, notificationThread.getNotificationLocalPort());
                            } else throw new CommandDispatchingException();
                            break;
                        case "create":
                            if (args.length > 2) {
                                try {
                                    String docName = args[1];
                                    int secNum = Integer.valueOf(args[2]);
                                    create(docName, secNum);
                                } catch (NumberFormatException ex) {
                                    throw new CommandDispatchingException();
                                }
                            } else throw new CommandDispatchingException();
                            break;
                        case "edit":
                            if (args.length > 2) {
                                String tmpFile = null;
                                if (args.length > 3) tmpFile = args[3];
                                try {
                                    String docName = args[1];
                                    int secNum = Integer.valueOf(args[2]);
                                    edit(docName, secNum, tmpFile);
                                } catch (NumberFormatException ex) {
                                    throw new CommandDispatchingException();
                                }
                            } else throw new CommandDispatchingException();
                            break;
                        case "stopedit":
                            editEnd();
                            break;
                        case "showsec":
                            if (args.length > 2) {
                                String outputFile = null;
                                if (args.length > 3) outputFile = args[3];
                                try {
                                    String docName = args[1];
                                    int secNum = Integer.valueOf(args[2]);
                                    showSection(docName, secNum, outputFile);
                                } catch (NumberFormatException ex) {
                                    throw new CommandDispatchingException();
                                }
                            } else throw new CommandDispatchingException();
                            break;
                        case "showdoc":
                            if (args.length > 1) {
                                String docName = args[1];
                                String outputFile = null;
                                if (args.length > 2) outputFile = args[2];
                                showDocument(docName, outputFile);
                            } else throw new CommandDispatchingException();
                            break;
                        case "logout":
                            logout();
                            break;
                        case "list":
                            documentsList();
                            break;
                        case "share":
                            if (args.length > 2) {
                                String username = args[1];
                                String docName = args[2];
                                share(username, docName);
                            } else throw new CommandDispatchingException();
                            break;
                        case "news":
                            printNews();
                            break;
                        case "receive":
                            showMessages();
                            break;
                        case "send":
                            if(args.length > 1){
                                String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                                sendMessage(text);
                            } else throw new CommandDispatchingException();
                            break;
                        case "help":
                            printCommandsHelp();
                            break;
                        default:
                            throw new CommandDispatchingException();
                    }
                } catch (CommandDispatchingException ex) {
                    System.err.println("Error in command arguments dispatching");
                }
            }
        } while (!dispatchingShutdown);
    }
}
