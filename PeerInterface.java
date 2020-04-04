import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

public interface PeerInterface extends Remote {
    void sayHello(int peerID) throws RemoteException;
    void getTransfer(double amount, int sendingPeerID) throws RemoteException;
    void getMarker(int origin, int sendingPeerID) throws RemoteException;
    void sendMarker(int origin) throws RemoteException;
    Hashtable<String, Double> getInstances() throws RemoteException;
    Hashtable<String, LinkedList<Double>> getChannels() throws RemoteException;
}
