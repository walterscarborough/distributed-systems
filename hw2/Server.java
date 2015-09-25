import java.util.Scanner;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    public static class ConnectionInfo {
        private String ip = "";
        private int port = 0;

        public ConnectionInfo() {}

        public ConnectionInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
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

        System.out.println("myId is: " + myID);
        System.out.println("numServerm is: " + numServer);
        System.out.println("numSeat is: " + numSeat);

        ArrayList<ConnectionInfo> servers = new ArrayList<ConnectionInfo>();

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

        while(true) {
            System.out.println("loop iterate!");
            // TODO: communicate w/ other servers
            // keyword: synchronize?

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
                Thread clientHandlerThread = new Thread(new ClientHandler(clientSocket, theater));

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

        public ClientHandler(Socket clientSocket, Theater theater) {
            this.clientSocket = clientSocket;
            this.theater = theater;
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
                        break;

                    case "delete":
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
            if (theaterSeats.containsValue(name) == true) {
                return "Seat already booked against the name provided";
            }

            // Step 3 - ok to reserve next available
            String outputMessage = "";
            for (int i = 1; i <= this.totalSeats; i++) {
                 if (theaterSeats.containsKey(i) == false) {
                    theaterSeats.put(i, name);
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
            if (theaterSeats.containsValue(name) == true) {
                return "Seat already booked against the name provided";
            } 

            // Step 3 - check if this seat already has a reservation
            if (theaterSeats.containsKey(seatNum) == true) {
                return seatNum + " is not available";
            }

            theaterSeats.put(seatNum, name);
            return "Seat assigned to you is " + seatNum;
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
