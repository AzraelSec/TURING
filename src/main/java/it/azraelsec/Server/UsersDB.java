package it.azraelsec.Server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UsersDB implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ArrayList<User> users;
    private final ReentrantReadWriteLock mutex;

    public UsersDB() {
        users = new ArrayList<User>();
        mutex = new ReentrantReadWriteLock();
    }

    public User doLogin(String username, String password) {
        User user = getUserByUsername(username);
        if(user != null)
            return user.checkPassword(password) ? user : null;
        return null;
    }

    public User addNewUser(String username, String password) {
        if(!isUsernameAvailable(username)) return null;
        User newUser = new User(username, password);
        mutex.writeLock().lock();
        users.add(newUser);
        mutex.writeLock().unlock();
        return newUser;
    }

    private boolean isUsernameAvailable(String username) {
        return getUserByUsername(username) == null;
    }

    public User getUserByUsername(String username) {
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