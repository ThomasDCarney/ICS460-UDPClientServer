package helpers;

/**
 * Class: ICS 460 - Computer Networks and Security <br>
 * Instructor: Damodar Chetty <br>
 * Description: Program 2, Implementing "Sliding Window" functionality above the 
 * Transport layer to make UDP more reliable. <br>
 * Due: 08/03/2016 <br><br>
 * 
 * This particular class is a receiver thread used by the UDP client. Its job is 
 * to wait for acknowledgement packets to arrive and if they don't, handle 
 * resending when necessary. 
 * 
 * Also, due to the simulation, it will correct check sum errors if the Datagram
 * originally gave it the "bad" value.
 * 
 * @author Tom Carney
 * @version 1.0
 * @since 07/14/2016
 */

import application.P2Client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
// Throws
import java.io.IOException;
import java.net.SocketTimeoutException;


public class ReceiverThread extends Thread {
    
    private P2Client client;
    private DatagramSocket socket;
    private DatagramPacket responsePacket;
    private byte[] responseBuffer;
    
    
    /**
     * A constructor for the ReceiverThread.
     */
    public ReceiverThread (P2Client newClient, DatagramSocket newSocket) {
        
        client = newClient;
        socket = newSocket;
        
    } // end ReceiverThread constructor
    
    /**
     * This is the executable portion of the thread. It handles responses from the client by modifying
     * the window when acknowledgements are received. It also manages re-sending packets when they are
     * not. Reasons vary depending on simulation parameters but are all handled essentially the same
     * via the sockets "time out" period.
     */
    @Override
    public void run() {
        
        responseBuffer = new byte[Helper.DEFAULTBUFFERSIZE];
        
        boolean loop = true;
        
        while(loop) {
            
            responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            
            try {
                
                // Will block until a response comes in.
                socket.receive(responsePacket);
                
                // FIRST!!! simulate packet loss by pretending it never arrived.
                if(Helper.isPacketReceived(client.getErrorPercent())) {
                    
                    // SECOND!!! If Check Sum is bad then discard and wait for next packet.
                    if(Helper.isCheckSumGood(responseBuffer)) {
                        
                        // THIRD!!! Only acknowledge the next sequence number expected.
                        if(isNextInSequence()) {
                            
                            printAcknowledgement();
                            client.incLAR();
                            
                        // THIRD.2!!! If cumulative updates of ack's are allowed.    
                        } else if(isCumulativeNext()){
                            
                            printCumulativeAck();
                            client.setLAR(Helper.retrieveAckNum(responseBuffer));
                            
                            
                        } else {    
                            
                            System.out.println("\n*** Unexpected ack received for seq num " + 
                                                   Helper.retrieveAckNum(responseBuffer) + 
                                                   ", waiting for ack on " + (client.getLAR() + 1) + 
                                                   "! ***");
                            
                        } // END INNER IF-ELSE
                        
                    } else {
                        
                        System.out.println("\n*** Check Sum bad, discarding ack for seq num " +
                                               Helper.retrieveAckNum(responseBuffer) + "! ***");
                        
                    } // END MIDDLE IF-ELSE
                    
                } else {
                    
                    System.out.println("\n*** Received but simulating lost ack for seq num " + 
                                           Helper.retrieveAckNum(responseBuffer) + "! ***");
                    
                } // END OUTER IF-ELSE
                
            } catch(SocketTimeoutException e) {
            
                // Meaning packets were sent but not yet acknowledged. In general, the socket times out
                // at a specified interval but action is only taken if this exception is handled.
                loop = processTimeOut();
            
            } catch (IOException e) {
                
                System.out.println("Error receiving packet!");
                
            } // end try-catch block
            
            Thread.yield();
            
        } // end while loop
        
    } // end run
    
    
    /**
     * This method will handle what happens when a timeout event occurs.
     */
    private boolean processTimeOut() {
        
        boolean moreData = true;
        
        if(client.getLAR() < client.getLFS()) {
            
            System.out.println("\nTimeout occured, waiting for ack on seq number " + 
                                (client.getLAR() + 1));
            resendWindow();
            
        } else if(client.getDoneReading()){
            
            System.out.println("\nDone sending file!");
            moreData = false;
            
        } else {
            
            System.out.println("\nTimeout but nothing waiting to send!");
            
        } // end if-else block
        
        return moreData;
        
    } // end processTimeOut
    
    
    /**
     * This method is used to re-send all unacknowledged segments in the window.
     */
    private void resendWindow() {
        
        // Calculate the number of unacknowledged sequence numbers.
        int numUnacknowledged = client.getLFS() - client.getLAR();
        
        // Find the lowest unacknowledged sequence number to send.
        int notAcknowledged = client.getLAR() + 1;
        
        System.out.println("Will attempt to resend " + numUnacknowledged + " sequence numbers!\n");
        
        for(int i = 0 ; i < numUnacknowledged ; i++) {
            
            DatagramPacket tempPacket = client.getPacketFromWindow(notAcknowledged % 7);
            byte[] tempBuffer = tempPacket.getData();
            
            // "Recalculate" the checksum (for simulation, 2nd time around just be good).
            Helper.bufferCheckSum(tempBuffer, Helper.CHECKSUMGOOD);
            
            // Update the ack waited on, may be different than when originally sent.
            Helper.bufferAckNumber(tempBuffer, client.getLAR() + 1);
            
            try {
                
                System.out.println("Attempting to resend seqNum " + notAcknowledged);
                socket.send(tempPacket);
            
            } catch (IOException e) {
                
                System.out.println("Error resending seqNum " + notAcknowledged + "!");
            
            }
            
            notAcknowledged++;
            
        }
        
    } // end resendWindow
    
    
    /**
     * This method is used to resend the oldest DatagramPacket that has not yet been acknowledged.
     */
    private void resendPacket() {
        
        int notAcknowledged = client.getLAR() + 1;
        
        System.out.println("Attempting to resend packet " + notAcknowledged);
        
        DatagramPacket tempPacket = client.getPacketFromWindow(notAcknowledged % 7);
        byte[] tempBuffer = tempPacket.getData();
        
        // "Recalculate" the checksum (for simulation, 2nd time around be good).
        Helper.bufferCheckSum(tempBuffer, Helper.CHECKSUMGOOD);
        
        // Update the acknowledgment being waited on (itself).
        Helper.bufferAckNumber(tempBuffer, notAcknowledged);
        
        try {
            
            socket.send(tempPacket);
        
        } catch (IOException e) {
            
            System.out.println("Error resending packet " + notAcknowledged + "!");
        
        }
        
    } // end resendPacket
    
    
    /**
     * This method will output the details of the acknowledgement packet to the console.
     */
    private void printAcknowledgement() {
        
        System.out.println("\n!!! Acknowledgement received for sequence number " + 
                                Helper.retrieveAckNum(responseBuffer));
        
    } // end printReply
    
    
    /**
     * This method will output the details of the acknowledgement packet to the console with
     * notice that LAR will be updated cumulatively.
     */
    private void printCumulativeAck() {
        
        System.out.println("\n!!! CUMULATIVE UPDATE... Acknowledgement received for seqNum " + 
                                Helper.retrieveAckNum(responseBuffer));
        
    } // end printCumulativeAck
    
    
    /**
     * This method is used to determine whether a packet is next in the sequence.
     * 
     * @return - True if it is the expected (next) sequence number, false otherwise.
     */
    private boolean isNextInSequence() {
        
        return Helper.retrieveAckNum(responseBuffer) == (client.getLAR() + 1);
        
    } // end isNextInSequence
    
    
    /**
     * This method is used to determine if the LAR can be updated based on whether cumulative
     * acknowledgments are allowed. 
     * 
     * eg... If the server acknowledges sequence number 3 but client is waiting on an ack for 
     * sequence number 2. The client can assume sequence number 2 was received and ack was 
     * simply lost or damaged in transit, updating LAR to 3.
     * 
     * @return A boolean true if cumulative update can be made, false otherwise.
     */
    private boolean isCumulativeNext() {
        
        return Helper.CUMULATIVE && Helper.retrieveAckNum(responseBuffer) > client.getLAR();
        
    } // end isCumulativeNext
    
    
} // end ReceiverThread