package it.azraelsec.Client;

import java.util.ArrayList;

public class LocalSession {
    private final String sessionToken;
    private final String username;
    private String onEditingFilename;
    private final ArrayList<String> notificationQueue;

    public LocalSession(String sessionToken, String username) {
        this.sessionToken = sessionToken;
        this.username = username;
        notificationQueue = new ArrayList<>();
        onEditingFilename = null;
    }

    public void setOnEdit(String onEditingFilename) {
        this.onEditingFilename = onEditingFilename;
    }

    public String getOnEditing() {
        return onEditingFilename;
    }

    public String getToken() {
        return sessionToken;
    }

    public String getUsername() {
        return username;
    }

    public boolean isEditing() {
        return onEditingFilename != null;
    }
}
