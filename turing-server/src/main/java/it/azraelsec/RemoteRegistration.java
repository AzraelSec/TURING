package it.azraelsec;

import java.net.SocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistration extends Remote {
    public String register(String username, String password, SocketAddress address) throws RemoteException;
}
