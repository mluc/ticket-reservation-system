import javafx.util.Pair;

import java.util.ArrayList;

public class ClientServerTable {
    public ArrayList<Pair<String, Integer>> serverEntries = new ArrayList<>();

    public ClientServerTable(){

    }

    public void insert(int serverId, String ipColonPort) {
        String[] ipPort = ipColonPort.split(":");
        String ip = ipPort[0];
        int port = Integer.parseInt(ipPort[1]);

        serverEntries.add(new Pair<>(ip, port));
    }
    public Pair<String, Integer> getServerByIndex(int ind){
        Pair<String, Integer> server = serverEntries.get(ind);

        return server;
    }

}
