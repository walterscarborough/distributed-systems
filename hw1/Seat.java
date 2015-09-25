
//Class Seat aggregates seatNumber, reservation Name, reserved boolean
//Methods to reserve and release seat
public class Seat {
	
	int SeatNumber;
	public int getSeatNumber() {
		return SeatNumber;
	}

	String Name;
	public String getName() {
		return Name;
	}

	boolean Reserved;
	public boolean isReserved() {
		return Reserved;
	}

	public int reserveSeat(String resName) throws IllegalArgumentException
	{
		if(resName == null || resName == "")
		{
			throw new IllegalArgumentException();
		}
		if(Reserved == false)
		{
			Reserved = true;
			Name = resName;
			return SeatNumber;
		}
		return -1;
		
	}
	
	public Seat(int seatNumber) {
		super();
		SeatNumber = seatNumber;
		Name = "";
		Reserved = false;
	}

	public int releaseSeat()
	{
		if(Reserved == true)
		{
			Name = "";
			Reserved = false;
			return SeatNumber;
		}
		return -1;
	}

}
