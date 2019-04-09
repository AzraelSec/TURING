package it.azraelsec.Server;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * The {@code User} class represents a user into the TURING system and has got all the methods
 * to retrieve all its notifications and to verify its credentials.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String password;
    private final ArrayList<String> unreadNotifications;

    /**
     * Initializes a {@code User} object.
     *
     * @param username
     * @param password
     */
    public User(String username, String password) {
        this.username = username;
        this.password = getHashedString(password);
        unreadNotifications = new ArrayList<>();
    }

    /**
     * Gets the user's username.
     *
     * @return user's username
     */
    String getUsername() {
        return username;
    }

    /**
     * Verifies that the correct password is given in input.
     *
     * @param password  user's password
     * @return  true if {@code password} is the right one, false otherwise
     */
    boolean checkPassword(String password) {
        String hashedPassword = getHashedString(password);
        if(hashedPassword == null) return false;
        return hashedPassword.compareTo(this.password) == 0;
    }

    /**
     * Generates a valid token starting from user's password and actual timestamp (used
     * like a salt value).
     *
     * @return  a valid {@code String} session token
     */
    String generateToken() {
        String tokenString = password + System.currentTimeMillis();
        return getHashedString(tokenString);
    }

    /**
     * Gets all the unread notifications to this user associated.
     *
     * @return  notifications strings array
     */
    public ArrayList<String> getUnreadNotifications() {
        synchronized(unreadNotifications) {
            ArrayList<String> unreadNotifications = new ArrayList<>(this.unreadNotifications);
            this.unreadNotifications.clear();
            return unreadNotifications;
        }
    }

    /**
     * Add a new notification value to the unread ones.
     *
     * @param doc   new document which user has access to
     */
    void pushNewNotification(String doc) {
        synchronized (unreadNotifications) {
            unreadNotifications.add(doc);
        }
    }

    /**
     * Hashes the input {@code String} generating a new one encrypted in SHA-256 format.
     *
     * @param toHash    string to hash
     * @return  the SHA-256 string hash
     */
    private String getHashedString(String toHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes =  md.digest(toHash.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch(NoSuchAlgorithmException ignore){}
        return null;
    }

    /**
     * Checks if a {@code User} instance is equal to another one given as input.
     *
     * @param obj   object to compare
     * @return  true if {@code obj} is a {@code User} instance and is equal to this one, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != User.class) return false;
        return ((User) obj).getUsername().compareTo(username) == 0;
    }
}