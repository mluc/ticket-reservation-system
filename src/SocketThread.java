import java.io.IOException;

public class SocketThread extends Thread{
    ServerTable serverTable;
    SeatTable seatTable;
    int myId;
    int serverId;
    LamportMutex lock;
    boolean debug = Helper.debug;
    public SocketThread(int mid, int sId, LamportMutex l, ServerTable st, SeatTable seats){
        serverTable = st;
        seatTable = seats;
        myId = mid;
        serverId = sId;
        lock = l;
    }
    public void run(){
        try {
            ServerEntry serverEntry = serverTable.serverEntries[serverId];
            Helper.tcpSend(serverEntry.dataOutputStream, myId+"");
            if(debug) System.out.println("SocketThread - sending server " + serverId + " my Id which is " + myId);
            String serverMsg;
            while ((serverMsg = serverEntry.bufferedReaderIn.readLine()) != null) {

                if(debug) System.out.println("SocketThread - received from server " + serverId + " :" + serverMsg);

                String[] tokens = serverMsg.trim().split(" ");
                String cmd = tokens[0].trim();
                if(cmd.equals("request")){
                    if(debug) System.out.println("SocketThread - received request from server " + serverId);
                    MyMsg myMsg = new MyMsg(serverId, myId,serverMsg);
                    lock.handleMsg(myMsg, serverId, "request");
                }
                else if(cmd.equals("ack")){
                    if(debug) System.out.println("SocketThread - received ack from server " + serverId);
                    MyMsg myMsg = new MyMsg(serverId, myId,serverMsg);
                    lock.handleMsg(myMsg, serverId, "ack");
                }
                else if(cmd.equals("release")){
                    if(debug) System.out.println("SocketThread - received release from server " + serverId);
                    MyMsg myMsg = new MyMsg(serverId, myId,serverMsg);
                    lock.handleMsg(myMsg, serverId, "release");
                }
                else if(cmd.equals("list")){
                    if(debug) System.out.println("SocketThread - received list from server " + serverId + " with seat table result " + serverMsg + ". Updating my seat result accordingly.");
                    //result of seat table
                    seatTable.updateSeats(serverMsg);
                }
                else if(cmd.equals("reserveSync")){
                    if(debug) System.out.println("SocketThread - received reserveSync from server " + serverId + ". Updating my seat result accordingly.");
                    String name = tokens[1].trim();
                    seatTable.reserveSeat(name);
                }
                else if(cmd.equals("bookSeatSync")){
                    if(debug) System.out.println("SocketThread - received bookSeatSync from server " + serverId + ". Updating my seat result accordingly.");
                    String name = tokens[1].trim();
                    String seatNum = tokens[2].trim();
                    seatTable.bookSeat(name, Integer.parseInt(seatNum));
                }
                else if(cmd.equals("deleteSync")){
                    if(debug) System.out.println("SocketThread - received deleteSync from server " + serverId + ". Updating my seat result accordingly.");
                    String name = tokens[1].trim();
                    seatTable.deleteSeatByName(name);
                }
                else if(cmd.equals("downSync")){
                    if(debug) System.out.println("SocketThread - received downSync from server " + serverId + ". Updating my seat result accordingly.");
                    int downServerId = Integer.parseInt(tokens[1]);
                    serverTable.closeSocketByServerId(downServerId);
                }
                else {
                    System.out.println("Invalid command");
                }

            }
        } catch (IOException e) {
            if(debug){
                System.err.println(e);
                System.out.println("SocketThread - server with id " + serverId + " is disconnected. Closing its socket");
            }
            try {
                serverTable.closeSocketByServerId(serverId);
                lock.requestCS();

                String syncSeatTable = "downSync "+ serverId;
                for(int i = 1; i<=serverTable.totalServerCount; i++){
                    ServerEntry serverEntry = serverTable.serverEntries[i];
                    if(serverEntry.serverId == myId || !serverEntry.isUp)
                        continue;
                    //if(serverEntry.serverId == serverId)
                    //    continue;
                    Helper.tcpSend(serverTable.serverEntries[serverEntry.serverId].dataOutputStream, syncSeatTable);
                }

                lock.releaseCS();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
