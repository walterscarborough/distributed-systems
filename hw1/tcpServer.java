
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class tcpServer extends Thread{
	Socket theClient;
	Theater theTheater;
	
	public tcpServer(Theater theater, Socket s)
	{
		theTheater = theater;
		theClient = s;
	}
	
	//run gets a tcp input through the .getInputStream
	//parses the input and returns the response through the println method
	public void run()
	{
		Scanner clientInput;
		try {
			clientInput = new Scanner(theClient.getInputStream());
		
			PrintWriter pout = new PrintWriter(theClient.getOutputStream());
			String command = clientInput.nextLine();
			Scanner st = new Scanner(command);          
			String tag = st.next();
			if (tag.equals("reserve")) {
				pout.println(theTheater.ReserveSeat(st.next()));
			} else if (tag.equals("bookSeat")) {
				pout.println(theTheater.ReserveSeatNum(st.next(), Integer.parseInt(st.next())));
			
			} else if (tag.equals("search")) {
				pout.println(theTheater.Search(st.next()));
			} else if (tag.equals("delete")) {
				pout.println(theTheater.ReleaseSeat(st.next()));
			}
			pout.flush();
			theClient.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
