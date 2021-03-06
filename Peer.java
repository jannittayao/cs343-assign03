import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.Hashtable;

// This is the main Peer class, which contains all the logic for an independent VM
public class Peer implements PeerInterface {
  // [IMPORTANT] You must add the list of VM IPs before running the code
  static String[] allPeerIPs = new String[]{"52.207.214.164", "3.89.144.2", "52.203.224.105"};
  // Starts with 0, and is mainly used to ensure that the node doesn't send money to itself
  private int peerID;
  // The total amount of money in the peer's account
  private double accountStatement;
  // Boolean that tells the node whether to start recording
  private Boolean recordMessages;
  // Counter that counts how many peers the node has received markers from
  private int receivedMarkers;
  // Boolean array that keeps track of markers sent along channels
  private Boolean[] sentMarkers;
  // Hashtable that stores channels
  private Hashtable<String, LinkedList<Double>> channels;
  // Two separate Hashtables for storing instance and channel states
  private Hashtable<String, Double> instance_state_dict;
  private Hashtable<String, LinkedList<Double>> channel_state_dict;

  public Peer(int theID, double initialAmount) {
    peerID = theID;
    accountStatement = initialAmount;
    recordMessages = false;
    receivedMarkers = 0;
    sentMarkers = new Boolean[allPeerIPs.length];
    // populate sentMarkers with all false
    for (int i = 0; i < allPeerIPs.length; i++){
      sentMarkers[i] = false;
    }
    channels = new Hashtable<String, LinkedList<Double>>();
    instance_state_dict = new Hashtable<String, Double>();
    channel_state_dict = new Hashtable<String, LinkedList<Double>>();
  }

  // You can use this to test out your connections
  public void sayHello(int peerID){
    System.out.println("Peer " + peerID + " is saying hello.");
  }

  // Getter method for instance state
  public Hashtable<String, Double> getInstances(){
    return instance_state_dict;
  }

  // Getter method for channel state
  public Hashtable<String, LinkedList<Double>> getChannels(){
    return channel_state_dict;
  }

  // The remote method used to commit a transfer
  public void getTransfer(double amount, int sendingPeerID){
    try{
      // Check if recording messages and start recording if so
      if (this.recordMessages = true){
        String channelName = (Integer.toString(sendingPeerID) + " -> " +
                              Integer.toString(this.peerID));
        if (this.channel_state_dict.containsKey(channelName)){
          LinkedList<Double> messages = this.channels.get(channelName);
          messages.add(amount);
          this.channels.put(channelName, messages);
        }
        else {
          LinkedList<Double> messages = new LinkedList<Double>();
          messages.add(amount);
          this.channels.put(channelName, messages);
        }
      }

      System.err.println("Peer number " + this.peerID + " received an amount of "+ amount);
      accountStatement += amount;

      Thread.sleep(5000);

      System.err.println("Peer number " + this.peerID + " has a total of "+ accountStatement);
    }
    catch(Exception e){
      System.err.println("Thread exception: " + e.toString());
      e.printStackTrace();
    }
  }

  // The remote method used to receive a marker
  public void getMarker(int origin, int sendingPeerID){
      try{
        ExecutorService pool = Executors.newFixedThreadPool(10);
        pool.execute(new MarkerReceive(origin, sendingPeerID, this));

      } catch(Exception e){
        System.err.println("getMarker exception: " + e.toString());
        e.printStackTrace();
      }
}

  // Method used to send a marker
  public void sendMarker(int origin){
    synchronized(this){
      try{
      String currentPeerID = Integer.toString(this.peerID);
      double currentState = this.accountStatement;
      // 1) Process records its state and then turns on record
      this.instance_state_dict.put(currentPeerID, currentState);
      this.recordMessages = true;

      // 2) For each outgoing channel in which a marker has not been sent,
      //    i sends a marker along c before i sends further messages along c.
      for (int i = 0; i < this.sentMarkers.length; i++){
        if ((this.sentMarkers[i] == false) && (i != this.peerID)){
          // Get stub of destination peer
          String destinationIP = allPeerIPs[i];
          Registry registry = LocateRegistry.getRegistry(destinationIP);
          PeerInterface peerStub = (PeerInterface) registry.lookup("StarterCode");

          // Call receiveMarker method of each receiving peer
          System.err.println("Sending marker to peer " + Integer.toString(i));
          this.sentMarkers[i] = true; //set peer to true
          peerStub.getMarker(origin, this.peerID);


        }
      }
    } catch (Exception e){
      System.err.println("Marker sending exception: " + e.toString());
      e.printStackTrace();
    }
   }
  }

  public void printSnapshot(){
    try{
      System.err.println("------------Snapshot------------");
      System.err.println("Peer accounts: " + this.instance_state_dict.toString());
      System.err.println("Channels: " + this.channel_state_dict.toString());
      System.err.println("--------------------------------");

      // reset everything
      for (int i = 0; i < this.allPeerIPs.length; i++){
          // Get stub of destination peer
          String destinationIP = allPeerIPs[i];
          Registry registry = LocateRegistry.getRegistry(destinationIP);
          PeerInterface peerStub = (PeerInterface) registry.lookup("StarterCode");

          peerStub.snapshotReset();
      }
    } catch (Exception e){
      System.err.println("Print snapshot exception: " + e.toString());
    }
  }

  public void snapshotReset(){
    try{
      recordMessages = false;
      receivedMarkers = 0;
      sentMarkers = new Boolean[allPeerIPs.length];
      // populate sentMarkers with all false
      for (int i = 0; i < allPeerIPs.length; i++){
        sentMarkers[i] = false;
      }
      channels = new Hashtable<String, LinkedList<Double>>();
      instance_state_dict = new Hashtable<String, Double>();
      channel_state_dict = new Hashtable<String, LinkedList<Double>>();
    }
    catch(Exception e) {
      System.err.println("Snapshot reset exception: " + e.toString());
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
          obj.sendMarker(obj.peerID);
        }

        // make a transaction
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

  // The helper class to multi-thread marker receiving process
  private static class MarkerReceive implements Runnable{
    // ID of snapshot requestor
    int originalPeerID;
    // Peer object sending the marker
    int sendingPeerID;
    // Peer object receiving the marker
    Peer receivingPeer;
    // Hashtables storing instance and channel states of receiving peer
    Hashtable<String,Double> receiver_instances;
    Hashtable<String,LinkedList<Double>> receiver_channels;

    MarkerReceive(int origin, int sender, Peer receiver){
      originalPeerID = origin;
      sendingPeerID = sender;
      receivingPeer = receiver;
    }

    @Override
    public void run() {
      try{
        // Update receiving peer hashtable
        Registry registry = LocateRegistry.getRegistry(allPeerIPs[sendingPeerID]);
        PeerInterface peerStub = (PeerInterface) registry.lookup("StarterCode");

        receivingPeer.instance_state_dict = peerStub.getInstances();
        receivingPeer.channel_state_dict = peerStub.getChannels();
        receiver_instances = receivingPeer.instance_state_dict;
        receiver_channels = receivingPeer.channel_state_dict;

        // Add realistic delay
        Thread.currentThread().sleep((int)Math.random()*10000);

        receivingPeer.receivedMarkers++;
        String channelName = (Integer.toString(sendingPeerID) + " -> " +
                              Integer.toString(receivingPeer.peerID));
        // If process has not recorded its state
        if (!receiver_instances.containsKey(Integer.toString(receivingPeer.peerID))){
          // 1) Record the channel state as the empty set
          LinkedList<Double> emptySet = new LinkedList<Double>();
          receiver_channels.put(channelName, emptySet);

          // 2) Follow marker sending rule
          receivingPeer.sendMarker(originalPeerID);
        } // Else if the process has already recorded its state
        else{
          // 1) Record state of channel as state of messages received along c
          if (receivingPeer.channels.containsKey(channelName)){
            LinkedList<Double> messages = receivingPeer.channels.get(channelName);
            receiver_channels.put(channelName, messages);
          } else {
            LinkedList<Double> messages = new LinkedList<Double>();
            receiver_channels.put(channelName, messages);
          }
        }

        // Check to see if Chandy-Lampert terminates
        if ((receivingPeer.receivedMarkers == receivingPeer.allPeerIPs.length - 1)
             && (originalPeerID == receivingPeer.peerID)){
               receivingPeer.printSnapshot();
             }

      } catch (Exception e){
        System.err.println("Marker receiving exception: " + e.toString());
        e.printStackTrace();
      }
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
      // Sending peer ID
      int sendingPeerID;

      TransferTransaction(Peer obj, String toWhom, double amount) {
          this.destinationIP = toWhom;
          this.transferAmount = amount;
          sendingPeer = obj;
          sendingPeerID = obj.peerID;
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
          peerStub.getTransfer(transferAmount, sendingPeerID);
          // Withdraw the amount from the sending account
          sendingPeer.accountStatement -= transferAmount;

        } catch (Exception e) {
            System.err.println("Connection to peer exception: " + e.toString());
            e.printStackTrace();
        }
      }
    }
}
