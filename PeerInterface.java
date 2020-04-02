import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInterface extends Remote {
    void sayHello(int peerID) throws RemoteException;
    void getTransfer(double amount, Peer sendingPeer) throws RemoteException;
    void getMarker(int origin, Peer sendingPeer) throws RemoteException;
    void sendMarker(int origin, Peer sendingPeer) throws RemoteException;
}
