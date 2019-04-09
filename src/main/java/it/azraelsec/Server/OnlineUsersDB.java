package it.azraelsec.Server;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code OnlineUsersDB} class stores and retrieves information about {@code User}s which are
 * at the moment online.
 * <p>
 * A {@code User} is online if exists a valid {@code String} token. This value is used all around
 * the TURING system to get retrieve information about a target {@code User}.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
class OnlineUsersDB {
    ConcurrentHashMap<String, OnlineUserRecord> onlineUsers;

    /**
     * Initializes the underlining {@code ConcurrentHashMap}.
     */
    OnlineUsersDB() {
        onlineUsers = new ConcurrentHashMap<>();
    }

    /**
     * Logs a {@code User} instance into the {@code OnlineUsersDB} letting it pass to the online
     * state.
     * <p>
     * Only one valid token at a time can exist but it would result senseless to look for a
     * pre-existing {@code User} because the assignment as a key of the {@code ConcurrentHashMap}
     * is a destructive operation anyway.
     *
     * @param user  user reference
     * @return  the session token or null if error occurs
     */
    String login(User user) {
        if (user == null) return null;
        OnlineUserRecord record = new OnlineUserRecord(user);
        // Trying to remove old values would be useless because put method already does it smoothly
        onlineUsers.put(user.getUsername(), record);
        return record.getToken();
    }

    /**
     * Gets a {@code User} object reference from a {@code String} token.
     *
     * @param token session token
     * @return  user reference or null if error occurs
     */
    User getUserByToken(String token) {
        for (OnlineUserRecord record : onlineUsers.values())
            if (record.verifyToken(token)) return record.getUser();
        return null;
    }

    /**
     * The {@code OnlineUserRecord} class represents a {@code OnlineUsersDB} single record and is used to
     * relate a {@code User} object to its {@code String} session token.
     *
     * @author Federico Gerardi
     * @author https://azraelsec.github.io/
     */
    private class OnlineUserRecord {
        private final User user;
        private final String token;

        /**
         * Initializes {@code OnlineUserRecord}.
         *
         * @param user  user reference
         */
        OnlineUserRecord(User user) {
            this.user = user;
            token = user.generateToken();
        }

        /**
         * Gets the {@code User}.
         *
         * @return  user reference
         */
        public User getUser() {
            return user;
        }

        /**
         * Gets the {@code String} session token.
         *
         * @return  session token
         */
        String getToken() {
            return token;
        }

        /**
         * Verifies if the {@code String} session token is this record related or not.
         *
         * @param token session token
         * @return  true if {@code token} is the actual one, false otherwise
         */
        boolean verifyToken(String token) {
            return (this.token.compareTo(token) == 0);
        }
    }
}