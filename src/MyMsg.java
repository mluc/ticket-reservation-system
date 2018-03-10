/**
 * Created by My Luc on 10/7/2017.
 */
public class MyMsg {
    public int src, dest;
    public String fullMessage;
    public String[] messages;
    public MyMsg(int s, int d, String msg){
        if(s == -1){
            System.err.println("Source cannot be -1");
        }
        src = s;
        dest = d;
        fullMessage = msg;
        messages = msg.split(" ");
    }
    public int getMessageInt() {
        return Integer.parseInt(messages[1]);
    }

    public String toString(){
        return "From " + src + " to " +dest + " with message: " + fullMessage;
    }
}
