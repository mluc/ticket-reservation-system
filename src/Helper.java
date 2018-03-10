import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Helper {

    public static boolean debug = false;
    public static boolean tcpSend(DataOutputStream outToServer, String msg){
        if(debug) System.out.println("tcpSend - sending: " + msg);
        try {
            outToServer.writeBytes(msg.trim() +"\n");
            return true;
        } catch (IOException e) {
            if(debug) e.printStackTrace();
            return false;
        }
    }
}
