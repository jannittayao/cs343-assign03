import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInterface extends Remote {
    void sayHello(int peerID) throws RemoteException;
    void getTransfer(double amount) throws RemoteException;
}
