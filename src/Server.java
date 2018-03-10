import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    public static void main (String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);
        int myID = sc.nextInt();
        int numServer = sc.nextInt();
        int numSeat = sc.nextInt();
        boolean debug = Helper.debug;

        SeatTable seatTable = new SeatTable();

        boolean isFirstStarted = true;
        //loop through all servers to see which one responses
        ServerTable serverTable = new ServerTable(myID, numServer, seatTable);
        LamportMutex lock = new LamportMutex(myID, serverTable);

        for (int id = 1; id <= numServer; id++) {
            String ipColonPort = sc.next().trim();
            String[] ipPort = ipColonPort.split(":");
            String ip = ipPort[0];
            int port = Integer.parseInt(ipPort[1]);

            //skip myID
            if(id == myID){
                serverTable.insert(id, ip, port);
                continue;
            }

            Socket s = new Socket();
            try {
                s.connect(new InetSocketAddress(ip, port), 100);
                serverTable.insert(id, ip, port, s, lock);
                isFirstStarted = false;
                if(debug) System.out.println("Server - My Id is " + myID + ". I am able to connect to " + id + ". A SocketThread is started.");

            } catch (IOException e) {
                serverTable.insert(id, ip, port);
                if(debug) System.out.println("Server - My Id is " + myID + ". I cannot connect to " + id);
            }
        }

        Runnable tcp = () -> {
            ServerSocket tcpSocket = null;
            try {
                int myPort = serverTable.getServerById(myID).port;
                tcpSocket = new ServerSocket(myPort);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Socket s;
            try {
                while ((s = tcpSocket.accept()) != null) {

                    Thread t = new ServerSocketAcceptThread(myID, s, seatTable, serverTable, lock);
                    t.start();

                    if(debug) System.out.println("Server - My Id is " + myID + ". A ServerSocketAcceptThread is started. It can be from server or client.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        new Thread(tcp).start();

        if(isFirstStarted){
            seatTable.setTotalSeat(numSeat);
            if(debug) System.out.println("Server - My Id is " + myID + ". I am the first server started up.");
        }
        else {
            if(debug) System.out.println("Server - My Id is " + myID + ". I am not the first server, go ask the server closest to me about seat table info");
            int closestServerId = serverTable.getClosestServerId();
            lock.requestCS();
            if(debug) System.out.println("Server - My Id is " + myID + ". I am in CS asking for seat table info.");
            Helper.tcpSend(serverTable.serverEntries[closestServerId].dataOutputStream, "list");
            while (seatTable.getTotalSeat() < 0){

            }
            lock.releaseCS();
            if(debug) System.out.println("Server - My Id is " + myID + ". I released CS asking for seat table info.");
        }


    }
}