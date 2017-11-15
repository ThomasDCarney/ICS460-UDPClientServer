package application;

/**
 * Class: ICS 460 - Computer Networks and Security <br>
 * Instructor: Damodar Chetty <br>
 * Description: Program 2, Implementing "Sliding Window" functionality above the 
 * Transport layer to make UDP more reliable. <br>
 * Due: 08/03/2016 <br><br>
 * 
 * This particular class is the server of our UDP Sliding Window simulation. It is 
 * used to receive a file from client to server, demonstrating how the overall
 * application handles damaged, delayed or lost packets.
 * 
 * The server is go-back-n so functionally has a buffer of only 1. Becides sending
 * acknowledgement for good/received packets, it checks for Check Sum and out of 
 * sequence errors while simulating lost packets as well.
 * 
 * @author Tom Carney
 * @version 1.0
 * @since 07/14/2016
 */

import helpers.Helper;



// For file I/O
import java.io.FileOutputStream;

// For UDP
import java.net.DatagramSocket;
import java.net.DatagramPacket;

// Throws
import java.io.IOException;
import java.net.SocketException;


public class P2Server {

    private DatagramSocket serverSocket;
    private DatagramPacket receivedPacket;
    private byte[] receivedBuffer;
    private int nextSeqNum;
    
    // Store user specified parameters.
    private int errorPercent;
    private FileOutputStream fStream;
    
    
    /**
     * A no argument constructor for the P2Server.
    */
    public P2Server() {
        
        inputSimulationParameters();
        
        nextSeqNum = 1;
        receivedBuffer  = new byte[Helper.DEFAULTBUFFERSIZE];
        
    } // end P2Server no-arg constructor
    
    
    /**
     * This method will gather specific parameters from the user used during the simulation.
     */
    private void inputSimulationParameters() {

        fStream = Helper.createFileOutputStream("Enter location to output the file (eg .\\newFile.txt): ");
        errorPercent = Helper.inputInteger("Enter an error percentage: ");
        
    } // end getSimulationParameters
    
    
    /**
     * Once user parameters have been entered, this method is used to get the server side of the 
     * simulation up and running.
     */
    private void startSimulation() {
        
        System.out.println("\nStarting simulation, waiting for client...\n");
        
        try {
            
            serverSocket = new DatagramSocket(Helper.PORT);
            
            boolean loop = true;
            
            while(loop) {

                receivedPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
                
                try {
                    
                    // Block until a packet comes in.
                    serverSocket.receive(receivedPacket);
                    
                    // Print the packets details for reference before possible error simulations.
                    printPacketDetails(receivedPacket);
                    
                    // FIRST!!! simulate packet loss by pretending it never arrived.
                    if(Helper.isPacketReceived(getErrorPercent())) { // START OUTER IF-ELSE
                        
                        // SECOND!!! If Check Sum is bad then discard and wait for next packet.
                        if(Helper.isCheckSumGood(receivedBuffer)) { // START MIDDLE IF-ELSE
                            
                            // THIRD!!! Go-Back-N only processes the expected (next) sequence number.
                            // Acknowledgements may also have gotten lost so reacknowledge dupes.
                            if(isNextInSequence()) { // START INNER IF-ELSE
                                
                                // If we made it here, all is good so write to file!
                                writeToFile();
                                
                                // Send acknowledgement that the packet/sequence number was processed.
                                sendAcknowledgement();
                                nextSeqNum++;
                                
                            } else if(isAlreadyReceived()){
                                
                                // Just resending an acknowledgement.
                                resendAcknowledgement();
                            
                            } else {
                                
                                System.out.println("*** Unexpected packet received, need sequence number " + 
                                        nextSeqNum + "! ***\n");
                                
                            } // END INNER IF-ELSE
                            
                        } else {
                            
                            System.out.println("*** Check Sum bad, discarding packet! ***\n");
                            
                        } // END MIDDLE IF-ELSE
                        
                    } else {
                        
                        System.out.println("*** Simulating loss of sequence number " + 
                                           Helper.retrieveSeqNum(receivedPacket.getData()) + "! ***\n");
                        
                    } // END OUTER IF-ELSE
                
                } catch (IOException e) {
                    
                    System.out.println("Socket error, could not receive packet!");
                    
                } // end inner try-catch block

            } // end while loop
            
            System.out.println("Server shutting down!");

        } catch (SocketException e) {
            
            System.out.println("Socket Exception: Socket could not be opened!");
        
        } // end outer try-catch block
        
    } // end startSimulation
    
    
    /**
     * This method will send an acknowledgement for the packet received.
     */
    private void sendAcknowledgement() {
        
        byte[] responseBuffer = new byte[Helper.ACKNOWLEDGEMENTHEADERSIZE];
        fillResponseBuffer(responseBuffer);
        
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, 
                receivedPacket.getAddress(), receivedPacket.getPort());
        
        try {
            
            serverSocket.send(responsePacket);
            System.out.println("!!! Sending acknowledgement for sequence number " + nextSeqNum + ".\n");
        
        } catch (IOException e) {
            
            System.out.println("Unable to send acknowledgement for sequence number " + nextSeqNum + ".\n");
        
        }
        
    } // end sendAcknowledgement
    
    
    /**
     * This method will resend an acknowledgement for the packet packet previously received.
     */
    private void resendAcknowledgement() {
        
        byte[] responseBuffer = new byte[Helper.ACKNOWLEDGEMENTHEADERSIZE];
        fillResponseBuffer(responseBuffer);
        
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, 
                receivedPacket.getAddress(), receivedPacket.getPort());
        
        try {
            
            serverSocket.send(responsePacket);
            System.out.println("!!! Resending acknowledgement for sequence number " + 
                                    Helper.retrieveSeqNum(receivedBuffer) + ".\n");
        
        } catch (IOException e) {
            
            System.out.println("*** Unable to resend acknowledgement for sequence number " + 
                                    Helper.retrieveSeqNum(receivedBuffer) + " ***\n");
        
        }
        
    } // end resendAcknowledgement
    
    
    /**
     * This method is used to populate the data needed in a packets reply.
     * 
     * @param buffer - The buffer where the information is stored.
     */
    private void fillResponseBuffer(byte[] buffer) {
        
        if(Helper.shouldCheckSumError(getErrorPercent())) {
            
            Helper.bufferCheckSum(buffer, Helper.CHECKSUMBAD);
            System.out.println("*** Simulating bad check sum acknowleding seq num " + 
                                   Helper.retrieveSeqNum(receivedBuffer)+ " ***\n");
            
        } else {
            
            Helper.bufferCheckSum(buffer, Helper.CHECKSUMGOOD);
            
        }
        
        Helper.bufferLength(buffer, 0);
        Helper.bufferAckNumber(buffer, Helper.retrieveSeqNum(receivedBuffer));
        
    } // end fillResponseBuffer
    
    
    /**
     * This method will provide the error percent specified by the user. If an error occurred 
     * while entering the value, a default percent is provided.
     * 
     * @return - The error percent, in whole numbers (%'s).
     */
    public int getErrorPercent() {
        
        // -1 was saved if an error occurred.
        if(errorPercent < 0) {
            
            return Helper.DEFAULTERRORPERCENT;
            
        } else {
        
            return errorPercent;
            
        }
        
    } // end getErrorPercent
    
    
    /**
     * This method is used to determine whether a packet is next in the sequence.
     * 
     * @return - True if it is the expected (next) sequence number, false otherwise.
     */
    private boolean isNextInSequence() {
        
        return Helper.retrieveSeqNum(receivedBuffer) == nextSeqNum;
        
    } // end isNextInSequence
    
    
    /**
     * This method is used to determine whether a packet was already received. If so, the
     * acknowledgement may have been lost so send another.
     * 
     * @return - True if it is the sequence number was already received, false otherwise.
     */
    private boolean isAlreadyReceived() {
        
        return Helper.retrieveSeqNum(receivedBuffer) < nextSeqNum;
        
    } // end alreadyReceived
    
    
    /**
     * This method is used to write the received data to the specified file location. It will also determine
     * whether more data is on the way or if the server can stop.
     * 
     * @return - A boolean true if more data is coming, false otherwise.
     */
    private void writeToFile() {
        
        short dataLength = (short)(Helper.retrieveLength(receivedBuffer) - Helper.APPLICATIONHEADERSIZE);
        
        try {
            
            fStream.write(receivedBuffer, Helper.APPLICATIONHEADERSIZE, dataLength);
        
        } catch (IOException e) {
            
            System.out.println("Could not write data to file!");
            
        }
        
    } // end writeToFile
    
    
    /**
     * This method will print the details of a received packet.
     * 
     * @param packet - The packet to analyze.
     */
    private void printPacketDetails(DatagramPacket packet) {
        
        byte[] payload = packet.getData();
        
        System.out.println("From: " + packet.getAddress() + " Port:" + packet.getPort());
        System.out.println("CheckSum: " + Helper.retrieveCheckSum(payload) + 
                           ", Length: " + Helper.retrieveLength(payload) + 
                           ", AckNum: " + Helper.retrieveAckNum(payload) + 
                           ", SeqNum: " + Helper.retrieveSeqNum(payload) + "\n");
        
    } // end printPacketDetails
    
    
    public static void main(String[] args) {
        
        P2Server server = new P2Server();
        
        server.startSimulation();
        
    } // end main
    
} // end P2Server