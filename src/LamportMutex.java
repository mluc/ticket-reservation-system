import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

public class LamportMutex {
    LamportClock c;
    int numAcks;
    Queue<Timestamp> q; // request queue
    ServerTable serverTable;

    int myId;
    boolean debug = Helper.debug;
    public LamportMutex(int mId, ServerTable st) {
        myId = mId;
        serverTable = st;
        c = new LamportClock();
        q = new PriorityQueue<Timestamp>(new Comparator<Timestamp>() {
                    public int compare(Timestamp a, Timestamp b) {
                        return Timestamp.compare(a, b);
                    }
                });
        numAcks = 0;
    }

    public synchronized void requestCS() {
        if(debug) {
            System.out.println("LamportMutex - requestCS by " + myId + ". Number of active server is " + serverTable.getNumActiveServer());
        }
        c.tick();
        q.add(new Timestamp(c.getValue(), serverTable.myId));
        sendMsgToNeighbors("request", c.getValue());
        numAcks = 0;
        while ((q.peek().pid != myId) || (numAcks < serverTable.getNumActiveServer() - 1))
        {
            try {wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        if(debug){
            System.out.println("LamportMutex - requestCS numAcks: "+ numAcks);
        }
    }

    public synchronized void releaseCS() {
        if(debug) System.out.println("LamportMutex - releaseCS by " + myId);
        q.remove();
        sendMsgToNeighbors("release", c.getValue());
    }

    public synchronized void handleMsg(MyMsg m, int src, String tag) {
        if (debug) System.out.println("LamportMutex - handleMsg: " + m.toString());
        int timeStamp = m.getMessageInt();
        c.receiveAction(src, timeStamp);
        if (tag.equals("request")) {
            q.add(new Timestamp(timeStamp, src));
            sendMsg(src, "ack", c.getValue());
        } else if (tag.equals("release")) {

            Iterator<Timestamp> it = q.iterator();
            while (it.hasNext()) {
                if (it.next().getPid() == src) it.remove();
            }
        } else if (tag.equals("ack"))
            numAcks++;
        notifyAll();
    }

    private void sendMsgToNeighbors(String request, int value) {
        if (debug) System.out.println("LamportMutex - sendMsgToNeighbors");
        for(int i = 1; i<=serverTable.totalServerCount; i++){
            ServerEntry serverEntry = serverTable.serverEntries[i];
            if(serverEntry.serverId == myId || !serverEntry.isUp)
                continue;
            sendMsg(serverEntry.serverId, request, value);
        }
    }
    private void sendMsg(int src, String request, int value) {
        if (debug) System.out.println("LamportMutex - sendMsg from " + myId + " to " + src + " : " + request + " " + value);
        Helper.tcpSend(serverTable.serverEntries[src].dataOutputStream, request + " " + value );
    }


}
