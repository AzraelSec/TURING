package it.azraelsec.Server;

import it.azraelsec.Chat.CDAManager;
import it.azraelsec.Client.Client;
import it.azraelsec.Documents.Document;
import it.azraelsec.Documents.DocumentsDatabase;
import it.azraelsec.Documents.Section;
import it.azraelsec.Notification.NotificationServerThread;
import it.azraelsec.Protocol.Commands;
import it.azraelsec.Protocol.Communication;
import it.azraelsec.Protocol.Execution;
import it.azraelsec.Protocol.Result;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public TCPRequestHandler(OnlineUsersDB onlineUsersDB, UsersDB usersDB, DocumentsDatabase documentDatabase, CDAManager cdaManager, Socket socket) throws IOException {
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

    @Override
    public void run() {
        do Communication.receive(socketInputStream, socketOutputStream, handlers); while (true);
    }

    private void onLogin(Object[] args, Result sendback) {
        if (!isSessionAlive()) {
            User user;
            if ((user = usersDB.doLogin((String) args[0], (String) args[1])) != null) {
                String token;
                if ((token = onlineUsersDB.login(user)) != null) {
                    notificationThread = new NotificationServerThread(user, socket.getInetAddress().getHostName(), (Integer)args[2]);
                    notificationThread.start();
                    sessionToken = token;
                    System.out.println("New user logged in: " + args[0]);
                    sendback.send(Commands.SUCCESS, token);
                } else sendback.send(Commands.FAILURE, "Login failed: token generation failed");
            } else sendback.send(Commands.FAILURE, "Login failed: authentication error");
        } else sendback.send(Commands.FAILURE, "You're already logged in");
    }

    private void onLogout(Object[] args, Result sendback) {
        sessionToken = null;
        notificationThread.close();
        try {
            notificationThread.join();
        } catch (InterruptedException ignore) {}
        System.out.println("Client's gone out");
        sendback.send(Commands.SUCCESS, "Good-bye");
    }

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

    private void onShowDocument(Object[] args, Result sendback) {
        if(isSessionAlive()) {
            User user;
            if ((user = onlineUsersDB.getUserByToken(sessionToken)) != null) {
                Document doc;
                String documentName = (String) args[0];
                if ((doc = documentDatabase.getDocumentByName(documentName)) != null) {
                    if(doc.canAccess(user)) {
                        InputStream documentInputStream = null;
                        try {
                            String sectionsList = String.join(",", doc.getOnEditingSections());
                            sendback.send(Commands.SUCCESS, sectionsList);
                            documentInputStream = doc.getDocumentInputStream();
                            Communication.receiveAndSendStream(socketInputStream, socketOutputStream, documentInputStream);
                        }  catch (IOException ex) {
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

    private void onList(Object[] args, Result sendback) {
        if(isSessionAlive()) {
            User user;
            if((user = onlineUsersDB.getUserByToken(sessionToken)) != null) {
                String[] documentsNames = documentDatabase.getAllDocumentsNames(user);
                if(documentsNames.length > 0) {
                    String encodedNames = String.join(",", documentsNames);
                    sendback.send(Commands.SUCCESS, encodedNames);
                }
                else sendback.send(Commands.SUCCESS, "None");
            } else sendback.send(Commands.FAILURE, "User's token cannot be found");
        } else sendback.send(Commands.FAILURE, "You're not logged in");
    }

    private void onShare(Object[] args, Result sendback) {
        if(isSessionAlive()) {
            User user;
            if((user = onlineUsersDB.getUserByToken(sessionToken)) != null) {
                User targetUser;
                if((targetUser = usersDB.getUserByUsername((String)args[0])) != null) {
                    Document doc;
                    if((doc = documentDatabase.getDocumentByName((String)args[1])) != null) {
                        if(doc.isCreator(user)) {
                            doc.addModifier(targetUser);
                            targetUser.pushNewNotification(doc.getName());
                            sendback.send(Commands.SUCCESS, "User " + targetUser.getUsername() + " can now access the document " + doc.getName());
                        } else sendback.send(Commands.FAILURE, "You need to be the document's creator to share it");
                    } else sendback.send(Commands.FAILURE, "Target document doesn't exist");
                } else sendback.send(Commands.FAILURE, "Target user doesn't exist");
            } else sendback.send(Commands.FAILURE, "User's token cannot be found");
        } else sendback.send(Commands.FAILURE, "You're not logged in");
    }

    private boolean isSessionAlive() {
        return sessionToken != null;
    }
}
