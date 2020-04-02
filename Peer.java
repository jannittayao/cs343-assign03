import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

// This is the main Peer class, which contains all the logic for an independent VM
public class Peer implements PeerInterface {
  // [IMPORTANT] You must add the list of VM IPs before running the code
  static String[] allPeerIPs = new String[]{"34.201.107.71", "34.226.203.75", "34.201.252.42"};
  // Starts with 0, and is mainly used to ensure that the node doesn't send money to itself
  private int peerID;
  // The total amount of money in the peer's account
  private double accountStatement;
  // Boolean that tells the node whether to start recording
  private Boolean recordMessages;
  // Counter that counts how many peers the node has received markers from
  private int receivedMarkers;
  // Hashtable that stores channels
  private Hashtable<String, Queue<Double>> channels;
  // Two separate Hashtables for storing instance and channel states
  private Hashtable<String, Double> instance_state_dict;
  private Hashtable<String, Queue<Double>> channel_state_dict;

  public Peer(int theID, double initialAmount) {
    peerID = theID;
    accountStatement = initialAmount;
    recordMessages = false;
    receivedMarkers = 0;
    channels = new Hashtable<String, Queue<Double>>();
    instance_state_dict = new Hashtable<String, Double>();
    channel_state_dict = new Hashtable<String, Queue<Double>>();
  }

  // You can use this to test out your connections
  public void sayHello(int peerID){
    System.out.println("Peer " + peerID + " is saying hello.");
  }

  // The remote method used to commit a transfer
  public void getTransfer(double amount){
    try{
      System.err.println("Peer number " + this.peerID + " received an amount of "+ amount);

      accountStatement += amount;
      //Thread.sleep(5000);

      System.err.println("Peer number " + this.peerID + " has a total of "+ accountStatement);
    }
    catch(Exception e){
      System.err.println("Thread exception: " + e.toString());
      e.printStackTrace();
    }
  }

  public static void main(String args[]) {
    try {
      // Create an instance of Peer, to get the node started
      // Note: You must enter the peer ID as a command-line argument
      // Example: java Peer 4
      Peer obj = new Peer(Integer.parseInt(args[0]),500);
      PeerInterface stub = (PeerInterface)UnicastRemoteObject.exportObject(obj, 0);

      // Bind the remote object's stub in the registry
      // I am using rebind here, to make re-running code easier
      Registry registry = LocateRegistry.getRegistry();
      registry.rebind("StarterCode", stub);

      // Ready message
      System.err.println("Peer number "+obj.peerID+" is ready");

      // Now start sending money!

      // The random number generator that we'll be using in the transfers
      Random theRandNumber = new Random();
      // The actual transfers are done based on user commands
      // This is to slow down the execution a bit, to give you enough time to start up all other peers before making a transaction
      Scanner in = new Scanner(System.in);
      System.err.println("Press enter to make a transaction, q to quit, and snap to take a snapshot");
      String theInput = in.nextLine();

      while(!theInput.equals("q")){

        if (theInput.equals("snap")){
          // take a snapshot
        }
        else{
        // Pick a peer at random
        int randIndex = theRandNumber.nextInt(allPeerIPs.length);
        // Make sure that it's not the same node
        if(randIndex == obj.peerID)
          randIndex = (randIndex+1)%allPeerIPs.length;
        String theIP = allPeerIPs[randIndex];

        // Pick an amount at random
        double amount = obj.accountStatement * theRandNumber.nextDouble();

        // Schedule the payment transaction
        ExecutorService pool = Executors.newFixedThreadPool(10);
        pool.execute(new TransferTransaction(obj, theIP, amount));

        }

        theInput = in.nextLine();
      }

      System.exit(0);



    } catch (Exception e) {
        System.err.println("Peer exception: " + e.toString());
        e.printStackTrace();
    }
  }

  // The helper class to multi-thread the transfer process
  private static class TransferTransaction implements Runnable {
      // The IP address of the destination of the transfer
      String destinationIP;
      // The amount of money to transfer
      double transferAmount;
      // The peer object making the transfer
      Peer sendingPeer;

      TransferTransaction(Peer obj, String toWhom, double amount) {
          this.destinationIP = toWhom;
          this.transferAmount = amount;
          sendingPeer = obj;
      }

      @Override
      public void run() {
        try{
          // Get the stub of the destination peer
          Registry registry = LocateRegistry.getRegistry(destinationIP);
          PeerInterface peerStub = (PeerInterface) registry.lookup("StarterCode");


          // To add a but of realistc delay
          // You can remove this
          Thread.currentThread().sleep((int)Math.random()*10000);

          System.err.println("Sending "+transferAmount+" to peer at IP "+ destinationIP);
          // The transfer being committed
          peerStub.getTransfer(transferAmount);
          // Withdraw the amount from the sending account
          sendingPeer.accountStatement -= transferAmount;

        } catch (Exception e) {
            System.err.println("Connection to peer exception: " + e.toString());
            e.printStackTrace();
        }
      }
    }
}
