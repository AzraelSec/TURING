package it.azraelsec.Server;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineUsersDB {
    // username, structure
    ConcurrentHashMap<String, OnlineUserRecord> onlineUsers;

    public OnlineUsersDB() {
        onlineUsers = new ConcurrentHashMap<String,OnlineUserRecord>();
    }

    public String login(User user) {
        if(user == null) return null;
        OnlineUserRecord record = new OnlineUserRecord(user);
        // Trying to remove old values would be useless because put method already does it smoothly
        onlineUsers.put(user.getUsername(), record);
        return record.getToken();
    }
    
    public void logout(User user) {
        if(user != null) onlineUsers.remove(user.getUsername());
    }

    public boolean verifyToken(String username, String token) {
        OnlineUserRecord record = onlineUsers.get(username);
        if(record == null) return false;
        return record.getToken().compareTo(token) == 0;
    }

    public User getUser(String username) {
        OnlineUserRecord record = onlineUsers.get(username);
        if(record == null) return null;
        else return record.getUser();
    }

    public User getUserByToken(String token) {
        for(OnlineUserRecord record : onlineUsers.values())
            if(record.verifyToken(token)) return record.getUser();
        return null;
    }

    private class OnlineUserRecord {
        private final User user;
        private final String token;

        public OnlineUserRecord(User user) {
            this.user = user;
            token = user.generateToken();
        }

        public User getUser() {
            return user;
        }

        public String getToken() {
            return token;
        }

        public boolean verifyToken(String token) {
            return (this.token.compareTo(token) == 0);
        }
    }
}