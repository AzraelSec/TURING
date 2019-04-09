package it.azraelsec.Server;

import it.azraelsec.Protocol.RemoteRegistration;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * The {@code RegistrationStub} class is a RMI Stub that allows the remote RMI client to
 * interact with the register method and to insert this way a new {@code User} object into
 * the {@code UsersDB} instance.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class RegistrationStub extends UnicastRemoteObject implements RemoteRegistration {
    private UsersDB usersDB;

    /**
     * Initializes the {@code RegistrationStub}.
     *
     * @param usersDB   {@code UsersDB} reference
     * @throws RemoteException  if RMI error occurs
     */
    RegistrationStub(UsersDB usersDB) throws RemoteException {
        this.usersDB = usersDB;
    }

    /**
     * Offers a remote RMI interface to register new users using their username and password.
     *
     * @param username  user's username
     * @param password  user's password
     * @return  true if the new {@code User} has been registered, false otherwise
     */
    @Override
    public boolean register(String username, String password) {
        return usersDB.addNewUser(username, password) != null;
    }
}
