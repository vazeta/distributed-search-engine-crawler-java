package search;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Gateway  extends UnicastRemoteObject implements IClientGateway {
    Gateway() throws RemoteException{
        super();
        gatewayReg();
    }

    private void gatewayReg() {
        try{    

        }catch(RemoteException e){
            System.out.println("Execption");
        }
        
    }


    public static void main(String[] args) {
        try {
            Gateway gateway = new Gateway();
        } catch (RemoteException e) {
            // TODO: handle exception
            System.out.println("Exception occured");
        }
        
    }
    
}
