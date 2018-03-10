import java.io.IOException;
import java.net.Socket;

public class ServerTable {
    public ServerEntry[] serverEntries = null;//index 0 is un-used
    public int myId;
    public int totalServerCount;
    SeatTable seatTable;
    boolean debug = Helper.debug;
    public ServerTable(int mId, int numServer, SeatTable seats){
        myId = mId;
        totalServerCount = numServer;
        serverEntries = new ServerEntry[numServer +1];
        seatTable = seats;
    }
    public synchronized int getNumActiveServer(){
        int count = 0;
        for (int i=1; i<= totalServerCount; i++){
            ServerEntry serverEntry = serverEntries[i];
            if(serverEntry.isUp)
                count++;
        }

        return count;
    }
    public synchronized int getClosestServerId(){
        for(int i = myId -1; i>=1; i--){
            if(serverEntries[i].isUp)
            {
                notifyAll();
                return i;
            }
        }
        for (int i= totalServerCount; i> myId; i--){
            if(serverEntries[i].isUp){
                notifyAll();
                return i;
            }

        }
        notifyAll();
        return -1;
    }
    // returns 0 if old value replaced, otherwise 1
    public synchronized void insert(int serverId, String ipColonPort) {
        String[] ipPort = ipColonPort.split(":");
        String ip = ipPort[0];
        int port = Integer.parseInt(ipPort[1]);

        //what if the server is crashed then start back up

        serverEntries[serverId] = new ServerEntry(myId, serverId,ip, port);
        notifyAll();
    }


    public synchronized void insert(int serverId, String ip, int port) {
        //what if the server is crashed then start back up

        serverEntries[serverId] = new ServerEntry(myId, serverId,ip, port);
        notifyAll();

    }
    public synchronized void insert(int serverId, String ip, int port, Socket s, LamportMutex lock) throws IOException {
        //what if the server is crashed then start back up

        serverEntries[serverId] = new ServerEntry(myId, serverId,ip, port, s);
        Thread t = new SocketThread(myId, serverId, lock, this, seatTable);
        t.start();

        notifyAll();

    }
    public synchronized ServerEntry getServerById(int serverId){
        ServerEntry server = serverEntries[serverId];
        notifyAll();
        return server;
    }

    public synchronized void closeSocketByServerId(int id) throws IOException {
        serverEntries[id].socket.close();
        serverEntries[id].isUp = false;
        if(debug){
            System.out.println("ServerTable - closeSocketByServerId: " + print());
        }
    }
    private String print(){
        String result = myId + " | ";
        for(int i = 1; i<= totalServerCount; i++){
            ServerEntry serverEntry = serverEntries[i];
            result += serverEntry.serverId + " | " + serverEntry.ipAddress + " | " + (serverEntry.isUp? "true": "false") + " | ";
        }
        return result;
    }
}
