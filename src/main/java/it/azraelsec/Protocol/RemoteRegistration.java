package it.azraelsec.Protocol;

import java.net.SocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistration extends Remote {
    static final String NAME = "TURING_REGISTRATION";
    public boolean register(String username, String password) throws RemoteException;

}
