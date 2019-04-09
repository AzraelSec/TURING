package it.azraelsec.Client;

import java.util.ArrayList;

/**
 * The {@code LocalSession} class collects all the session's values related to the {@code User}'s session.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
class LocalSession {
    private final String sessionToken;
    private final String username;
    private String onEditingFilename;
    private final ArrayList<String> notificationQueue;

    /**
     * Initializes the {@code LocalSession} using the {@code sessionToken} and the respective
     * {@code User}'s {@code username}.
     * <p>
     * A new notifications' {@code ArrayList} is instantiated.
     *
     * @param sessionToken  user session token
     * @param username  user's username
     */
    LocalSession(String sessionToken, String username) {
        this.sessionToken = sessionToken;
        this.username = username;
        notificationQueue = new ArrayList<>();
        onEditingFilename = null;
    }

    /**
     * Sets the {@code LocalSession}'s status on editing, storing the {@code Document}'s filename is being edited.
     *
     * @param onEditingFilename on editing document's filename
     */
    void setOnEdit(String onEditingFilename) {
        this.onEditingFilename = onEditingFilename;
    }

    /**
     * Gets the on editing document's filename.
     *
     * @return  the actual on editing document's filename
     */
    String getOnEditing() {
        return onEditingFilename;
    }

    /**
     * Gets the actual session token.
     *
     * @deprecated
     */
    String getToken() {
        return sessionToken;
    }

    /**
     * Gets the session user's username.
     *
     * @return  actual user's username
     */
    String getUsername() {
        return username;
    }

    /**
     * Verifies if the actual {@code LocalSession} is on editing or not.
     *
     * @return true if the actual user is editing any document, false otherwise
     * */
    boolean isEditing() {
        return onEditingFilename != null;
    }
}
