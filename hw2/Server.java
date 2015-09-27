import java.util.Scanner;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    public static class ConnectionInfo {
        private String ip = "";
        private int port = 0;
        private boolean isAlive = true;

        public ConnectionInfo() {}

        public ConnectionInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public void setIsAlive(boolean isAlive) {
            this.isAlive = isAlive;
        }

        public boolean getIsAlive() {
            return this.isAlive;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getIp() {
            return this.ip;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getPort() {
            return this.port;
        }

        public String toString() {
            return "ip: " + this.ip + ", port: " + this.port;
        }
    }

/*
    public static class LogicalTimestamp {

        private ConnectionInfo connectionInfo;
        private int timestamp;

        public LogicalTimestamp(ConnectionInfo connectionInfo, int timestamp) {
            this.connectionInfo = connectionInfo;
            this.timestamp = timestamp;
        }


        public ConnectionInfo getConnectionInfo() {
            return this.connectionInfo;
        }

        public int getTimestamp() {
            return this.timestamp;
        }

        public int compareTo(LogicalTimestamp logicalTimestamp) {
            if (this.timestamp < logicalTimestamp.getTimestamp()) {
                return -1;
            }
            else if (this.timestamp == logicalTimestamp.getTimestamp()) {

                if (this.connectionInfo.getPort() < logicalTimestamp.getConnectionInfo().getPort()) {

                    return -1;
                }
                else {

                    return 1;
                }
            }
            else {
                return 1;
            }
        }
    }
*/

    public static void main (String[] args) {

        Scanner sc = new Scanner(System.in);
        int myID = sc.nextInt();
        int numServer = sc.nextInt();
        int numSeat = sc.nextInt();

        Theater theater = new Theater(numSeat);

        ConnectionInfo myInfo = new ConnectionInfo();

        ArrayList<ConnectionInfo> servers = new ArrayList<ConnectionInfo>();
        List<Integer> logicalClocks = Collections.synchronizedList(new ArrayList<Integer>());

        SortedMap<Integer, Integer> criticalSectionQueue = Collections.synchronizedSortedMap(new TreeMap<Integer, Integer>());

        System.out.println("myId is: " + myID);
        System.out.println("numServerm is: " + numServer);
        System.out.println("numSeat is: " + numSeat);

        sc.nextLine(); // Consume newline for scanner. Java, why do you make things so awkward?

        // Collect other server info
        for (int i = 0; i < numServer; i++) {
            try {
                String otherServerInfo = sc.nextLine();
                String[] split = otherServerInfo.split(":");

                String ip = split[0];
                int port = Integer.parseInt(split[1]);

                ConnectionInfo otherServer = new ConnectionInfo(ip, port);

                System.out.println("new input received");
                servers.add(otherServer);
                logicalClocks.add(0);
            }
            catch(Exception e) {
                System.out.println("Warning: unable to parse server input");
            }

        }

        // Get this server's IP/port
        try {
            myInfo = servers.get(myID - 1);
            System.out.println("myinfo is: " + myInfo);
        }
        catch(Exception e) {
            System.out.println("Warning: this server's id is out of bounds.");
        }

        // tmp debug - all server list
        for (int i = 0; i < servers.size(); i++) {
            System.out.println("other is: " + servers.get(i));
        }

        ServerHandler serverHandler = new ServerHandler(
            servers,
            myID,
            myInfo,
            criticalSectionQueue,
            logicalClocks
        );

        // Synchronize w/ other servers if available
        for (int i = 0; i < servers.size(); i++) {

            if (i != myID-1) {
            
                try {
                    ConnectionInfo senderConnectionInfo = servers.get(i);

                    TcpClient tcpClient = new TcpClient(senderConnectionInfo);
                    String unparsedResponse = tcpClient.sendTCP("synchronizeNewServer");
                    tcpClient.close();

                    String[] responseSplit = new String[numServer * 2];
                    responseSplit = unparsedResponse.split(" ");

                    for (int j = 0; j < responseSplit.length; j += 2) {
                        logicalClocks.set(
                            Integer.parseInt(responseSplit[j]),
                            Integer.parseInt(responseSplit[j+1])
                        );
                    }

                    System.out.println("self sync successful!");

                    break;
                } catch(Exception e) {
                    System.out.println("synchronizeNewServer fail: no other servers available");
                }
            }
        }

        System.out.println("sync check: " + logicalClocks);

        while(true) {
            System.out.println("loop iterate!");



            ServerSocket serverSocket = null;

            try {
                serverSocket = new ServerSocket(myInfo.getPort());
            }
            catch (IOException e) {
                // Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Socket clientSocket = serverSocket.accept();
                Thread clientHandlerThread = new Thread(new ClientHandler(clientSocket, theater, serverHandler));

                //start a tcp thread to handle incoming connection
                clientHandlerThread.start();
                serverSocket.close();
            }
            catch (IOException e) {
                // Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static class ClientHandler extends Thread {
        Socket clientSocket;
        Theater theater;
        ServerHandler serverHandler;

        public ClientHandler(Socket clientSocket, Theater theater, ServerHandler serverHandler) {
            this.clientSocket = clientSocket;
            this.theater = theater;
            this.serverHandler = serverHandler;
        }

        public void run() {

            Scanner clientInput;
            try {
                clientInput = new Scanner(clientSocket.getInputStream());

                PrintWriter pout = new PrintWriter(clientSocket.getOutputStream());
                String unparsedCommand = clientInput.nextLine();
                String[] commandSplit = new String[3];
                String commandKeyword = "";

                try {
                    commandSplit = unparsedCommand.split(" ");
                    commandKeyword = commandSplit[0];
                }
                catch(Exception commandParseException) {
                    System.out.println("Warning: unable to parse client input command");
                }

                //Scanner st = new Scanner(command);
                //String tag = st.next();

                System.out.println("clientInput 1 is: " + commandKeyword);

                switch(commandKeyword) {
                    case "reserve":
                        try {
                            // input: reserve <name>
                            String name = commandSplit[1];

                            String result = this.theater.reserve(name);

                            System.out.println(result);
                            pout.println(result);
                        } catch(Exception e) {
                            System.out.println("Warning: unable to reserve seat");
                        }

                        break;

                    case "bookSeat":
                        try {
                            // input: bookSeat <name> <seatNum>
                            String name = commandSplit[1];
                            int seatNum = Integer.parseInt(commandSplit[2]);

                            String result = this.theater.bookSeat(name, seatNum);

                            System.out.println(result);
                            pout.println(result);
                        } catch(Exception e) {
                            System.out.println("Warning: unable to book seat");
                        }

                        break;

                    case "search":
                        try {
                            // input: search <name>
                            String name = commandSplit[1];

                            String result = this.theater.search(name);

                            System.out.println(result);
                            pout.println(result);
                        } catch(Exception e) {
                            System.out.println("Warning: unable to search by name");
                        }

                        break;

                    case "delete":
                        try {
                            // input: delete <name>
                            String name = commandSplit[1];

                            String result = this.theater.delete(name);

                            System.out.println(result);
                            pout.println(result);
                        } catch(Exception e) {
                            System.out.println("Warning: unable to delete by name");
                        }

                        break;

                    case "requestCriticalSection":
                        try {
                            //this.serverHandler.incrementAndGetLocalLogicalClock();
                            // TODO: only tick when you send, not receive

                            // input: requestCriticalSection remoteServerNumber remoteServerTimestamp
                            int remoteServerNumber = Integer.parseInt(commandSplit[1]);
                            int remoteServerTimestamp = Integer.parseInt(commandSplit[2]);

                            String responseMessage = serverHandler.processAcknowledgeRequestCriticalSectionMessage(remoteServerNumber, remoteServerTimestamp);

                            System.out.println(responseMessage);
                            pout.println(responseMessage);
                        } catch(Exception e) {
                            System.out.println(e);
                        }

                        break;

                    case "synchronizeNewServer":
                        try {

                            // input: synchronizeNewServer

                            String responseMessage = serverHandler.processSynchronizeNewServerMessage();

                            System.out.println("synced remote server");
                            pout.println(responseMessage);
                        } catch(Exception e) {
                            System.out.println(e);
                        }

                        break;

/*
                    case "releaseCriticalSection":
                        try {
                            // input: releaseCriticalSection serverNumber timestamp
                            int serverNumber = Integer.parseInt(commandSplit[1]);
                            int timestamp = Integer.parseInt(commandSplit[2]);

                            // TODO: add action
                            String result = unparsedCommand;

                            System.out.println(result);
                            pout.println(result);
                        } catch(Exception e) {
                            System.out.println("Warning: unable to delete by name");
                        }

                        break;
*/
                    default:
                        break;
                }

                pout.flush();
                clientSocket.close();
            }
            catch (IOException e) {
                // Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static class ServerHandler {

        int myID = 0;
        ConnectionInfo myConnectionInfo;
        ArrayList<ConnectionInfo> servers = new ArrayList<ConnectionInfo>();
        SortedMap<Integer, Integer> criticalSectionQueue;
        List<Integer> logicalClocks;

        public ServerHandler(ArrayList<ConnectionInfo> servers, int myID, ConnectionInfo myConnectionInfo, SortedMap<Integer, Integer> criticalSectionQueue, List<Integer> logicalClocks) {
            this.servers = servers;
            this.myID = myID;
            this.myConnectionInfo = myConnectionInfo;
            this.criticalSectionQueue = criticalSectionQueue;
            this.logicalClocks = logicalClocks;
        }

        public synchronized int incrementAndGetLocalLogicalClock() {
            int localLogicalClock = this.logicalClocks.get(this.myID - 1);

            localLogicalClock++;

            this.logicalClocks.set(this.myID - 1, localLogicalClock);

            return localLogicalClock;
        }

        public synchronized int getLocalLogicalClock() {
            int localLogicalClock = this.logicalClocks.get(this.myID - 1);

            return localLogicalClock;
        }

        public synchronized String processAcknowledgeRequestCriticalSectionMessage(int remoteServerNumber, int remoteServerTimestamp) {

            this.logicalClocks.set(remoteServerNumber-1, remoteServerTimestamp);

            this.criticalSectionQueue.put(remoteServerTimestamp, remoteServerNumber);

            String response = "acknowledgeRequestCriticalSection"
                            + " "
                            + this.myID
                            + " "
                            + this.getLocalLogicalClock()
                            ;

            return response;

            /*
            try {

                ConnectionInfo senderConnectionInfo = this.servers.get(serverNumber);

                TcpClient tcpClient = new TcpClient(senderConnectionInfo);
                tcpClient.sendTCP(
                    "acknowledgeRequestCriticalSection"
                    + " " + this.myID
                    + " " + this.localLogicalClock.get()
                );
            } catch(Exception e) {
                System.out.println("acknowledgeRequestCriticalSection: server " + serverNumber + " is dead, setting status");

                ConnectionInfo serverConnectionInfo = this.servers.get(serverNumber);
                serverConnectionInfo.setIsAlive(false);
                this.servers.set(serverNumber, serverConnectionInfo);
            }
            */

        }

        public synchronized String processSynchronizeNewServerMessage() {
            String outputMessage = "";

            for (int i = 0; i < this.logicalClocks.size(); i++) {

                if (i > 0) {
                    outputMessage += " ";
                }

                outputMessage += i
                              + " "
                              + this.logicalClocks.get(i)
                              ;
            }

            return outputMessage;
        };
/*
        public synchronized void sendRequestCriticalSection(logicalTimestamp) {

            int logicalTimestamp = localLogicalClock.incrementAndGet();

            int serverCounter = 0;
            for (int i = 0; i < this.servers.size(); i++) {
                try {
                    if (i != this.myID - 1) {
                        serverCounter = i;
                        ConnectionInfo serverInfo = this.servers.get(i);

                        TcpClient tcpClient = new TcpClient(serverInfo);
                        tcpClient.sendTCP(
                            "requestCriticalSection"
                            + " " + this.myConnectionInfo.getIp()
                            + " " + this.myConnectionInfo.getPort()
                            + " " + logicalTimestamp
                        );

                    }
                } catch(Exception e) {
                    System.out.println("requestCriticalSection: server " + serverCounter + " is dead, setting status");
                    ConnectionInfo serverConnectionInfo = this.servers.get(serverCounter);
                    serverConnectionInfo.setIsAlive(false);
                    this.servers.set(serverCounter, serverConnectionInfo);
                }
            }

            this.criticalSectionQueue.put(logicalTimestamp, this.myConnectionInfo);
        }

        public synchronized void acknowledgeCriticalSectionRequest() {

            int serverCounter = 0;
            for (int i = 0; i < this.servers.size(); i++) {
                try {
                    if (i != this.myID - 1) {
                        serverCounter = i;
                        ConnectionInfo serverInfo = this.servers.get(i);

                        TcpClient tcpClient = new TcpClient(serverInfo);
                        tcpClient.sendTCP(
                            "acknowledgeCriticalSectionRequest"
                            + " " + this.myConnectionInfo.getIp()
                            + " " + this.myConnectionInfo.getPort()
                            + " " + logicalTimestamp
                        );

                    }
                } catch(Exception e) {
                    System.out.println("requestCriticalSection: server " + serverCounter + " is dead, setting status");
                    ConnectionInfo serverConnectionInfo = this.servers.get(serverCounter);
                    serverConnectionInfo.setIsAlive(false);
                    this.servers.set(serverCounter, serverConnectionInfo);
                }
            }

        }
*/

    }

    public static class TcpClient {

        Socket clientSocket;
        PrintWriter out;
        BufferedReader in;

        public TcpClient(ConnectionInfo connectionInfo) {

            try {
                this.clientSocket = new Socket(connectionInfo.getIp(), connectionInfo.getPort());
                this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

            }
            catch (Exception e) {
                //System.err.println(e);
                //System.out.println("Server " + serverCounter + " unavailable, trying next server...");
            }
        }

        public String sendTCP(String Message) {
            try {
                this.out.println(Message);
                return this.in.readLine();
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return "error";
        }

        public void close() {
            try {
                this.out.close();
                this.in.close();
                this.clientSocket.close();
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                System.err.println(e);
            }
        }
    }

    public static class Theater {
        ConcurrentHashMap<Integer, String> theaterSeats = new ConcurrentHashMap<Integer, String>();

        int totalSeats = 0;

        public Theater(int totalSeats) {
            this.totalSeats = totalSeats;
        }

        public synchronized String reserve(String name) {
            // Step 1 - check if seats are available
            if (this.isSoldOut() == true) {
                return "Sold out - No seat available";
            }

            // Step 2 - check if this user already has a reservation
            if (this.theaterSeats.containsValue(name) == true) {
                return "Seat already booked against the name provided";
            }

            // Step 3 - ok to reserve next available
            String outputMessage = "";
            for (int i = 1; i <= this.totalSeats; i++) {
                 if (this.theaterSeats.containsKey(i) == false) {
                    this.theaterSeats.put(i, name);
                    outputMessage = "Seat assigned to you is " + i;
                    break;
                 }
            }

            return outputMessage;
        }

        public synchronized String bookSeat(String name, int seatNum) {
            // Step 1 - check if seats are available
            if (this.isSoldOut() == true) {
                return "Sold out - No seat available";
            }

            // Step 2 - check if this name already has a reservation
            if (this.theaterSeats.containsValue(name) == true) {
                return "Seat already booked against the name provided";
            }

            // Step 3 - check if this seat already has a reservation
            if (this.theaterSeats.containsKey(seatNum) == true) {
                return seatNum + " is not available";
            }

            this.theaterSeats.put(seatNum, name);
            return "Seat assigned to you is " + seatNum;
        }

        public synchronized String search(String name) {

            // Step 1 - search for name (no other steps necessary)
            String outputMessage = "No reservation found for " + name;

            for (int i = 1; i <= this.totalSeats; i++) {
                if (this.theaterSeats.containsKey(i) == true) {
                    if (this.theaterSeats.get(i).equals(name) == true) {
                        outputMessage = String.valueOf(i);
                        break;
                    }
                }
            }

            return outputMessage;
        }

        public synchronized String delete(String name) {

            // Step 1 - search for name
            String outputMessage = "No reservation found for " + name;

            for (int i = 1; i <= this.totalSeats; i++) {
                if (this.theaterSeats.containsKey(i) == true) {
                    if (this.theaterSeats.get(i).equals(name) == true) {

                        // Step 2 - delete key/val pair
                        this.theaterSeats.remove(i);
                        outputMessage = String.valueOf(i);
                        break;
                    }
                }
            }

            return outputMessage;
        }

        private synchronized boolean isSoldOut() {
            int takenSeats = this.theaterSeats.size();

            if (takenSeats >= totalSeats) {
                return true;
            }
            else {
                return false;
            }
        }
    }
}
