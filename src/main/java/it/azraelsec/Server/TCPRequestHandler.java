package it.azraelsec.Server;

import it.azraelsec.Chat.CDAManager;
import it.azraelsec.Documents.Document;
import it.azraelsec.Documents.DocumentsDatabase;
import it.azraelsec.Documents.Section;
import it.azraelsec.Notification.NotificationServerThread;
import it.azraelsec.Protocol.Commands;
import it.azraelsec.Protocol.Communication;
import it.azraelsec.Protocol.Execution;
import it.azraelsec.Protocol.Result;


import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code TCPRequestHandler} class extends {@code Runnable} and represents a new {@code Client} connection
 * execution and {@code Commands} dispatching and interpreting.
 * <p>
 * The {@code TCPRequestHandler} instance hold the TCP command connection active since the beginning to the
 * end of the conversation between {@code Client} and {@code Server}.
 * <p>
 * The {@code NotificationServerThread} object is instanced only after a login request and is shutdown when
 * its session ends up. This ensures a consistent reverse connection structure, in which the {@code Client}
 * acts like a server and vice versa.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class TCPRequestHandler implements Runnable {
    private CDAManager cdaManager;
    private OnlineUsersDB onlineUsersDB;
    private UsersDB usersDB;
    private DocumentsDatabase documentDatabase;
    private String sessionToken;
    private Section editingSection;
    private Document editingDocument;
    private Map<Commands, Execution> handlers;

    private Socket socket;
    private DataInputStream socketInputStream;
    private DataOutputStream socketOutputStream;

    private NotificationServerThread notificationThread;

    /**
     * Initializes the object and stores all the references to the global objects.
     *
     * @param onlineUsersDB    online users references
     * @param usersDB          users database
     * @param documentDatabase documents database
     * @param cdaManager       chat dynamic address manager
     * @param socket           socket
     * @throws IOException if an I/O error occurs
     */
    TCPRequestHandler(OnlineUsersDB onlineUsersDB, UsersDB usersDB, DocumentsDatabase documentDatabase, CDAManager cdaManager, Socket socket) throws IOException {
        this.cdaManager = cdaManager;
        this.onlineUsersDB = onlineUsersDB;
        this.usersDB = usersDB;
        this.socket = socket;
        this.documentDatabase = documentDatabase;
        socketInputStream = new DataInputStream(socket.getInputStream());
        socketOutputStream = new DataOutputStream(socket.getOutputStream());
        handlers = new HashMap<>();
        handlers.put(Commands.LOGIN, this::onLogin);
        handlers.put(Commands.LOGOUT, this::onLogout);
        handlers.put(Commands.EDIT, this::onEdit);
        handlers.put(Commands.EDIT_END, this::onEditEnd);
        handlers.put(Commands.CREATE, this::onCreate);
        handlers.put(Commands.SHOW_SECTION, this::onShowSection);
        handlers.put(Commands.SHOW_DOCUMENT, this::onShowDocument);
        handlers.put(Commands.LIST, this::onList);
        handlers.put(Commands.SHARE, this::onShare);
        sessionToken = null;
        editingSection = null;
        editingDocument = null;
    }

    /**
     * Receives {@code Commands} requests within a loop and manage them though the related handlers.
     */
    @Override
    public void run() {
        do Communication.receive(socketInputStream, socketOutputStream, handlers); while (true);
    }

    /**
     * {@code Commands#LOGIN} handler.
     * <p>
     * Tries to authenticate the given {@code User} through its {@code String} username and password
     * as command invocation arguments. If the authentication succeeds a {@code NotificationServerThread}
     * is run and a new {@code String} session token generated and sent back to the {@code Client}.
     *
     * @param args     connection arguments
     * @param sendback connection response
     */
    private void onLogin(Object[] args, Result sendback) {
        if (!isSessionAlive()) {
            User user;
            if ((user = usersDB.doLogin((String) args[0], (String) args[1])) != null) {
                String token;
                if ((token = onlineUsersDB.login(user)) != null) {
                    notificationThread = new NotificationServerThread(user, socket.getInetAddress().getHostName(), (Integer) args[2]);
                    notificationThread.start();
                    sessionToken = token;
                    System.out.println("New user logged in: " + args[0]);
                    sendback.send(Commands.SUCCESS, token);
                } else sendback.send(Commands.FAILURE, "Login failed: token generation failed");
            } else sendback.send(Commands.FAILURE, "Login failed: authentication error");
        } else sendback.send(Commands.FAILURE, "You're already logged in");
    }

    /**
     * {@code Commands#LOGOUT} handler.
     * <p>
     * Kills the actual session and stops the {@code NotificationServerThread}.
     *
     * @param args     connection arguments
     * @param sendback connection response
     */
    private void onLogout(Object[] args, Result sendback) {
        sessionToken = null;
        notificationThread.close();
        try {
            notificationThread.join();
        } catch (InterruptedException ignore) {
        }
        System.out.println("Client's gone out");
        sendback.send(Commands.SUCCESS, "Good-bye");
    }

    /**
     * {@code Commands#EDIT} handler.
     * <p>
     * Tries to get the permission to exclusively edit the target {@code Section} or manage the situation in which
     * another {@code User} is editing it. The actual {@code Section} version is sent (streamed) to the {@code Client}.
     * <p>
     * A new multicast address is requested to the {@code CDAManager} and sent back to the {@code Client}.
     *
     * @param args  connection arguments
     * @param sendback  connection response
     */
    private void onEdit(Object[] args, Result sendback) {
        if (isSessionAlive()) {
            String documentName = (String) args[0];
            Document doc;
            int sectionNumber = (Integer) args[1];
            if ((doc = documentDatabase.getDocumentByName(documentName)) != null) {
                User user;
                if ((user = onlineUsersDB.getUserByToken(sessionToken)) != null) {
                    if (doc.canAccess(user)) {
                        if (editingSection == null) {
                            Section section;
                            if ((section = doc.getSection(sectionNumber)) != null) {
                                if (section.tryToSetEditing(user)) {
                                    Long multicastAddr = cdaManager.getChatAddress(doc);
                                    if (multicastAddr > 0) {
                                        try {
                                            InputStream fileStream = section.getFileInputStream();
                                            sendback.send(Commands.SUCCESS, String.valueOf(multicastAddr));
                                            try {
                                                Communication.receiveAndSendStream(socketInputStream, socketOutputStream, fileStream);
                                                editingSection = section;
                                                editingDocument = doc;
                                            } catch (IOException ex) {
                                                sendback.send(Commands.FAILURE, ex.getMessage());
                                                fileStream.close();
                                            }
                                        } catch (IOException ex) {
                                            sendback.send(Commands.FAILURE, "Section's reading error: " + ex.getMessage());
                                        }
                                    } else sendback.send(Commands.FAILURE, "No multicast address available");
                                } else sendback.send(Commands.FAILURE, "Someone's already editing this file");
                            } else sendback.send(Commands.FAILURE, "Section's not found");
                        } else sendback.send(Commands.FAILURE, "You can modify one section at time");
                    } else sendback.send(Commands.FAILURE, "You haven't got permissions to modify this file");
                } else sendback.send(Commands.FAILURE, "User's token cannot be found");
            } else sendback.send(Commands.FAILURE, "Document doesn't exist");
        } else sendback.send(Commands.FAILURE, "You're not logged in");
    }

    /**
     * {@code Commands#EDIT_END} handler.
     * <p>
     * Ends the editing session up and receives the new {@code Section} version from the {@code Client}.
     * <p>
     * Imposes to {@code CDAManager} to check if the actual multicast group should be considered as free and
     * reallocated for another editing group or not.
     *
     * @param args  connection arguments
     * @param sendback  connection response
     */
    private void onEditEnd(Object[] args, Result sendback) {
        if (isSessionAlive()) {
            if (editingSection != null) {
                editingSection.tryToSetEditing(null);
                OutputStream fileStream = null;
                try {
                    sendback.send(Commands.SUCCESS, "Send me new version");
                    fileStream = editingSection.getWriteStream();
                    Communication.readFileFromSocket(socketInputStream, fileStream);
                    cdaManager.checkRemove(editingDocument);
                    editingSection = null;
                    editingDocument = null;
                } catch (IOException ex) {
                    sendback.send(Commands.FAILURE, ex.getMessage());
                    if (fileStream != null)
                        try {
                            fileStream.close();
                        } catch (IOException ex1) {
                            ex1.printStackTrace();
                        }
                }
            } else sendback.send(Commands.FAILURE, "You are not editing any file");
        } else sendback.send(Commands.FAILURE, "You're not logged in");
    }

    /**
     * {@code Commands#CREATE} handler.
     * <p>
     * Creates a new {@code Document} owned by the requesting {@code User}.
     *
     * @param args  connection arguments
     * @param sendback  connection response
     */
    private void onCreate(Object[] args, Result sendback) {
        if (isSessionAlive()) {
            User user = onlineUsersDB.getUserByToken(sessionToken);
            if (user != null) {
                try {
                    documentDatabase.createNewDocument(Server.getDataDirectoryPath(), (Integer) args[1], (String) args[0], user);
                    sendback.send(Commands.SUCCESS, "Document created");
                } catch (IOException ex) {
                    sendback.send(Commands.FAILURE, ex.getMessage());
                }
            } else sendback.send(Commands.FAILURE, "User's token cannot be found");
        } else sendback.send(Commands.FAILURE, "You're not logged in");
    }

    /**
     * {@code Commands#SHOW_SECTION} handler.
     * <p>
     * Gets the requested {@code Section}'s actual content and informs the {@code Client} about how many
     * editors there are and who they specifically are.
     *
     * @param args  connection arguments
     * @param sendback  connection response
     */
    private void onShowSection(Object[] args, Result sendback) {
        if (isSessionAlive()) {
            String documeentName = (String) args[0];
            Document doc;
            int sectionNumber = (Integer) args[1];
            if ((doc = documentDatabase.getDocumentByName(documeentName)) != null) {
                User user;
                if ((user = onlineUsersDB.getUserByToken(sessionToken)) != null) {
                    if (doc.canAccess(user)) {
                        Section section;
                        if ((section = doc.getSection(sectionNumber)) != null) {
                            User onEditingUser;
                            if ((onEditingUser = section.getUserOnEditing()) != null)
                                sendback.send(Commands.SUCCESS, onEditingUser.getUsername());
                            else sendback.send(Commands.SUCCESS, "None");
                            try (InputStream fileStream = section.getFileInputStream()) {
                                Communication.receiveAndSendStream(socketInputStream, socketOutputStream, fileStream);
                            } catch (IOException ex) {
                                sendback.send(Commands.FAILURE, ex.getMessage());
                            }
                        } else sendback.send(Commands.FAILURE, "Section's not found");
                    } else sendback.send(Commands.FAILURE, "You haven't got permissions to modify this file");
                } else sendback.send(Commands.FAILURE, "User's token cannot be found");
            } else sendback.send(Commands.FAILURE, "Document doesn't exist");
        } else sendback.send(Commands.FAILURE, "You're not logged in");
    }

    /**
     * {@code Commands#SHOW_DOCUMENT} handler.
     * <p>
     * Sends (streams) to the {@code Client}, the content concatenation of all the {@code Document}'s
     * {@code Section}s. It informs the {@code Client} about the {@code Section}s that are on editing
     * at the moment.
     *
     * @param args  connection arguments
     * @param sendback  connection response
     */
    private void onShowDocument(Object[] args, Result sendback) {
        if (isSessionAlive()) {
            User user;
            if ((user = onlineUsersDB.getUserByToken(sessionToken)) != null) {
                Document doc;
                String documentName = (String) args[0];
                if ((doc = documentDatabase.getDocumentByName(documentName)) != null) {
                    if (doc.canAccess(user)) {
                        InputStream documentInputStream = null;
                        try {
                            String[] sectionsList = doc.getOnEditingSections();
                            String sectionsListString = sectionsList.length > 0 ? String.join(",", doc.getOnEditingSections()) : "None";
                            sendback.send(Commands.SUCCESS, sectionsListString);
                            documentInputStream = doc.getDocumentInputStream();
                            Communication.receiveAndSendStream(socketInputStream, socketOutputStream, documentInputStream);
                        } catch (IOException ex) {
                            sendback.send(Commands.FAILURE, ex.getMessage());
                            if (documentInputStream != null) {
                                try {
                                    documentInputStream.close();
                                } catch (IOException ex1) {
                                    ex1.printStackTrace();
                                }
                            }
                        }
                    } else sendback.send(Commands.FAILURE, "You haven't got permissions to modify this file");
                } else sendback.send(Commands.FAILURE, "Document doesn't exist");
            } else sendback.send(Commands.FAILURE, "User's token cannot be found");
        } else sendback.send(Commands.FAILURE, "You're not logged in");
    }

    /**
     * {@code Commands#LIST} handler.
     * <p>
     * Informs the {@code Client} about all the {@code Document}s it has access to.
     *
     * @param args  connection arguments
     * @param sendback  connection response
     */
    private void onList(Object[] args, Result sendback) {
        if (isSessionAlive()) {
            User user;
            if ((user = onlineUsersDB.getUserByToken(sessionToken)) != null) {
                String[] documentsNames = documentDatabase.getAllDocumentsNames(user);
                if (documentsNames.length > 0) {
                    String encodedNames = String.join(",", documentsNames);
                    sendback.send(Commands.SUCCESS, encodedNames);
                } else sendback.send(Commands.SUCCESS, "None");
            } else sendback.send(Commands.FAILURE, "User's token cannot be found");
        } else sendback.send(Commands.FAILURE, "You're not logged in");
    }

    /**
     * {@code Commands#SHARE} handler.
     * <p>
     * Gives the target {@code User} the possibility to access the requested {@code Document}.
     * Only the {@code Document}'s owner is allowed to perform this action.
     *
     * @param args  connection arguments
     * @param sendback  connection response
     */
    private void onShare(Object[] args, Result sendback) {
        if (isSessionAlive()) {
            User user;
            if ((user = onlineUsersDB.getUserByToken(sessionToken)) != null) {
                User targetUser;
                if ((targetUser = usersDB.getUserByUsername((String) args[0])) != null) {
                    Document doc;
                    if ((doc = documentDatabase.getDocumentByName((String) args[1])) != null) {
                        if (doc.isCreator(user)) {
                            doc.addModifier(targetUser);
                            targetUser.pushNewNotification(doc.getName());
                            sendback.send(Commands.SUCCESS, "User " + targetUser.getUsername() + " can now access the document " + doc.getName());
                        } else sendback.send(Commands.FAILURE, "You need to be the document's creator to share it");
                    } else sendback.send(Commands.FAILURE, "Target document doesn't exist");
                } else sendback.send(Commands.FAILURE, "Target user doesn't exist");
            } else sendback.send(Commands.FAILURE, "User's token cannot be found");
        } else sendback.send(Commands.FAILURE, "You're not logged in");
    }

    /**
     * Checks if the session is alive or not: if the {@code User} has been already authenticated and a valid
     * {@code String} token exists.
     *
     * @return true if the session is already existing, false otherwise
     */
    private boolean isSessionAlive() {
        return sessionToken != null;
    }
}
