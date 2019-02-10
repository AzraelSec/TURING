package it.azraelsec;

import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RegistrationStub extends UnicastRemoteObject implements RemoteRegistration {
    public static final String NAME = "TURING_REGISTRATION";

    private UsersDB usersDB;
    private OnlineUsersDB onlineUsersDB;

    public RegistrationStub(UsersDB usersDB, OnlineUsersDB onlineUsersDB) throws RemoteException {
        this.usersDB = usersDB;
        this.onlineUsersDB = onlineUsersDB;
    }

    @Override
    public String register(String username, String password, SocketAddress address) {
        User user = usersDB.addNewUser(username, password);
        if(user != null) return onlineUsersDB.login(user, address);
        else return null;
    }
}
