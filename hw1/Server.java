
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

//main method starts threads to receive udp and tcp reservation requests
public class Server {
  public static void main (String[] args) {
    int N;
    int tcpPort;
    int udpPort;
    if (args.length != 3) {
      System.out.println("ERROR: Provide 3 arguments");
      System.out.println("\t(1) <N>: the total number of available seats");
      System.out.println("\t\t\tassume the seat numbers are from 1 to N");
      System.out.println("\t(2) <tcpPort>: the port number for TCP connection");
      System.out.println("\t(3) <udpPort>: the port number for UDP connection");

      System.exit(-1);
    }
    N = Integer.parseInt(args[0]);
    tcpPort = Integer.parseInt(args[1]);
    udpPort = Integer.parseInt(args[2]);

    // TODO: handle request from clients
    //initialize theater with N seats
    Theater theTheater = new Theater(N);
	
    //setup the tcp server socket

	
	//setup the udp socket
	DatagramSocket s;
	try {
		s = new DatagramSocket(udpPort);
	
		Thread u = new Thread(new udpServer(s, theTheater));
		//start a thread to catch udp client connections
		u.start();
	} catch (SocketException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	while(true)
	{
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(tcpPort);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		try {
	        Socket clientSocket = serverSocket.accept();
	        Thread t = new Thread(new tcpServer(theTheater, clientSocket));
	        //start a tcp thread to handle incoming connection
	        t.start();
	        serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//serverSocket.close();
    
    
  }
}
