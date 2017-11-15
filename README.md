# Java-UDPClientServer

As noted, this was my term project for ICS460, basically a simple FTP client/server. 

The intent of the project was to better understand the various layers of the OSI network model. To do that, we used the Java Sockets/Network API, starting with UDP but added a sliding window and other features to make the process more reliable. We also built in error simulations to show what can go wrong and how the system should respond.

## Note:

This is a multithreaded application however it was written prior taking classes on multithreaded programming. Not an excuse but I should point out there are certainly ways in which this could/should be more "thread safe". Certain items should be synchronized, threads self executing vs extended, etc.
