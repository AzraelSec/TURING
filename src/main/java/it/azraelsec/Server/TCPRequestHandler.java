package it.azraelsec.Server;

import it.azraelsec.Documents.Document;
import it.azraelsec.Documents.DocumentsDatabase;
import it.azraelsec.Documents.Section;
import it.azraelsec.Protocol.Commands;
import it.azraelsec.Protocol.Communication;
import it.azraelsec.Protocol.Execution;
import it.azraelsec.Protocol.Result;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TCPRequestHandler implements Runnable {
    OnlineUsersDB onlineUsersDB;
    UsersDB usersDB;
    Socket socket;
    DataInputStream socketInputStream;
    DataOutputStream socketOutputStream;
    DocumentsDatabase documentDatabase;
    String sessionToken;
    Section editingSection;
    Map<Commands, Execution> handlers;

    public TCPRequestHandler(OnlineUsersDB onlineUsersDB, UsersDB usersDB, DocumentsDatabase documentDatabase, Socket socket) throws IOException {
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
        sessionToken = null;
        editingSection = null;
    }

    @Override
    public void run() {
        try {
            do Communication.receive(socketInputStream, socketOutputStream, handlers); while (isSessionAlive());
        } finally {
            try {
                socketInputStream.close();
                socketOutputStream.close();
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void onLogin(Object[] args, Result sendback) {
        if (!isSessionAlive()) {
            User user;
            if ((user = usersDB.doLogin((String) args[1], (String) args[2])) != null) {
                String token;
                if ((token = onlineUsersDB.login(user)) != null) {
                    sessionToken = token;
                    System.out.println("New user logged in: " + args[1]);
                    sendback.send(Commands.SUCCESS, token);
                } else sendback.send(Commands.FAILURE, "Login failed: token generation failed");
            } else sendback.send(Commands.FAILURE, "Login failed: authentication error");
        } else sendback.send(Commands.FAILURE, "You're already logged in");
    }

    private void onLogout(Object[] args, Result sendback) {
        sessionToken = null;
        System.out.println("Client's gone out");
        sendback.send(Commands.SUCCESS, "Good-bye");
    }

    private void onEdit(Object[] args, Result sendback) {
        if (isSessionAlive()) {
            String documeentName = (String) args[0];
            Document doc;
            int sectionNumber = (Integer) args[1];
            if ((doc = documentDatabase.getDocumentByName(documeentName)) != null) {
                User user;
                if ((user = onlineUsersDB.getUserByToken(sessionToken)) != null) {
                    if (doc.canAccess(user)) {
                        if (editingSection == null) {
                            Section section;
                            if ((section = doc.getSection(sectionNumber)) != null) {
                                if (section.tryToSetEditing(user)) {
                                    try {
                                        InputStream fileStream = section.getFileInputStream();
                                        sendback.send(Commands.SUCCESS, "You're editing");
                                        try {
                                            Communication.receiveAndSendStream(socketInputStream, socketOutputStream, fileStream);
                                            editingSection = section;
                                        } catch (IOException ex) {
                                            sendback.send(Commands.FAILURE, ex.getMessage());
                                            fileStream.close();
                                        }
                                    } catch (IOException ex) {
                                        sendback.send(Commands.FAILURE, "Section's reading error: " + ex.getMessage());
                                    }
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
                    editingSection = null;
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
                        if (editingSection == null) {
                            Section section;
                            if ((section = doc.getSection(sectionNumber)) != null) {
                                User onEditingUser;
                                if ((onEditingUser = section.getUserOnEditing()) != null)
                                    sendback.send(Commands.SUCCESS, onEditingUser.getUsername());
                                else sendback.send(Commands.SUCCESS, "None");
                                try (InputStream fileStream = section.getFileInputStream()) {
                                    Communication.receiveAndSendStream(socketInputStream, socketOutputStream, fileStream);
                                    editingSection = section;
                                } catch (IOException ex) {
                                    sendback.send(Commands.FAILURE, ex.getMessage());
                                }
                            } else sendback.send(Commands.FAILURE, "Section's not found");
                        } else sendback.send(Commands.FAILURE, "You can modify one section at time");
                    } else sendback.send(Commands.FAILURE, "You haven't got permissions to modify this file");
                } else sendback.send(Commands.FAILURE, "User's token cannot be found");
            } else sendback.send(Commands.FAILURE, "Document doesn't exist");
        } else sendback.send(Commands.FAILURE, "You're not logged in");
    }

    /**
     * @deprecated DO NOT USE IT!
     * todo: not working...
     */
    private void onShowDocument(Object[] args, Result sendback) {
        if (isSessionAlive()) {
            String documentName = (String) args[0];
            Document doc;
            if ((doc = documentDatabase.getDocumentByName(documentName)) != null) {
                User user;
                if ((user = onlineUsersDB.getUserByToken(sessionToken)) != null) {
                    if (doc.canAccess(user)) {
                        /*todo: MAGIC!!!!*/
                        //Communication.receiveAndSendStream(socketInputStream, socketOutputStream, fileStream);
                    } else sendback.send(Commands.FAILURE, "You haven't got permissions to modify this file");
                } else sendback.send(Commands.FAILURE, "User's token cannot be found");
            } else sendback.send(Commands.FAILURE, "Document doesn't exist");
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

    private boolean isSessionAlive() {
        return sessionToken != null;
    }
}
