
import java.util.ArrayList;

import javax.print.DocFlavor.STRING;

//Theater class holds an arraylist of seats
//Theater class is thread safe
//public methods to reserve, release and search for seats
public class Theater {
	int TotalSeats;
	public int getTotalSeats() {
		return TotalSeats;
	}
	
	ArrayList <Seat> seats = new ArrayList<Seat>();
	
	public Theater(int totalSeats) {
		super();
		TotalSeats = totalSeats;
		Seat tempSeat;
		for(int i = 1; i <= totalSeats; i++)
		{
			tempSeat = new Seat(i);
			seats.add(tempSeat);
		}
		
	}

	private Seat getNextAvailableSeat()
	{
		for(Seat currSeat : seats)
		{
			if(!currSeat.isReserved())
				return currSeat;
				
		}
		return null;
	}
	
	private Seat findByName(String Name)
	{
		for(Seat currSeat : seats)
		{
			if(currSeat.getName().equals(Name))
				return currSeat;
				
		}
		return null;
	}
	
	private Seat findBySeat(int seatNumber)
	{
		for(Seat currSeat : seats)
		{
			if(currSeat.getSeatNumber() == seatNumber)
				return currSeat;
				
		}
		return null;
	}
	
	
	
	
	public synchronized String ReserveSeat(String Name)
	{ 
		Seat assignSeat =  getNextAvailableSeat();
		Seat tmpSeat;
		if(assignSeat != null)
		{
		tmpSeat = findByName(Name);
		
		if(tmpSeat == null)
		{
			
				assignSeat.reserveSeat(Name);
				return "Seat assigned to you is " + assignSeat.getSeatNumber();
			
		}
		else
		{
			return "Seat already booked against the name provided.";
		}
		
		}
		else
		{
			return "Sold out - No seat avaialable.";
		}
	}
	
	public synchronized String ReserveSeatNum(String Name, int seatNum)
	{
		if(seatNum > TotalSeats || seatNum < 1)
		{
			return seatNum + " is not available.";
		}
		Seat tmpSeat =  getNextAvailableSeat();
		if(tmpSeat != null)
		{
		tmpSeat = findByName(Name);
		
		if(tmpSeat == null)
		{
			tmpSeat = findBySeat(seatNum);
			if(tmpSeat.isReserved())
			{
				return seatNum + " is not available.";
			}
			else
			{
				tmpSeat.reserveSeat(Name);
				return "Seat assigned to you is " + tmpSeat.getSeatNumber();
			}
		}
		else
		{
			return "Seat already booked against the name provided.";
		}
		
		}
		else
		{
			return "Sold out - No seat avaialable.";
		}
	}
	
	public synchronized String ReleaseSeat(String Name)
	{
		Seat tmpSeat = this.findByName(Name);
		if(tmpSeat != null)
		{
			return "" + tmpSeat.releaseSeat();
			
		}
		else
		{
			return "No reservation found for " + Name;
		}
		
	}
	
	public synchronized String Search(String Name)
	{
		Seat tmpSeat = this.findByName(Name);
		if(tmpSeat != null)
		{
			return "" + tmpSeat.getSeatNumber();
			
		}
		else
		{
			return "No reservation found for " + Name;
		}
		
	}
	
	
}
