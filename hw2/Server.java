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

    public static void main (String[] args) {

        Scanner sc = new Scanner(System.in);
        int myID = sc.nextInt();
        int numServer = sc.nextInt();
        int numSeat = sc.nextInt();

        Theater theater = new Theater(numSeat);

        ConnectionInfo myInfo = new ConnectionInfo();

        ArrayList<ConnectionInfo> servers = new ArrayList<ConnectionInfo>();
        List<Float> logicalClocks = Collections.synchronizedList(new ArrayList<Float>());

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
                logicalClocks.add(Float.POSITIVE_INFINITY);
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
            logicalClocks,
            theater
        );

        // Synchronize w/ other servers if available
        for (int i = 0; i < servers.size(); i++) {

            if (i != myID-1) {

                try {
                    ConnectionInfo senderConnectionInfo = servers.get(i);

                    TcpClient tcpClient = new TcpClient(senderConnectionInfo);
                    String unparsedResponse = tcpClient.sendTCP("synchronizeNewServerClocks");
                    tcpClient.close();

                    String[] responseSplit = new String[numServer * 2];
                    responseSplit = unparsedResponse.split(" ");

                    for (int j = 0; j < responseSplit.length; j += 2) {

                        int otherServerNumber = Integer.parseInt(responseSplit[j]);
                        float otherServerTimestamp = Float.POSITIVE_INFINITY;

                        String otherServerTimestampString = responseSplit[j+1];
                        if (otherServerTimestampString.equals("Infinity") == false) {
                            otherServerTimestamp = Float.parseFloat(otherServerTimestampString);
                        }

                        logicalClocks.set(
                            otherServerNumber,
                            otherServerTimestamp
                        );
                    }

                    System.out.println("self clock sync successful! clocks are: " + serverHandler.logicalClocks);

                    break;
                } catch(Exception e) {
                    System.out.println("synchronizeNewServerClocks fail: server " + (i + 1) + " unavailable");
                }
            }
        }

        for (int i = 0; i < servers.size(); i++) {

            if (i != myID-1) {

                try {
                    ConnectionInfo senderConnectionInfo = servers.get(i);

                    TcpClient tcpClient = new TcpClient(senderConnectionInfo);
                    String unparsedResponse = tcpClient.sendTCP("synchronizeNewServerCriticalSection");
                    tcpClient.close();

                    String[] responseSplit = unparsedResponse.split(" ");

                    if (responseSplit.length > 1) {
                        for (int j = 0; j < responseSplit.length; j += 2) {
                            theater.theaterSeats.put(
                                Integer.parseInt(responseSplit[j]),
                                responseSplit[j+1]
                            );
                        }
                    }

                    System.out.println("self critical section sync successful! critical section data is: " + theater.theaterSeats);

                    break;
                } catch(Exception e) {
                    System.out.println("synchronizeNewServerCriticalSection fail: server " + (i + 1) + " unavailable");
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
                            this.serverHandler.sendRequestCriticalSection();

                            while(this.serverHandler.canEnterCriticalSection() == false) {
                                System.out.println("crit section check is: " + this.serverHandler.logicalClocks);
                            }

                            // input: reserve <name>
                            String name = commandSplit[1];

                            String result = this.theater.reserve(name);

                            this.serverHandler.sendReleaseCriticalSection();

                            System.out.println(result);
                            pout.println(result);
                        } catch(Exception e) {
                            System.out.println("Warning: unable to reserve seat");
                        }

                        break;

                    case "bookSeat":
                        try {
                            this.serverHandler.sendRequestCriticalSection();

                            while(this.serverHandler.canEnterCriticalSection() == false) {
                                System.out.println("crit section check is: " + this.serverHandler.logicalClocks);

                            }

                            // input: bookSeat <name> <seatNum>
                            String name = commandSplit[1];
                            int seatNum = Integer.parseInt(commandSplit[2]);

                            String result = this.theater.bookSeat(name, seatNum);

                            this.serverHandler.sendReleaseCriticalSection();

                            System.out.println(result);
                            pout.println(result);
                        } catch(Exception e) {
                            System.out.println("Warning: unable to book seat");
                        }

                        break;

                    case "search":
                        try {
                            this.serverHandler.sendRequestCriticalSection();

                            while(this.serverHandler.canEnterCriticalSection() == false) {
                                System.out.println("crit section check is: " + this.serverHandler.logicalClocks);
                            }

                            // input: search <name>
                            String name = commandSplit[1];

                            String result = this.theater.search(name);

                            this.serverHandler.sendReleaseCriticalSection();

                            System.out.println(result);
                            pout.println(result);
                        } catch(Exception e) {
                            System.out.println("Warning: unable to search by name");
                        }

                        break;

                    case "delete":
                        try {
                            this.serverHandler.sendRequestCriticalSection();

                            while(this.serverHandler.canEnterCriticalSection() == false) {
                                System.out.println("crit section check is: " + this.serverHandler.logicalClocks);
                            }

                            // input: delete <name>
                            String name = commandSplit[1];

                            String result = this.theater.delete(name);

                            this.serverHandler.sendReleaseCriticalSection();

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
                            float remoteServerTimestamp = Float.POSITIVE_INFINITY;

                            // TODO: put this in a method
                            String otherServerTimestampString = commandSplit[2];
                            if (otherServerTimestampString.equals("Infinity") == false) {
                                remoteServerTimestamp = Float.parseFloat(otherServerTimestampString);
                            }

                            String responseMessage = serverHandler.processAcknowledgeRequestCriticalSectionMessage(remoteServerNumber, remoteServerTimestamp);

                            System.out.println(responseMessage);
                            pout.println(responseMessage);
                        } catch(Exception e) {
                            System.out.println(e);
                        }

                        break;

                    case "synchronizeNewServerClocks":
                        try {

                            // input: synchronizeNewServer
                            String responseMessage = serverHandler.processSynchronizeNewServerMessage();

                            System.out.println("synced remote server");
                            pout.println(responseMessage);
                        } catch(Exception e) {
                            System.out.println(e);
                        }

                        break;

                    case "synchronizeNewServerCriticalSection":
                        try {

                            // input: synchronizeNewServerCriticalSection
                            String responseMessage = theater.serializeTheaterData();

                            System.out.println("synchronizeNewServerCriticalSection message: " + responseMessage);
                            pout.println(responseMessage);
                        } catch(Exception e) {
                            System.out.println(e);
                        }

                        break;

                    case "releaseCriticalSection":
                        try {

                            // input: releaseCriticalSection serverNumber timestamp
                            int serverNumber = Integer.parseInt(commandSplit[1]);
                            float remoteServerTimestamp = Float.POSITIVE_INFINITY;

                            // TODO: put this in a method
                            System.out.println("releaseCriticalSection msg is: " + unparsedCommand);
                            String otherServerTimestampString = commandSplit[2];
                            System.out.println("otherServerTimestampString is: " + otherServerTimestampString);
                            if (otherServerTimestampString.equals("Infinity") == false) {
                                remoteServerTimestamp = Float.parseFloat(otherServerTimestampString);
                            }

                            System.out.println("remoteServerTimestamp check is: " + remoteServerTimestamp);

                            String[] filteredData = new String[commandSplit.length-3];
                            for (int j = 0; j < filteredData.length; j++) {
                                System.out.println("filtered chunk is: " + commandSplit[j+3]);
                                filteredData[j] = commandSplit[j+3];
                            }


                            String responseMessage = serverHandler.processUpdateCriticalSectionMessage(serverNumber, remoteServerTimestamp, filteredData);

                            System.out.println("updateCriticalSection message: " + responseMessage);
                            pout.println("remote server crit section update ok");
                        } catch(Exception e) {
                            System.out.println("landed in exception");
                            System.out.println(e);
                        }

                        break;

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
        List<Float> logicalClocks;
        Theater theater;

        public ServerHandler(ArrayList<ConnectionInfo> servers, int myID, ConnectionInfo myConnectionInfo, List<Float> logicalClocks, Theater theater) {
            this.servers = servers;
            this.myID = myID;
            this.myConnectionInfo = myConnectionInfo;
            this.logicalClocks = logicalClocks;
            this.theater = theater;
        }

        public synchronized float incrementAndGetLocalLogicalClock() {

            float localLogicalClock = this.logicalClocks.get(this.myID - 1);

            if (localLogicalClock == Float.POSITIVE_INFINITY) {
                System.out.println("logicalClock if ok");
                localLogicalClock = 0;
            }
            else {
                System.out.println("logicalClock else ok");
                localLogicalClock++;
            }

            this.logicalClocks.set(this.myID - 1, localLogicalClock);

            return localLogicalClock;
        }

        public synchronized float getLocalLogicalClock() {
            float localLogicalClock = this.logicalClocks.get(this.myID - 1);

            return localLogicalClock;
        }

        public synchronized String processAcknowledgeRequestCriticalSectionMessage(int remoteServerNumber, float remoteServerTimestamp) {

            this.logicalClocks.set(remoteServerNumber-1, remoteServerTimestamp);

            String response = "acknowledgeRequestCriticalSection"
                            + " "
                            + this.myID
                            + " "
                            + this.getLocalLogicalClock()
                            ;

            return response;
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

        public synchronized String processUpdateCriticalSectionMessage(int serverNumber, float timestamp, String[] criticalSectionData) {

            this.logicalClocks.set(serverNumber - 1, Math.max(this.logicalClocks.get(serverNumber-1), timestamp));

            this.theater.theaterSeats.clear();

            for (int j = 0; j < criticalSectionData.length; j += 2) {

                int seat = Integer.parseInt(criticalSectionData[j]);
                String name = criticalSectionData[j+1];

                System.out.println("seat is: " + seat);
                System.out.println("name is: " + name);

                this.theater.theaterSeats.put(seat, name);
            }

            return this.theater.theaterSeats.toString();
        };

        public synchronized void sendRequestCriticalSection() {

            this.incrementAndGetLocalLogicalClock();

            int serverCounter = 0;
            for (int i = 0; i < this.servers.size(); i++) {
                try {
                    if (i != this.myID - 1) {
                        serverCounter = i;
                        ConnectionInfo serverInfo = this.servers.get(i);

                        TcpClient tcpClient = new TcpClient(serverInfo);
                        tcpClient.sendTCP(
                            "requestCriticalSection"
                            + " " + this.myID
                            + " " + this.logicalClocks.get(this.myID - 1)
                        );
                        tcpClient.close();

                    }
                } catch(Exception e) {
                    System.out.println("requestCriticalSection: server " + serverCounter + " is dead, setting status");
                    this.logicalClocks.set(serverCounter, Float.POSITIVE_INFINITY);
                    /*
                    ConnectionInfo serverConnectionInfo = this.servers.get(serverCounter);
                    serverConnectionInfo.setIsAlive(false);
                    this.servers.set(serverCounter, serverConnectionInfo);
                    */
                }

            }
            System.out.println("scen 3");
        }

        public synchronized void sendReleaseCriticalSection() {

            this.logicalClocks.set(this.myID-1, Float.POSITIVE_INFINITY);

            int serverCounter = 0;
            for (int i = 0; i < this.servers.size(); i++) {
                try {
                    if (i != this.myID - 1) {
                        serverCounter = i;
                        ConnectionInfo serverInfo = this.servers.get(i);

                        TcpClient tcpClient = new TcpClient(serverInfo);
                        tcpClient.sendTCP(
                            "releaseCriticalSection"
                            + " " + this.myID
                            + " " + this.logicalClocks.get(this.myID - 1)
                            + " " + theater.serializeTheaterData()
                        );
                        tcpClient.close();

                    }
                } catch(Exception e) {
                    System.out.println("sendReleaseCriticalSection: server " + serverCounter + " is dead, setting status");
                    this.logicalClocks.set(serverCounter, Float.POSITIVE_INFINITY);
                    /*
                    ConnectionInfo serverConnectionInfo = this.servers.get(serverCounter);
                    serverConnectionInfo.setIsAlive(false);
                    this.servers.set(serverCounter, serverConnectionInfo);
                    */
                }

            }
        }

        public synchronized Boolean canEnterCriticalSection() {

            boolean canEnter = true;

            float myTimestamp = this.logicalClocks.get(this.myID - 1);

            for (int i = 0; i < this.logicalClocks.size(); i++) {

                float otherTimestamp = this.logicalClocks.get(i);

                if (otherTimestamp != Float.POSITIVE_INFINITY) {

                    if (myTimestamp > otherTimestamp || ((myTimestamp == otherTimestamp) && this.myID-1 > i)) {
                        return false;
                    }
                }
            }

            System.out.println("canEnterCriticalSection is: " + canEnter + " and timestamps are: " + this.logicalClocks);

            // TODO: temp debug, REMOVE THIS
            //return false;

            return canEnter;
        }
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
                System.out.println("Unable to send TCP to remote server: " + this.clientSocket.getInetAddress());
            }

            return "";
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

        public synchronized String serializeTheaterData() {
            String output = "";
            for (int i = 1; i <= this.totalSeats; i++) {
                if (this.theaterSeats.containsKey(i) == true) {

                    if (i > 1) {
                        output += " ";
                    }

                    output += i
                           + " "
                           + this.theaterSeats.get(i)
                           ;
                }
            }

            System.out.println("serialized theater data is: " + output);
            return output;
        }
    }
}
