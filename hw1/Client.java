
import java.util.Scanner;
//main class that sends either udp or tcp reserve commands
//and prints out the response
public class Client {
  public static void main (String[] args) {
    String hostAddress;
    int tcpPort;
    int udpPort;

    if (args.length != 3) {
      System.out.println("ERROR: Provide 3 arguments");
      System.out.println("\t(1) <hostAddress>: the address of the server");
      System.out.println("\t(2) <tcpPort>: the port number for TCP connection");
      System.out.println("\t(3) <udpPort>: the port number for UDP connection");
      System.exit(-1);
    }

    hostAddress = args[0];
    tcpPort = Integer.parseInt(args[1]);
    udpPort = Integer.parseInt(args[2]);


    //setup the sockets
	udpClient udpSocket = new udpClient(hostAddress, udpPort);
	
    
    
    Scanner sc = new Scanner(System.in);
    while(sc.hasNextLine()) {
      String cmd = sc.nextLine();
      String[] tokens = cmd.split(" ");
      tcpClient tcpSocket = new tcpClient(hostAddress, tcpPort);
      if (tokens[0].equals("reserve")) {
    	  if(tokens[2].equals("T")) {
    		  System.out.println(tcpSocket.sendTCP(cmd));
    	  } else if(tokens[2].equals("U")) {
    		  System.out.println(udpSocket.sendUDP(cmd));
    	  } else {
    		  System.out.println("Invalid socket type " + tokens[2]);
    	  }
      } else if (tokens[0].equals("bookSeat")) {
    	  if(tokens[3].equals("T")) {
    		  System.out.println(tcpSocket.sendTCP(cmd));
    	  } else if(tokens[3].equals("U")) {
    		  System.out.println(udpSocket.sendUDP(cmd));
    	  } else {
    		  System.out.println("Invalid socket type " + tokens[2]);
    	  }
      } else if (tokens[0].equals("search")) {
    	  if(tokens[2].equals("T")) {
    		  System.out.println(tcpSocket.sendTCP(cmd));
    	  } else if(tokens[2].equals("U")) {
    		  System.out.println(udpSocket.sendUDP(cmd));
    	  } else {
    		  System.out.println("Invalid socket type " + tokens[2]);
    	  }
      } else if (tokens[0].equals("delete")) {
    	  if(tokens[2].equals("T")) {
    		  System.out.println(tcpSocket.sendTCP(cmd));
    	  } else if(tokens[2].equals("U")) {
    		  System.out.println(udpSocket.sendUDP(cmd));
    	  } else {
    		  System.out.println("Invalid socket type " + tokens[2]);
    	  }
      } else {
        System.out.println("ERROR: No such command");
      }
      tcpSocket.close();
    }
  }
}
