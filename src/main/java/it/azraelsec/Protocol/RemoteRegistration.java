package it.azraelsec.Protocol;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The {@code RemoteRegistration} interface represents the remote interface to interact with the remote
 * RMI stub.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public interface RemoteRegistration extends Remote {
    String NAME = "TURING_REGISTRATION";

    /**
     * Execute the remote RMI method for registering a new user and creating its related
     * {@code User} object.
     *
     * @param username  user's username
     * @param password  user's password
     * @return  true if new user has been created, false otherwise
     * @throws RemoteException  if an RMI communication error occurs
     */
    boolean register(String username, String password) throws RemoteException;
}
