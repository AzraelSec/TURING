package it.azraelsec.Client;

import java.util.ArrayList;

public class LocalSession {
    private final String sessionToken;
    private String onEditingFilename;
    private final ArrayList<String> notificationQueue;

    public LocalSession(String sessionToken) {
        this.sessionToken = sessionToken;
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

    public boolean isEditing() {
        return onEditingFilename != null;
    }
}
