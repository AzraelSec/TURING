package it.azraelsec.Server;

import it.azraelsec.Protocol.RemoteRegistration;

import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RegistrationStub extends UnicastRemoteObject implements RemoteRegistration {
    private UsersDB usersDB;
    private OnlineUsersDB onlineUsersDB;

    public RegistrationStub(UsersDB usersDB, OnlineUsersDB onlineUsersDB) throws RemoteException {
        this.usersDB = usersDB;
        this.onlineUsersDB = onlineUsersDB;
    }

    @Override
    public boolean register(String username, String password) {
        return usersDB.addNewUser(username, password) != null;
    }
}
