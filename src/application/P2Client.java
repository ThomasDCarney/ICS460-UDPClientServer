package application;

/**
 * Class: ICS 460 - Computer Networks and Security <br>
 * Instructor: Damodar Chetty <br>
 * Description: Program 2, Implementing "Sliding Window" functionality above the 
 * Transport layer to make UDP more reliable. <br>
 * Due: 08/03/2016 <br><br>
 * 
 * This particular class is the client of our UDP Sliding Window simulation. It is 
 * used to transmit a file from client to server, demonstrating how the overall
 * application handles damaged, delayed or lost packets.
 * 
 * The client itself maintains a Sliding Window, the size of which is specified by
 * the user. Check Sum errors are simulated by giving payload headers a value
 * both client and server recognize as "bad".
 * 
 * @author Tom Carney
 * @version 1.0
 * @since 07/14/2016
 */

import helpers.Helper;
import helpers.ReceiverThread;
import helpers.SenderThread;

import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class P2Client {
    
    private InetAddress IPAddress;
    private DatagramSocket clientSocket;
    private SenderThread sender;
    private ReceiverThread receiver;
    
    // Store user specified parameters.
    private FileInputStream fStream;
    private int timeOutPeriod;
    private int maxDataSize;
    private int windowSize;
    private int errorPercent;
    
    // Store variables for Sliding Window. Terminology matches that of the data link 
    // layer even though it could be modified a bit.
    private int LAR = 0;    // Last Acknowledgement Received
    private int LFS = 0;    // Last Frame Sent
    private DatagramPacket[] window;
    
    // Used as a flag to say when the file has been totally read, initially false.
    private boolean doneReading = false;
    
    
    /**
     * A no argument constructor for the P2Client.
     */
    public P2Client() {
        
        inputSimulationParameters();
        
    } // end P2Client constructor
    
    
    /**
     * This method allows the Sending thread to indicate when the file has been 
     * completely read and windowed.
     * 
     * @param done - should be true if file is completely processed, false otherwise.
     */
    public void setDoneReading(boolean done) {
        
        doneReading = done;
        
    } // end setDoneReading
    
    
    /**
     * This method allows the Sending thread to indicate when the file has been 
     * completely read and windowed.
     * 
     * @param done - should true if file has been completely processed, false otherwise.
     */
    public boolean getDoneReading() {
        
        return doneReading;
        
    } // end getDoneReading

    
    /**
     * This method will provide the max data size of any application packet. If an error occurred 
     * while entering the value, a default data size is returned instead.
     * 
     * @return - The maximum data size, in bytes.
     */
    public int getMaxDataSize() {
        
        // -1 was saved if an error occurred.
        if(maxDataSize < 0) {
            
            return Helper.DEFAULTMAXDATASIZE;
            
        } else {
        
            return maxDataSize;
            
        }
        
    } // end getMaxDataSize
    
    
    /**
     * This method will provide the time out period specified by the user. If an error occurred 
     * while entering the value, a default period is provided.
     * 
     * @return - The time out period, in milliseconds.
     */
    public int getTimeOutPeriod() {
        
        // -1 was saved if an error occurred.
        if(timeOutPeriod < 0) {
            
            return Helper.DEFAULTTIMEOUT;
            
        } else {
        
            return timeOutPeriod;
            
        }
        
    } // end getTimeOutPeriod
    
    
    /**
     * This method will provide the window size specified by the user. If an error occurred 
     * while entering the value, a default size is provided.
     * 
     * @return - The window size, in packets.
     */
    public int getWindowSize() {
        
        // -1 was saved if an error occurred.
        if(windowSize < 0) {
            
            return Helper.DEFAULTWINDOWSIZE;
            
        } else {
        
            return windowSize;
            
        }
        
    } // end getWindowSize
    
    
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
     * This method will provide the "Last Acknowledgement Received".
     * 
     * @return - The value of the LAR.
     */
    public int getLAR() {
        
        return LAR;
        
    } // end getLAR
    
    
    /**
     * This method will provide the "Last Frame Sent".
     * 
     * @return - The value of the LFS.
     */
    public int getLFS() {
        
        return LFS;
        
    } // end getLAR
    
    
    /** This method will set the "Last Acknowledgement Received" to a specific value.
     * 
     * @param newLAR - The value to set the LAR to.
     */
    public void setLAR(int newLAR) {
        
        // There should be more checks here before changing the LAR but not needed for the demo, 
        // just tinkering.
        
        LAR = newLAR;
        
    } // end setLAR

    
    /**
     * This method will increment the value of "Last Acknowledgement Received".
     */
    public void incLAR() {
        
        LAR++;
        
    } // end incLAR
    
    
    /**
     * This method will increment the value of "Last Frame Sent".
     */
    public void incLFS() {
        
        LFS++;
        
    } // end incLFS
    
    
    /**
     * This method will return a DatagramPacket saved in the specified window index.
     * 
     * @param i - The window index with the desired contents.
     * 
     * @return - The requested DatagramPacket.
     */
    public DatagramPacket getPacketFromWindow(int i) {
        
        return window[i];
        
    } // end getPacketFromWindow
    

    /**
     * This method is used to determine if the sliding window has room to save/send another block 
     * of data. 
     * 
     * @return - A boolean true if there is room in the window, false otherwise.
     */
    public boolean isRoomInWindow() {
        
        // LFS - LAR should at most equal the window size. If it does then there is no room in the 
        // window and we'll have to wait for an acknowledgement to come in before proceeding.
        return ((LFS - LAR) < getWindowSize()); 
        
    } // end roomInWindow
    
    
    /**
     * This method accepts a DatagramPacket and index number. If the index is valid for the window
     * size, the packet is placed there.
     * 
     * @param packet - The DatagramPacket to store.
     * 
     * @param index - The window index to store the DatagramPacket.
     */
    public void putInWindow(DatagramPacket packet, int index) {
        
        if(index >= 0 && index < getWindowSize()) {
            
            window[index] = packet;
            
        } else {
            
            System.out.println("Invalid index specified, packet could not be windowed!");
            
        }
        
    } // end putInWindow

 
    /**
     * This method will gather specific parameters from the user used during the simulation.
     */
    private void inputSimulationParameters() {

        fStream = Helper.createFileInputStream("Enter file location (eg .\\2000Bytes.txt): ");
        maxDataSize = Helper.inputInteger("Enter max data size (in bytes): ");
        timeOutPeriod = Helper.inputInteger("Enter time out period (in milliseconds): ");
        windowSize = Helper.inputInteger("Enter a window size: ");
        errorPercent = Helper.inputInteger("Enter an error percentage: ");
        
    } // end getSimulationParameters
    
    
    /**
     * Once user parameters have been entered, this method is used to get the client side of the 
     * simulation up and running.
     */
    private void startSimulation() {
        
        System.out.println("\nStarting Simulation...\n");
        
        window = new DatagramPacket[getWindowSize()];
        
        try {
            
            // will return this computers IP (could use loop-back address directly 127.0.0.1)
            IPAddress = InetAddress.getLocalHost();
            System.out.println("Server At: " + IPAddress + ":" + Helper.PORT + "\n");
            
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(getTimeOutPeriod());
            
            sender = new SenderThread(this, clientSocket, IPAddress, fStream);
            sender.start(); 
            
            receiver = new ReceiverThread(this, clientSocket);
            receiver.start();
            
        } catch (UnknownHostException e) {
            
            System.out.println("Unable to resolve hostname!");
            
        } catch (SocketException e) {
            
            System.out.println("Unable to establish socket!");
            
        } // end try-catch block
        
    } // end startSimulation
    

    public static void main(String[] args) {
        
        P2Client client = new P2Client();
        
        client.startSimulation();
        
    } // end main
    
} // end P2Client