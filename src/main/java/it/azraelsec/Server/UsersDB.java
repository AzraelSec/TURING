package it.azraelsec.Server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The {@code UsersDB} class stores and manages all the data related to the {@code User}s instances
 * present into the system. It provides methods for user's creation, username's availability checking
 * and session token initializing too.
 * <p>
 * The {@code User}'s username must be a unique identifier, so cannot exist two instances with the
 * same username.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
class UsersDB implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ArrayList<User> users;
    private final ReentrantReadWriteLock mutex;

    /**
     * Initializes the environment.
     */
    UsersDB() {
        users = new ArrayList<>();
        mutex = new ReentrantReadWriteLock();
    }

    /**
     * Logs the user into system through its username and password credentials, retrieving
     * its object reference.
     * @param username user's username
     * @param password  user's password
     * @return  the user object if exists and the credentials are valid, false otherwise
     */
    User doLogin(String username, String password) {
        User user = getUserByUsername(username);
        if(user != null)
            return user.checkPassword(password) ? user : null;
        return null;
    }

    /**
     * Registers a new {@code User} storing its data into the {@code UsersDB} if the input
     * credentials do not exist yet.
     *
     * @param username  user's username
     * @param password  user's password
     * @return  new user reference or null if that username is not available
     */
    User addNewUser(String username, String password) {
        if(!isUsernameAvailable(username)) return null;
        User newUser = new User(username, password);
        mutex.writeLock().lock();
        users.add(newUser);
        mutex.writeLock().unlock();
        return newUser;
    }

    /**
     * Checks if the input username is available or not.
     *
     * @param username  user's username
     * @return  true if does not exist any user with that username, false otherwise
     */
    private boolean isUsernameAvailable(String username) {
        return getUserByUsername(username) == null;
    }

    /**
     * Gets the {@code User} object from its username {@code String}.
     *
     * @param username  user's username
     * @return  related user's object if exists, null otherwise
     */
    User getUserByUsername(String username) {
        mutex.readLock().lock();
        for (User user : users) {
            if(user.getUsername().compareTo(username) == 0) {
                mutex.readLock().unlock();
                return user;
            }
        }
        mutex.readLock().unlock();
        return null;
    }
}