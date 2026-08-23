package search;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


class ReliableMulticastClientImpl extends UnicastRemoteObject implements ReliableMulticastClient {
    protected ReliableMulticastClientImpl() throws RemoteException {
        super();
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {
        System.out.println("Mensagem recebida via ReliableMulticast: " + message);
    }

    
}