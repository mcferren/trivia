import java.io.*;
import java.net.*;
import java.util.*;

public class Connection implements Runnable {

    Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private long authorID;
    private String authorName;
    private Queue<String> msgQueue = new LinkedList<String>();
    private LinkedTree tree;

    public Connection(Socket sock) throws IOException {
        
        socket = sock;
        
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream());
    }
    
    public void run(){   
        
        try {
        
            send("What is your name?");
            setAuthorName(receive()); 
        
            send("Would you like to play a guessing game?");
            
            char reply = getYN();
            
            tree = new LinkedTree(socket);
            while (reply == 'Y') {
                tree.execute(this);
                send("Would you like to play a guessing game?");
                reply = getYN();
            }
            
            Server.getInstance().removeFromActiveConnectionList(getAuthorID()); // be sure to remove user from active connection list
                                                                                // before leaving
            
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            System.out.println("Connection dropped");
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                    System.out.println("Connection closed.");
                } catch (IOException ex) {
                    System.err.println("IOException occured while closing connection.");
                }
            }
        }
    }

    public void send(String msg) {
        out.println(msg);
        out.flush();
    }

    public String receive() throws IOException {
        String result = in.readLine();
        processBackspace(result);
        return result;
    }

    char getYN() throws IOException {
        char result;
        while (true) {
            String temp = receive().toUpperCase();
            if (temp.equals("YES") || temp.equals("Y")) {
                return 'Y';
            } else if (temp.equals("NO") || temp.equals("N")) {
                return 'N';
            } else {
                send("That was not a Y/N answer.  Please answer Y or N.");
            }
        } 
    }
    
    private String processBackspace(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '\b') {
                if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public long getAuthorID() {
        return authorID;
    }

    public void setAuthorID(long aID) {
        authorID = aID;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String aN) {
        authorName = aN;
    }

    private Queue<String> getMsgQueue() {
        return msgQueue;
    }
    
    public void addToMessageQueue(String msg){
        getMsgQueue().add(msg);
    }
    
    public ArrayList<String> returnMessages(){
        return new ArrayList<String>(getMsgQueue());
    }
    
    public void clearMessages(){
        getMsgQueue().clear();
    }

    public LinkedTree getTree() {
        return tree;
    }
}
