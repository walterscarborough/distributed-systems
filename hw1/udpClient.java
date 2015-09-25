
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

//sends udp messages via datagram packets
public class udpClient {

	private InetAddress ia;
	private DatagramPacket sPacket, rPacket;
	private DatagramSocket datasocket; 
	private int port;
	private int len = 64;
	
	public udpClient(String hostname, int portNumber)
	{
		port = portNumber;
		try {
			ia = InetAddress.getByName(hostname);
			datasocket = new DatagramSocket();
		} catch (UnknownHostException e) {
			System.err.println(e);
		} catch (SocketException e) {
			System.err.println(e);
		}
	}
	
	public String sendUDP(String Message)
	{
		
		byte[] rbuffer = new byte[len];
		try
		{		
			byte[] buffer = new byte[Message.length()];
			buffer = Message.getBytes();
			sPacket = new DatagramPacket(buffer, buffer.length, ia, port);
			datasocket.send(sPacket);            	
			rPacket = new DatagramPacket(rbuffer, rbuffer.length);
			datasocket.receive(rPacket);
			String retstring = new String(rPacket.getData(), 0,
        			rPacket.getLength());
			return retstring;
		}
		catch (UnknownHostException e) {
	        System.err.println(e);
	    } catch (SocketException e) {
	        System.err.println(e);
	    } catch (IOException e) {
	         System.err.println(e);
	    }	
		return "error";
		
	}
	
	public void close()
	{
		datasocket.close();
	}
	
}
