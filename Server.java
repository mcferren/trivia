import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private ServerSocket serverSocket;
    private volatile static Server instance;
    private Map<Long, Connection> activeConnectionList = new HashMap<Long, Connection>();
    Map<Long, LinkedTreeNode> activeLeafRegistry = new HashMap<Long, LinkedTreeNode>(); //key: nodeID, value: userID

    private Server() throws IOException {
        
    }

    public void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                System.err.println("IOException occured while closing server socket.");
            }
        }
    }
    
    public static Server getInstance() throws IOException {
        
        if(instance == null)
            synchronized(Server.class)
            {
                if(instance == null)
                {
                    instance = new Server();
                }
            }
        
        return instance;
        
    }

    public void run(int port) throws IOException {
        
        serverSocket = new ServerSocket(port);
        
        System.out.println("Server started");
            
        while (true) {
            
            Socket socket;                              
            try 
            {
                socket = serverSocket.accept();
                
                Connection newUserConnection = new Connection(socket);
                
                newUserConnection.setAuthorID(System.currentTimeMillis());      // give user a unique serial number
                
                getActiveConnectionList().put(newUserConnection.getAuthorID(), 
                                              newUserConnection);               // add user to active connections list
                
                Thread t = new Thread(newUserConnection);
                
                t.start();
                
            } catch (IOException ex) {
                System.out.println("Unable to accept socket connection.");
            }
            
            System.out.println("Connection accepted!");
            
        }
    }    
    
    public boolean checkActiveConnectionList(long authorID){
        if(getActiveConnectionList().containsKey(authorID))
            return true;
        else
            return false;
    }
    
    public Connection connectionRetrieval(long authorID){
        return getActiveConnectionList().get(authorID);
    }
    
    public void removeFromActiveConnectionList(long authorID){
        getActiveConnectionList().remove(authorID);
    }

    private Map<Long, Connection> getActiveConnectionList() {
        return activeConnectionList;
    }

    public Map<Long, LinkedTreeNode> getActiveLeafRegistry() {
        return activeLeafRegistry;
    }
    
    public LinkedTreeNode getFromActiveLeafRegistry(long reference){
        return getActiveLeafRegistry().get(reference);
    }
    
    public void addToActiveLeafRegistry(long filePos, LinkedTreeNode subject){
        activeLeafRegistry.put(filePos, subject);
    }
    
    public boolean checkActiveLeafDirectory(long filePos){
        return getActiveLeafRegistry().containsKey(filePos);
    }
    
    public void removeFromActiveLeafRegistry(long filePos){
        activeLeafRegistry.remove(filePos);
    }
}
