import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerSocketAcceptThread extends Thread{

    private SeatTable seatTable;
    private ServerTable serverTable;
    private Socket socket;
    private LamportMutex lock;
    private int myId;
    private int serverId = -1;
    private boolean debug = Helper.debug;
    public ServerSocketAcceptThread(int mId, Socket s, SeatTable seats, ServerTable servers, LamportMutex l){
        socket = s;
        serverTable = servers;
        seatTable = seats;
        lock = l;
        myId = mId;
    }
    public void run() {
        String serverMsg;

        BufferedReader inBuff = null;
        DataOutputStream outStream = null;
        try {
            inBuff = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            while ((serverMsg = inBuff.readLine()) != null) {
                if(debug) System.out.println("ServerSocketAcceptThread - recieved: " + serverMsg);

                int senderId = -1;
                try{
                    senderId = Integer.parseInt(serverMsg.trim());
                }catch (Exception ex){

                }
                if(senderId != -1){
                    if(debug) System.out.println("ServerSocketAcceptThread - from server id: " + senderId + ". Updating server table entry of server id " + senderId);
                    serverId = senderId;
                    ServerEntry serverEntry = serverTable.serverEntries[senderId];
                    serverEntry.socket = socket;
                    serverEntry.bufferedReaderIn = inBuff;
                    serverEntry.dataOutputStream = outStream;
                    serverEntry.isUp = true;

                }
                else{
                    //some actions
                    String[] tokens = serverMsg.trim().split(" ");
                    String cmd = tokens[0].trim();
                    String msg = null;
                    if(cmd.equals("reserve")){
                        if(debug) System.out.println("ServerSocketAcceptThread - reserve from Client");
                        String name = tokens[1].trim();
                        if(serverTable.getNumActiveServer() > 1){
                            if(debug) System.out.println("ServerSocketAcceptThread - need to update seat table");
                            lock.requestCS();
                            if(debug) System.out.println("ServerSocketAcceptThread - in CS");
                            msg = seatTable.reserveSeat(name);
                            boolean success = msg.startsWith("Seat assigned to you is");
                            if(success){
                                //notify other servers
                                String syncSeatTable = serverMsg.replace("reserve", "reserveSync");

                                for(int i = 1; i<=serverTable.totalServerCount; i++){
                                    ServerEntry serverEntry = serverTable.serverEntries[i];
                                    if(serverEntry.serverId == myId || !serverEntry.isUp)
                                        continue;
                                    Helper.tcpSend(serverTable.serverEntries[serverEntry.serverId].dataOutputStream, syncSeatTable);
                                }
                            }
                            lock.releaseCS();
                            if(debug) System.out.println("ServerSocketAcceptThread - released CS");
                        }
                        else{
                            msg = seatTable.reserveSeat(name);
                        }

                        Helper.tcpSend(outStream, msg);
                    }
                    else if(cmd.equals("bookSeat")){
                        if(debug) System.out.println("ServerSocketAcceptThread - bookSeat from Client");
                        String name = tokens[1].trim();
                        String seatNum = tokens[2].trim();
                        if(serverTable.getNumActiveServer() > 1){
                            if(debug) System.out.println("ServerSocketAcceptThread - need to update seat table");
                            lock.requestCS();
                            if(debug) System.out.println("ServerSocketAcceptThread - in CS");
                            msg = seatTable.bookSeat(name, Integer.parseInt(seatNum));
                            boolean success = msg.startsWith("Seat assigned to you is");
                            if(success){
                                //notify other servers
                                String syncSeatTable = serverMsg.replace("bookSeat", "bookSeatSync");
                                for(int i = 1; i<=serverTable.totalServerCount; i++){
                                    ServerEntry serverEntry = serverTable.serverEntries[i];
                                    if(serverEntry.serverId == myId || !serverEntry.isUp)
                                        continue;
                                    Helper.tcpSend(serverTable.serverEntries[serverEntry.serverId].dataOutputStream, syncSeatTable);
                                }
                            }

                            lock.releaseCS();
                            if(debug) System.out.println("ServerSocketAcceptThread - released CS");
                        }else {
                            msg = seatTable.bookSeat(name, Integer.parseInt(seatNum));
                        }

                        Helper.tcpSend(outStream, msg);
                    }
                    else if(cmd.equals("search")){
                        if(debug) System.out.println("ServerSocketAcceptThread - search from Client");
                        String name = tokens[1].trim();
                        msg = seatTable.searchSeatNumByName(name);
                        Helper.tcpSend(outStream, msg);
                    }
                    else if(cmd.equals("delete")){
                        if(debug) System.out.println("ServerSocketAcceptThread - delete from Client");
                        String name = tokens[1].trim();
                        if(serverTable.getNumActiveServer() > 1){
                            if(debug) System.out.println("ServerSocketAcceptThread - need to update seat table");
                            lock.requestCS();
                            if(debug) System.out.println("ServerSocketAcceptThread - in CS");
                            msg = seatTable.deleteSeatByName(name);
                            boolean success = !msg.startsWith("No reservation found for");
                            if(success){
                                //notify other servers
                                String syncSeatTable = serverMsg.replace("delete", "deleteSync");
                                for(int i = 1; i<=serverTable.totalServerCount; i++){
                                    ServerEntry serverEntry = serverTable.serverEntries[i];
                                    if(serverEntry.serverId == myId || !serverEntry.isUp)
                                        continue;
                                    Helper.tcpSend(serverTable.serverEntries[serverEntry.serverId].dataOutputStream, syncSeatTable);
                                }
                            }
                            lock.releaseCS();
                            if(debug) System.out.println("ServerSocketAcceptThread - released CS");
                        }else {
                            msg = seatTable.deleteSeatByName(name);
                        }

                        Helper.tcpSend(outStream, msg);
                    }
                    else if(cmd.equals("request")){
                        if(debug) System.out.println("ServerSocketAcceptThread - request from Server " + serverId);
                        MyMsg myMsg = new MyMsg(serverId, myId,serverMsg);
                        lock.handleMsg(myMsg, serverId, "request");
                    }
                    else if(cmd.equals("ack")){
                        if(debug) System.out.println("ServerSocketAcceptThread - ack from Server " + serverId);
                        MyMsg myMsg = new MyMsg(serverId, myId,serverMsg);
                        lock.handleMsg(myMsg, serverId, "ack");
                    }
                    else if(cmd.equals("release")){
                        if(debug) System.out.println("ServerSocketAcceptThread - release from Server " + serverId);
                        MyMsg myMsg = new MyMsg(serverId, myId,serverMsg);
                        lock.handleMsg(myMsg, serverId, "release");
                    }
                    else if(cmd.equals("list")){
                        if(debug) System.out.println("ServerSocketAcceptThread - list from Server " + serverId);
                        msg = seatTable.getSeats();

                        Helper.tcpSend(outStream, msg);
                    }
                    else if(cmd.equals("reserveSync")){
                        if(debug) System.out.println("ServerSocketAcceptThread - reserveSync from Server");
                        String name = tokens[1].trim();
                        seatTable.reserveSeat(name);
                    }
                    else if(cmd.equals("bookSeatSync")){
                        if(debug) System.out.println("ServerSocketAcceptThread - bookSeatSync from Server " + serverId);
                        String name = tokens[1].trim();
                        String seatNum = tokens[2].trim();
                        seatTable.bookSeat(name, Integer.parseInt(seatNum));
                    }
                    else if(cmd.equals("deleteSync")){
                        if(debug) System.out.println("ServerSocketAcceptThread - deleteSync from Server " + serverId);
                        String name = tokens[1].trim();
                        seatTable.deleteSeatByName(name);
                    }
                    else if(cmd.equals("downSync")){
                        if(debug) System.out.println("ServerSocketAcceptThread - received downSync from server " + serverId + ". Updating my seat result accordingly.");
                        int downServerId = Integer.parseInt(tokens[1]);
                        serverTable.closeSocketByServerId(downServerId);
                    }
                    else {
                        System.out.println("Invalid command");
                    }
                }
            }
        } catch (IOException e) {
            if(debug){
                e.printStackTrace();
                System.out.println("ServerSocketAcceptThread - server with id " + serverId + " is disconnected. Closing its socket");
            }
            try {
                boolean isClientThread = serverId == -1;
                if(!isClientThread){
                    serverTable.closeSocketByServerId(serverId);
                    lock.requestCS();

                    String syncSeatTable = "downSync "+ serverId;
                    for(int i = 1; i<=serverTable.totalServerCount; i++){
                        ServerEntry serverEntry = serverTable.serverEntries[i];
                        if(serverEntry.serverId == myId || !serverEntry.isUp)
                            continue;
                        //if(serverEntry.serverId == serverId)
                        //   continue;
                        Helper.tcpSend(serverTable.serverEntries[serverEntry.serverId].dataOutputStream, syncSeatTable);
                    }

                    lock.releaseCS();
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
