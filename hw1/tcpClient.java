
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

//sends tcp messages through the streams
public class tcpClient {
	

	Socket clientSocket;
	PrintWriter out;
	BufferedReader in;
	
	public tcpClient(String hostname, int port)
	{
		
		try {
			clientSocket = new Socket(hostname, port);
            // HW 2 requirement: sockets must timeout within 100ms
            clientSocket.setSoTimeout(100);
			out = new PrintWriter(clientSocket.getOutputStream(), true);
		    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {
			System.err.println(e);
		}
	}
	
	public String sendTCP(String Message)
	{
		try {
			out.println(Message);
			return in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "error";
		
	}
	
	public void close()
	{
		try {
			out.close();
			in.close();
			clientSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println(e);
		}
	}

}
