import java.io.*;
import java.net.Socket;

public class ServerEntry {
    public int myId;
    public int serverId;
    public String ipAddress;
    public int port;
    public InputStream inStream = null;
    public OutputStream outStream = null;
    public boolean isUp = false;
    public Socket socket = null;
    public BufferedReader bufferedReaderIn = null;
    public DataOutputStream dataOutputStream = null;

    public ServerEntry(int mId, int id, String ip, int p) {
        myId = mId;
        serverId = id;
        ipAddress = ip;
        port = p;
        if(myId == serverId)
            isUp = true;
    }

    public ServerEntry(int mId, int id, String ip, int p, Socket s) {
        myId = mId;
        serverId = id;
        ipAddress = ip;
        port = p;
        try {
            inStream = s.getInputStream();
            outStream = s.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        isUp = true;
        socket = s;
        bufferedReaderIn = new BufferedReader(new InputStreamReader(inStream));
        dataOutputStream = new DataOutputStream(outStream);
    }
}
