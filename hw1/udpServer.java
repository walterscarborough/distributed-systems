
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class udpServer extends Thread{
	private DatagramPacket sPacket, rPacket;
	private DatagramSocket datasocket; 
	private int len = 64;
	Theater theTheater;
	public udpServer(DatagramSocket socket, Theater theater)
	{
		
		theTheater = theater;
		datasocket = socket;
		
	}
	
	//run starts an infinite while loop that receives udp messages and
	//parses the input and returns the results of the input command
	public void run()
	{
				
		try {
			//setup the read buffer
			byte[] buf = new byte[len];
			//this thread runs until forcibly terminated
			while(true) {
				rPacket = new DatagramPacket(buf, buf.length);
				//wait till a message is received
				datasocket.receive(rPacket);
				//get the command
				String inputString = new String(rPacket.getData(), 0,
	        			rPacket.getLength());
				String[] inputArgs = inputString.split(" ");
				//parse the command
				String cmd = inputArgs[0];
				String name = inputArgs[1];
				String Message = "";
				
				//the theater class returns the appropriate string response
				if (cmd.equals("reserve")) {
					Message = theTheater.ReserveSeat(name);
				} else if (cmd.equals("bookSeat")) {
					Message = theTheater.ReserveSeatNum(name, Integer.parseInt(inputArgs[2]));
					
				} else if (cmd.equals("search")) {
					Message = theTheater.Search(name);
				} else if (cmd.equals("delete")) {
					Message = theTheater.ReleaseSeat(name);
				}
				
				//convert the return string to a byte array 
				byte[] outBuffer = new byte[Message.length()];
				outBuffer = Message.getBytes();
				//setup the datagram packet and send				
				sPacket = new DatagramPacket(
						outBuffer,
						outBuffer.length,
						rPacket.getAddress(),
						rPacket.getPort());
				datasocket.send(sPacket);
			}
		} catch (SocketException e) {
			System.err.println(e);
		} catch (IOException e) {
			System.err.println(e);
		}
		
		
	}	
}
