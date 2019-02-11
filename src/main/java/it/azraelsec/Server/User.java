package it.azraelsec.Server;

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
    private final ArrayList<Integer> ownDocuments;
    private final ArrayList<Integer> unreadNotifications;

    public User(String username, String password) {
        this.username = username;
        this.password = getHashedString(password);
        ownDocuments = new ArrayList<Integer>();
        unreadNotifications = new ArrayList<Integer>();
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
        String tokenString = password + String.valueOf(System.currentTimeMillis());
        return getHashedString(tokenString);
    }

    public void addOwnDocument(Integer newDoc, boolean isOnline) {
        synchronized(ownDocuments) {
            if(!ownDocuments.contains(newDoc)){
                ownDocuments.add(newDoc);
                if(!isOnline) {
                    synchronized(unreadNotifications) {
                        if(!unreadNotifications.contains(newDoc))
                            unreadNotifications.add(newDoc);
                    }
                }
            }
        }
    }

    public boolean canEdit(Integer doc) {
        synchronized(ownDocuments) {
            return ownDocuments.contains(doc);
        }
    }

    public ArrayList<Integer> getownDocuments() {
        synchronized(ownDocuments) {
            return new ArrayList<Integer>(ownDocuments);
        }
    }

    public ArrayList<Integer> getUnreadNotifications() {
        synchronized(this.unreadNotifications) {
            ArrayList<Integer> unreadNotifications = new ArrayList<Integer>(this.unreadNotifications);
            this.unreadNotifications.clear();
            return unreadNotifications;
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
        catch(NoSuchAlgorithmException ex){}
        return null;
    }
}