package helpers;

/**
 * Class: ICS 460 - Computer Networks and Security <br>
 * Instructor: Damodar Chetty <br>
 * Description: Program 2, Implementing "Sliding Window" functionality above the 
 * Transport layer to make UDP more reliable. <br>
 * Due: 08/03/2016 <br><br>
 * 
 * This particular class is a helper class for our UDP Sliding Window simulation. 
 * It is used house static methods and values used commonly by both client and 
 * server. It is provides uniform functionality that both need but also makes for 
 * easy modifications when simulating.
 * 
 * 
 * @author Tom Carney
 * @version 1.0
 * @since 07/14/2016
 */

// For user input
import java.io.BufferedReader;
import java.io.InputStreamReader;

// For file I/O
import java.io.FileInputStream;
import java.io.FileOutputStream;

// Throws
import java.io.FileNotFoundException;
import java.io.IOException;


public class Helper {
    
    // Store static parameters for the simulation.
    public static final int PORT = 65000;
    public static final short CHECKSUMGOOD = 1;
    public static final short CHECKSUMBAD = 0;
    public static final int DEFAULTTIMEOUT = 3000;
    public static final int DEFAULTMAXDATASIZE = 500;
    public static final int DEFAULTBUFFERSIZE = 1024;
    public static final int DEFAULTWINDOWSIZE = 7;
    public static final int DEFAULTERRORPERCENT = 25;
    public static final int APPLICATIONHEADERSIZE = 12;
    public static final int ACKNOWLEDGEMENTHEADERSIZE = 8;
    
    // 100 milliseconds * the values below.
    public static final int SENDNEWPACKETDELAY = 0;
    public static final int WINDOWFULLSLEEPMODIFIER = 0;
    
    // Decide whether cumulative acknowledgments are allowed.
    public static final boolean CUMULATIVE = false;
    
    /**
     * This method accepts a String "message" prompting the user for another String used 
     * by the application. In case of the unexpected it will loop, re-prompting the user
     * until the string is properly read.
     * 
     * @param message - A user prompt.
     * 
     * @return - The string requested from the user.
     */
    public static String inputString(String message) {
        
        String tempString = null;
        boolean loop = true;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        
        while(loop) {
            
            System.out.print(message);
            
            try {
                
                tempString = input.readLine();
                loop = false;
                
            } catch (IOException e) {
                
                System.out.println("I/O Exception while reading input, try again!\n");
                
            } // end try-catch
            
        } // end while loop
        
        return tempString;
        
    } // end inputString

    
    /**
     * This method accepts a String "message" prompting the user for a non-negative integer 
     * value. It will return the integer entered or a negative value if an error occurred.
     * 
     * @param message - A user prompt.
     * 
     * @return - An integer value for the parameter or a negative value if an error occurred.
     */
    public static int inputInteger(String message) {
        
        // initially an invalid value (for this application).
        int tempInt = -1;
        
        boolean loop = true;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        
        while(loop) {
            
            System.out.print(message);
            
            try {
                
                tempInt = Integer.parseInt(input.readLine());
                
                if(tempInt < 0) {
                    
                    System.out.println("Entry must be non-negative, try again!\n");
                    
                } else {
                    
                    loop = false;
                    
                }
            
            } catch (NumberFormatException e) {
                
                System.out.println("Entry must be an integer, try again!\n");
                
            } catch (IOException e) {
                
                // For the simulation, if this happens, stop and use a default.
                System.out.println("I/O Exception while reading input!\n");
                loop = false;
                
            } // end try-catch
            
        } // end while loop
        
        return tempInt;
        
    } // end inputInteger
    
    
    /**
     * This method is used to open the file specified by the user and make it available for 
     * reading via a FileInputStream.
     * 
     * @param message - A message prompting the user for location of the file.
     * 
     * @return - A FileInputStream containing the file.
     */
    public static FileInputStream createFileInputStream(String message) {
        
        boolean loop = true;
        String fileLocation;
        FileInputStream fStream = null;
        
        // Loop until the user enters a valid path/filename.
        while(loop) {
            
            fileLocation = inputString(message);
            
            try {
                
                fStream = new FileInputStream(fileLocation);
                loop = false;
                
            } catch (FileNotFoundException e) {
                
                System.out.println("File not found, try again!\n");
                
            } // end try-catch block
            
        } // end while loop
        
        return fStream;
        
    } // end createFileInputStream
    
    
    /**
     * This method is used to create the file specified by the user and make it available for
     * writing to via a FileOutputStream.
     * 
     * @param message - A message prompting the user for where to put the data.
     * 
     * @return - A FileOutputStream which writes to the specified location.
     */
    public static FileOutputStream createFileOutputStream(String message) {
        
        boolean loop = true;
        String fileLocation;
        FileOutputStream fStream = null;
        
        // Loop until the user enters a valid path/filename.
        while(loop) {
            
            fileLocation = inputString(message);
            
            try {
                
                fStream = new FileOutputStream(fileLocation);
                loop = false;
                
            } catch (FileNotFoundException e) {
                
                System.out.println("File cannot be written as entered, try again!\n");
                
            } // end try-catch block
            
        } // end while loop
        
        return fStream;
        
    } // end createFileOutputStream

    
    /**
     * This method will convert an integer to an array of bytes. modified from version found at
     * http://stackoverflow.com/questions/1936857/convert-integer-into-byte-array-java.
     * 
     * @param i - The integer to convert.
     * 
     * @return - The integer as an array of bytes (most significant in lowest index).
     */
    public static byte[] toBytes(int i){
        
      byte[] result = new byte[4];

      result[0] = (byte) (i >> 24);
      result[1] = (byte) (i >> 16);
      result[2] = (byte) (i >> 8);
      result[3] = (byte) (i);

      return result;
      
    } // end toBytes (integer version)
    
    
    /**
     * This method will convert a short to an array of bytes. modified from version found at
     * http://stackoverflow.com/questions/1936857/convert-integer-into-byte-array-java.
     * 
     * @param s - The short to convert.
     * 
     * @return - The short as an array of bytes (most significant in lowest index).
     */
    public static byte[] toBytes(short s)
    {
      byte[] result = new byte[2];

      result[0] = (byte) (s >> 8);
      result[1] = (byte) (s);

      return result;
      
    } // end toBytes (short version)
    
    
    /**
     * This method will take a byte array (up to 4 bytes in length) and convert it into 
     * an integer. Longer arrays will be truncated and shorter arrays can be type-casted
     * upon return. For example, an array of only two bytes can be cast to a short.
     * 
     * @param array - The byte array to convert (most significant in lowest index).
     * 
     * @return - The bytes of the array converted to an integer.
     */
    public static int toNumber(byte[] array) {
        
        int tempNum = array[0];
        
        for(int i = 1 ; i < array.length ; i++) {
            
            tempNum = tempNum << 8;
            tempNum = tempNum | array[i];
            
        }
        
        return tempNum;
        
    } // end toNumber
    
    
    /**
     * This method will retrieve the checksum from the header portion of the application
     * payload.
     * 
     * @param payload - The data portion of a DatagramPacket.
     * 
     * @return - The checksum.
     */
    public static short retrieveCheckSum(byte[] payload) {
        
        byte[] tempArray = {payload[0], payload[1]};
        
        return (short)toNumber(tempArray);
        
    } // end retrieveCheckSum
    
    
    /**
     * This method will retrieve the length from the header portion of the application
     * payload. 
     * 
     * Reminder... The length is the total number of bytes, header included.
     * 
     * @param payload - The data portion of a DatagramPacket.
     * 
     * @return - The length.
     */
    public static short retrieveLength(byte[] payload) {
        
        byte[] tempArray = {payload[2], payload[3]};
        
        return (short)toNumber(tempArray);
        
    } // end retrieveLength
    
    
    /**
     * This method will retrieve the acknowledgement number from the header portion of
     * the application payload. 
     * 
     * Reminder... The acknowledgement number for the client is the lowest sequence number 
     * unacknowledged. For the server, this is the sequence number being acknowledged.
     * 
     * @param payload - The data portion of a DatagramPacket.
     * 
     * @return - The acknowledgement number.
     */
    public static int retrieveAckNum(byte[] payload) {
        
        byte[] tempArray = {payload[4], payload[5], payload[6], payload[7]};
        
        return toNumber(tempArray);
        
    } // end retrieveAckNum
    
    
    /**
     * This method will retrieve the sequence number from the header portion of the 
     * application payload. 
     * 
     * Reminder... This is not part of an acknowledgement header which also lacks a 
     * data component.
     * 
     * @param payload - The data portion of a DatagramPacket.
     * 
     * @return - The sequence number.
     */
    public static int retrieveSeqNum(byte[] payload) {
        
        byte[] tempArray = {payload[8], payload[9], payload[10], payload[11]};
        
        return toNumber(tempArray);
        
    } // end retrieveSeqNum
    
    
    /**
     * This method places the length specified into the appropriate indices of the
     * payload. 
     * 
     * Reminder... Only pass the number of data bytes being sent. Based on the value
     * passed, the method will choose the appropriate header length to be added. 
     * 
     * 
     * @param payload - The buffer being used by the DatagramPacket.
     * 
     * @param dataLength - The number of data bytes being sent.
     */
    public static void bufferLength(byte[] payload) {
        
        byte[] length = toBytes((short)ACKNOWLEDGEMENTHEADERSIZE);
        
        // Place the length values in their header locations.
        payload[2] = length[0];
        payload[3] = length[1];
        
    } // end bufferLength
    
    
    /**
     * This method places the length specified into the appropriate indices of the
     * payload. 
     * 
     * Reminder... Only pass the number of data bytes being sent. Based on the value
     * passed, the method will choose the appropriate header length to be added. 
     * 
     * @param payload - The buffer being used by the DatagramPacket.
     * 
     * @param dataLength - The number of data bytes being sent.
     */
    public static void bufferLength(byte[] payload, int dataLength) {
        
        byte[] length = toBytes((short)(APPLICATIONHEADERSIZE + dataLength));
        
        // Place the length values in their header locations.
        payload[2] = length[0];
        payload[3] = length[1];
        
    } // end bufferLength
    
    
    /**
     * This method will place a two byte checksum into the appropriate indices of the 
     * payload. 
     * 
     * Reminder... For the simulation, this really only places the static good or bad
     * checksum values into said locations. No calculated checksum is being generated.
     * 
     * @param payload - The buffer being used by the DatagramPacket.
     * 
     * @param checkSumValue - The value to be buffered.
     */
    public static void bufferCheckSum(byte[] payload, short checkSumValue) {
        
        // For the simulation, initially setting a dummy "good" checksum. 
        byte[] checkSum = toBytes(checkSumValue);
        
        // Place checksum values in their header locations.
        payload[0] = checkSum[0];
        payload[1] = checkSum[1];
         
    } // end bufferCheckSum
    
    
    /**
     * This method will place a four byte acknowledgement number into the appropriate 
     * indices of the payload header. 
     * 
     * Reminder... For the client, this number is the sequence number currently being 
     * waited on (LAR + 1). For the server, this is acknowledging a specific frame 
     * that has been received so the window can be slid.
     * 
     * @param payload - The buffer being used by the DatagramPacket.
     * 
     * @param ackNum - The value to be buffered.
     */
    public static void bufferAckNumber(byte[] payload, int ackNum) {
        
        // LAR is the last acknowledged number, we are waiting on the next.
        byte[] ackBytes = toBytes(ackNum);
        
        // Place the values in their header locations. 
        payload[4] = ackBytes[0];
        payload[5] = ackBytes[1];
        payload[6] = ackBytes[2];
        payload[7] = ackBytes[3];
        
    } // end bufferAckNumber
    
    
    /**
     * This method will place the four byte sequence number of the data being sent into 
     * the appropriate indices of the header. 
     * 
     * @param payload - The buffer being used by the DatagramPacket.
     * 
     * @param seqNum - The sequence number to be buffered.
     */
    public static void bufferSeqNumber(byte[] payload, int seqNum) {
        
        byte[] seqNumber = toBytes(seqNum);
        
        // Place the values in their header locations.
        payload[8] = seqNumber[0];
        payload[9] = seqNumber[1];
        payload[10] = seqNumber[2];
        payload[11] = seqNumber[3];
        
    } // end bufferSeqNumber
    
    
    /**
     * This method will return a random whole percent between 1 and 100 (inclusive)
     * 
     * @return - An integer between 1 and 100.
     */
    public static int getRandomPercent() {
        
        return ((int)(Math.random() * 100)) + 1;
        
    } // end getRandomPercent
    
    
    /**
     * This method is used to determine whether a packet was received or will be simulated lost.
     * 
     * @return - A boolean true if the packet was received, false if "lost".
     */
    public static boolean isPacketReceived(int errorPercent) {
        
        // An errorPercent of 0 will cause a perfect run, all else may cause simulated errors.
        return getRandomPercent() >= errorPercent;
        
    } // end isPacketReceived
    
    
    /**
     * This method is used to determine whether a packets is good and whether processing should
     * continue.
     * 
     * @return - A boolean true if the checksum is good, false otherwise.
     */
    public static boolean isCheckSumGood(byte[] payload) {
        
        return retrieveCheckSum(payload) == CHECKSUMGOOD;
        
    } // end isCheckSumGood
    
    
    /**
     * This method is used to determine whether a packets is good and whether processing should
     * continue.
     * 
     * @return - A boolean true if the checksum is good, false otherwise.
     */
    public static boolean shouldCheckSumError(int errorPercent) {
        
        // An errorPercent of 0 will cause a perfect run, all else may cause simulated errors.
        return errorPercent >= getRandomPercent();
        
    } // end shouldCheckSumError
    
    
} // end Helper