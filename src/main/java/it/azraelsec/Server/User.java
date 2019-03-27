package it.azraelsec.Server;

import it.azraelsec.Documents.Document;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * <code>TURING User Class</code>
 * @author Federico Gerardi
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    public final String password;
    private final ArrayList<String> unreadNotifications;

    public User(String username, String password) {
        this.username = username;
        this.password = getHashedString(password);
        unreadNotifications = new ArrayList<String>();
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String password) {
        String hashedPassword = getHashedString(password);
        if(hashedPassword == null) return false;
        return hashedPassword.compareTo(this.password) == 0;
    }

    public String generateToken() {
        String tokenString = password + System.currentTimeMillis();
        return getHashedString(tokenString);
    }

    public ArrayList<String> getUnreadNotifications() {
        synchronized(unreadNotifications) {
            ArrayList<String> unreadNotifications = new ArrayList<>(this.unreadNotifications);
            this.unreadNotifications.clear();
            return unreadNotifications;
        }
    }

    public void pushNewNotification(String doc) {
        synchronized (unreadNotifications) {
            unreadNotifications.add(doc);
        }
    }

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

    @Override
    public boolean equals(Object obj) {
        return ((User) obj).getUsername().compareTo(username) == 0;
    }
}