package helpers;

/**
 * Class: ICS 460 - Computer Networks and Security <br>
 * Instructor: Damodar Chetty <br>
 * Description: Program 2, Implementing "Sliding Window" functionality above the 
 * Transport layer to make UDP more reliable. <br>
 * Due: 08/03/2016 <br><br>
 * 
 * This particular class is a sender thread used by the UDP client. Its job is 
 * to create, send and buffer new DatagramPackets as the window space in the window 
 * opens up. 
 * 
 * @author Tom Carney
 * @version 1.0
 * @since 07/14/2016
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import application.P2Client;


public class SenderThread extends Thread {
    
    private P2Client client;
    private DatagramSocket socket;
    private InetAddress IPAddress;
    private FileInputStream fStream;
    
    
    /**
     * A constructor for the SenderThread.
     */
    public SenderThread(P2Client newClient, DatagramSocket newSocket, InetAddress newAddress, 
                            FileInputStream newStream) {
        
        client = newClient;
        socket = newSocket;
        IPAddress = newAddress;
        fStream = newStream;
        
    } // end SenderThread constructor
    
    
    /**
     * This is the executable portion of the thread. It schedules, creates and sends  
     * DatagramPackets via the clients sliding window setup.
     */
    @Override
    public void run() {
        
        // Actual number of bytes of data.
        int dataLength;
        
        // Buffer which will hold header and data to be sent. 
        byte[] applicationBuffer;
            
        try {
                
            // Will be false once the file has been totally read.
            while(fStream.available() != 0) {
                    
                // Only create/send a new packet if there is room in the window.
                if(client.isRoomInWindow()) {
                    
                    // Packets are buffered in a sliding window so each needs a separate buffer.
                    applicationBuffer = new byte[client.getMaxDataSize() + Helper.APPLICATIONHEADERSIZE];
                    
                    // Actual application payload size (header and data) may not totally fill the buffer.
                    dataLength = fillApplicationBuffer(applicationBuffer);
                    
                    sendData(applicationBuffer, dataLength);
                    
                    simulationDelay(Helper.SENDNEWPACKETDELAY, "Couldn't delay sending new packet!");
                      
                } else {
                    
                    //System.out.println("\nWindow full ( " + printWindowContents() + ")");
                    
                    simulationDelay(Helper.WINDOWFULLSLEEPMODIFIER, "Window full but couldn't put to sleep!");
                    
                } // end if-else block
                    
                Thread.yield();
                    
            } // end while loop
            
            // Mark the file as being fully read.
            client.setDoneReading(true);
                
        } catch (IOException e) {
                
            System.out.println("Error reading file!");
                
        } // end outer try-catch
            
    } // end run 

    
    /**
     * Use this method to slow down the simulation so people can keep up with the console
     * output. The thread will go to sleep for .1 seconds times the modifier.
     * 
     * @param delayModifier - A multiple for the default sleep time. 
     * 
     * @param message - A message to print in case the thread couldn't sleep.
     */
    private void simulationDelay(int delayModifier, String message) {
        
        try {
            
            sleep(100 * delayModifier);
            
        } catch (InterruptedException e) {
            
            System.out.println(message);
        
        }
        
    } // end simulationDelay
    
    
    /**
     * This method will return a String containing the sequence numbers in the window.
     * 
     * @return - A string with the sequence numbers currently windowed.
     */
    private String printWindowContents() {
        
        String windowContents = "";
        int start = client.getLAR() + 1;
        int end = client.getLFS();
        
        for( ; start <= end ; start++) {
            
            windowContents += start + " ";
            
        }
        
        return windowContents;
        
    } // end printWindowContents
    
    
    /**
     * This method is used to fill the application buffer with the header and data 
     * information to be sent.
     * 
     * @param buffer - The buffer to fill with header and data information.
     */
    private int fillApplicationBuffer(byte[] buffer) {
        
        // Amount of data available may be less than available space in the buffer.
        int dataLength = bufferData(buffer);
        
        Helper.bufferLength(buffer, dataLength);
        Helper.bufferAckNumber(buffer, client.getLAR() + 1);
        Helper.bufferSeqNumber(buffer, client.getLFS() + 1);
        
        // Decide if simulating a check sum error.
        if(Helper.shouldCheckSumError(client.getErrorPercent())) {
            
            Helper.bufferCheckSum(buffer, Helper.CHECKSUMBAD);
            System.out.println("\n*** Simulating bad check sum on sequence number " +
                                    (client.getLFS() + 1) + " ***");
            
        } else {
            
            Helper.bufferCheckSum(buffer, Helper.CHECKSUMGOOD);
            
        }
        
        return dataLength;
        
    } // end fillApplicationBuffer
    
    
    /**
     * This method will read a number of byte from the file being transfered and place them
     * in the data portion of the byte array. The number of bytes specified by the user as the
     * max data size or default data size may exceed available data. 
     * 
     * An integer is returned stating how many bytes were read which may be less than the total
     * available space. 
     * 
     * @param buffer - The buffer being used by the DatagramPacket.
     * 
     * @return - An integer stating how many bytes were read into the array.
     */
    private int bufferData(byte[] buffer) {
        
        int numBytesRead = 0;
        
        try {
            
            // Read at most the max data size and place them into the buffer at the specified
            // offset (just past the header), returning the actual number of bytes read.
            numBytesRead = fStream.read(buffer, Helper.APPLICATIONHEADERSIZE, 
                                            client.getMaxDataSize());
        
        } catch (IOException e) {
            
            System.out.println("Error filling buffer from file!");
            
        } // end try-catch block
        
        return numBytesRead;
        
    } // end bufferData
    
    
    /**
     * This  method is used to send the application packet to the server.
     * 
     * @param buffer - The byte array to be sent.
     * 
     * @param dataLength - The amount of actual data, non-header bytes.
     */
    private void sendData(byte[] buffer, int dataLength) {
        
        // The full packet size.
        int applicationPacketSize = Helper.APPLICATIONHEADERSIZE + dataLength;
        
        System.out.println("\nSending " + applicationPacketSize + 
                                " bytes as seqNum " + (client.getLFS() + 1) + ".");
        
        DatagramPacket sendPacket = 
                new DatagramPacket(buffer, applicationPacketSize, IPAddress, Helper.PORT);
        
        try {
            
            socket.send(sendPacket);
            client.incLFS();
            
            // store the DatagramPacket in the window until it is acknowledged.
            int index = client.getLFS() % client.getWindowSize();
            System.out.println("Storing seqNum " + client.getLFS() + " in window slot " + index + ".");
            client.putInWindow(sendPacket, index);
        
        } catch (IOException e) {
            
            System.out.println("Error sending data!");
        
        } // end try-catch block
        
    } // end sendData
        
} // end SenderThread