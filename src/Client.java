import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Socket _tcpSocket = null;
    private static DataOutputStream _tcpOutToServer = null;
    private static BufferedReader _tcpInFromServer = null;
    private static boolean debug = Helper.debug;
    private static int connectToPort;

    // line 1: n:  the number of servers present
    //next n lines: <ip-address>:<port-number> per line about n servers
    public static void main (String[] args) throws IOException{

        Scanner sc = new Scanner(System.in);
        int numServer = sc.nextInt();

        ClientServerTable serverTable = new ClientServerTable();
        for (int id = 1; id <= numServer; id++) {
            String ipColonPort = sc.next().trim();
            serverTable.insert(id, ipColonPort);
        }

        for(int ind = 0; ind<numServer; ind++){
            //setup connection with the first closet server
            Pair<String, Integer> closestServer = serverTable.getServerByIndex(ind);
            boolean connectionEstablished = setupTcpProtocol(closestServer.getKey(), closestServer.getValue());
            if(connectionEstablished)
                break;
        }

        while(sc.hasNextLine()) {
            String cmd = sc.nextLine();
            if(cmd.trim().length()==0){
                continue;
            }
            String[] tokens = cmd.split(" ");

            if (tokens[0].equals("reserve") || tokens[0].equals("bookSeat") || tokens[0].equals("search") || tokens[0].equals("delete") || tokens[0].equals("list")) {
                boolean success = Helper.tcpSend(_tcpOutToServer, cmd);
                while (!success){
                    //connect to another available server
                    for(int ind = 0; ind<numServer; ind++){
                        //setup connection with the first closet server
                        Pair<String, Integer> closestServer = serverTable.getServerByIndex(ind);
                        boolean connectionEstablished = setupTcpProtocol(closestServer.getKey(), closestServer.getValue());
                        if(connectionEstablished)
                            break;
                    }
                    success = Helper.tcpSend(_tcpOutToServer, cmd);
                }

                success = tcpWaitForReponseFromServer();
                while(!success){
                    //connect to another available server
                    for(int ind = 0; ind<numServer; ind++){
                        //setup connection with the first closet server
                        Pair<String, Integer> closestServer = serverTable.getServerByIndex(ind);
                        boolean connectionEstablished = setupTcpProtocol(closestServer.getKey(), closestServer.getValue());
                        if(connectionEstablished)
                            break;
                    }
                    success = Helper.tcpSend(_tcpOutToServer, cmd);
                    if(!success) continue;
                    success = tcpWaitForReponseFromServer();
                }
            }  else {
                System.out.println("ERROR: No such command");
            }
        }
    }
    private static boolean setupTcpProtocol(String hostAddress, int tcpPort) {

        try {
            _tcpSocket = new Socket();
            //hardcode timeout 100 ms
            _tcpSocket.connect(new InetSocketAddress(hostAddress, tcpPort), 100);
            _tcpOutToServer = new DataOutputStream(_tcpSocket.getOutputStream());
            _tcpInFromServer = new BufferedReader(new InputStreamReader(_tcpSocket.getInputStream()));

            if(debug) {
                System.out.println("Client - connected to server with port "+ tcpPort);
                connectToPort = tcpPort;
            }
        } catch (IOException e) {
            _tcpSocket = null;
            if(debug){
                System.out.println("Client - timeout connecting to server with port " + tcpPort);
            }
            return false;
        }
        return true;
    }

    private static boolean tcpWaitForReponseFromServer() {
        try {
            String serverMsg;
            while ((serverMsg = _tcpInFromServer.readLine()) != null) {

                System.out.println(serverMsg.replace("|", "\n").trim());
                System.out.println();
                break;
            }
            return true;
        }
        catch (IOException e){
            if(debug){
                System.out.println("Client - the server (" + connectToPort +") it is connecting to is down during CS. Need to connect to another active server.");
            }
            return false;
        }

    }
}