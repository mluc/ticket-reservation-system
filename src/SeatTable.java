import java.util.*;

public class SeatTable {
    private int _totalSeat = -1;
    private Hashtable<Integer, String> _seatNumMapName = new Hashtable<>();

    public SeatTable(){

    }
    public synchronized void setTotalSeat(int seatNum){
        _totalSeat = seatNum;
    }
    public synchronized int getTotalSeat(){
        return _totalSeat;
    }
    public synchronized void updateSeats(String msg){
        //msg: list |5|2 Mary|1 John
        String[] seats = msg.replace("|","\t").split("\t");
        _totalSeat = Integer.parseInt(seats[1]);
        _seatNumMapName.clear();
        for(int i=2; i< seats.length; i++){
            String[] seatNumToName = seats[i].trim().split(" ");
            bookSeat(seatNumToName[1], Integer.parseInt(seatNumToName[0]));
        }

        notifyAll();
    }
    public synchronized String reserveSeat(String name){
        String result = null;
        //completely booked
        if(_seatNumMapName.size() == _totalSeat){
            result ="Sold out - No seat available";
        }
        else if(_seatNumMapName.values().contains(name.trim())){
            result = "Seat already booked against the name provided";
        }
        else {
            int newSeatNum = 0;
            Set<Integer> takenSeats = _seatNumMapName.keySet();
            //find an available seat
            for(int i = 1; i<= _totalSeat; i++){
                if(!takenSeats.contains(i)){
                    newSeatNum = i;
                    break;
                }
            }
            _seatNumMapName.put(newSeatNum, name);
            result = "Seat assigned to you is "+ newSeatNum;
        }
        notifyAll();
        return result;
    }
    public synchronized String bookSeat(String name, int seatNum){
        String result = null;
        //completely booked
        if(_seatNumMapName.size() == _totalSeat){
            result ="Sold out - No seat available";
        }
        else if(_seatNumMapName.values().contains(name.trim())){
            result = "Seat already booked against the name provided";
        }
        else {
            //check if seatNum is available
            boolean requestedSeatAvailable = !_seatNumMapName.containsKey(seatNum);
            if(!requestedSeatAvailable){
                result = seatNum + "  is not available";
            }
            else {
                _seatNumMapName.put(seatNum, name);
                result = "Seat assigned to you is "+ seatNum;
            }
        }
        notifyAll();
        return result;
    }
    public synchronized String searchSeatNumByName(String name){
        String result = null;
        if(_seatNumMapName.containsValue(name)){
            int seatNum=0;
            for(Map.Entry<Integer, String> entry : _seatNumMapName.entrySet()){
                if(entry.getValue().equals(name)){
                    seatNum = entry.getKey();
                    break;
                }
            }
            result = ""+seatNum;
        }
        else{
            result = "No reservation found for " + name;
        }

        notifyAll();
        return result;
    }
    public synchronized String deleteSeatByName(String name){
        String result = null;
        if(_seatNumMapName.containsValue(name)){
           int deleteSeat=0;
           for(Map.Entry<Integer, String> entry : _seatNumMapName.entrySet()){
               if(entry.getValue().equals(name)){
                   deleteSeat = entry.getKey();
                   break;
               }
           }
           _seatNumMapName.remove(deleteSeat);
           result = ""+deleteSeat;
        }
        else {
            result = "No reservation found for " +name;
        }

        notifyAll();
        return result;
    }

    public synchronized String getSeats(){
        String result = "list |"+_totalSeat +"|";
        for(Map.Entry<Integer, String> entry : _seatNumMapName.entrySet()){
            result += entry.getKey() + " " + entry.getValue() + "|";
        }
        notifyAll();
        return result;
    }
}
