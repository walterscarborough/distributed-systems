import java.util.Scanner;
import java.util.ArrayList;
import java.net.Socket;
import java.net.SocketException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Client {

        public static class ConnectionInfo {
        private String ip = "";
        private int port = 0;

        public ConnectionInfo() {}

        public ConnectionInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public void setIp(String ip) {
            this.ip = ip;                                                             }

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
        int numServer = sc.nextInt();

        ArrayList<ConnectionInfo> servers = new ArrayList<ConnectionInfo>();

        sc.nextLine(); // Consume newline for scanner.

        for (int i = 0; i < numServer; i++) {
            // TODO: refactor split into ConnectionInfo class
            try {
                String otherServerInfo = sc.nextLine();
                String[] split = otherServerInfo.split(":");

                String ip = split[0];
                int port = Integer.parseInt(split[1]);

                ConnectionInfo otherServer = new ConnectionInfo(ip, port);

                //System.out.println("new input received");
                servers.add(otherServer);
            }
            catch(Exception e) {
                //System.out.println("Warning: unable to parse server input");
            }
        }

        while(sc.hasNextLine()) {
            String cmd = sc.nextLine();
            String[] tokens = cmd.split(" ");


            if (tokens[0].equals("reserve")) {
                TcpClient tcpClient = new TcpClient(servers);
                System.out.println(tcpClient.sendTCP(cmd));
                tcpClient.close();
            }
            else if (tokens[0].equals("bookSeat")) {
                TcpClient tcpClient = new TcpClient(servers);
                System.out.println(tcpClient.sendTCP(cmd));
                tcpClient.close();
            }
            else if (tokens[0].equals("search")) {
                TcpClient tcpClient = new TcpClient(servers);
                System.out.println(tcpClient.sendTCP(cmd));
                tcpClient.close();
            }
            else if (tokens[0].equals("delete")) {
                TcpClient tcpClient = new TcpClient(servers);
                System.out.println(tcpClient.sendTCP(cmd));
                tcpClient.close();
            }
            else {
                System.out.println("ERROR: No such command");
            }
        }
    }

    public static class TcpClient {

        Socket clientSocket;
        PrintWriter out;
        BufferedReader in;

        ArrayList<ConnectionInfo> servers = new ArrayList<ConnectionInfo>();

        //ConnectionInfo selectedServer;

        public TcpClient(ArrayList<ConnectionInfo> servers) {

            this.servers = servers;
            int serverCounter = 0;

            for (int i = 0; i < servers.size(); i++)  {
                try {
                    serverCounter = i;
                    // Loop through servers in order
                    ConnectionInfo server = this.servers.get(i);

                    this.clientSocket = new Socket(server.getIp(), server.getPort());
                    this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
                    this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

                    break;
                }
                catch (Exception e) {
                    //System.err.println(e);
                    //System.out.println("Server " + serverCounter + " unavailable, trying next server...");
                }
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
}
