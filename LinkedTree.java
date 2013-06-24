import java.io.*;
import java.net.*;

/**
 *
 * @author Ben
 */
public class LinkedTree {

    private LinkedTreeNode startupRoot;
    RandomAccessFile raf;
    Socket connectionSocket;

    public LinkedTree(Socket s) throws IOException {
        
        connectionSocket = s;
        raf = new RandomAccessFile("test.txt", "rw");             
                
        // if you are the first user and the file is empty
        if(raf.length() == 0)
        {
            
            // then create startup no object
            startupRoot = new LinkedTreeNode("Barack Obama");
            
            startupRoot.setAuthorID(0); // make sure noone is attributed as author of this node
            
            // writes the startup object to file
            writeNew_NodeToFile(startupRoot);
            
        }
    }

    public void execute(Connection c) throws IOException { 
            
        // establish root node object before entering the loop
        LinkedTreeNode current = null;
        try {
            
            // populate the root object by reading from file at index zero
            // if the root node is not at zero, then what is at zero will give a 
            // reference to the root
            current = readExisting_NodeFromFile(0);                             // this is why synchronized(this) won't work
            
            // loop until after you reach leaf and make revisions to that leaf node
            while (current != null) {
                pingMessageQueue(c);           // appropriate time to broadcast "I guessed it!" messages
                current = current.execute(c); // execute() returns a LinkedTreeNode object
            }
        
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private void pingMessageQueue(Connection c){

        for(String msg : c.returnMessages())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("***ALERT*** ");
            sb.append(msg);
            sb.append(" ***ALERT***");
            c.send(sb.toString());
        }
        
        c.clearMessages();
    }
    
    public LinkedTreeNode readExisting_NodeFromFile(long reference) 
                                    throws IOException, ClassNotFoundException {
        
        // first check the activeLeafDirectory to see if this node is already there
        if(Server.getInstance().checkActiveLeafDirectory(reference) == true)
        {
            // if its there get that exact object instead of creating a new copy
            return Server.getInstance().getFromActiveLeafRegistry(reference); 
        }
                
        // go to file where object is located
        raf.seek(reference);
        
        // first identify the node length which is found in the first 8 bytes
        // this node tells us how many bytes long the node is
        long length = raf.readLong();
        
        // create shell used to return node object
        LinkedTreeNode returnNode = new LinkedTreeNode("");

        // read from file and populate the shell node object
        returnNode.setLength(length);                           // thisLen
        returnNode.setFilePostion(raf.readLong());              // thisPos
        
        returnNode.setAuthorID(raf.readLong());                 // authorID
        
        
        returnNode.setYes(raf.readLong(),                       // yesLen
                          raf.readLong());                      // yesPos
        
        returnNode.setNo(raf.readLong(),                        // noLen
                          raf.readLong());                      // noPos
        
        returnNode.setReference(raf.readLong(),                 // redLen
                                raf.readLong());                // refPos
            
        // read and then writing data attribute is complicated
        // you must build up the string by reading chars 
        int increment = 72;
        StringBuilder sb = new StringBuilder();
        while(increment < length)
        {
            sb.append(raf.readChar());
            increment += 2;
        }
        returnNode.setData(sb.toString());  
        
        if(returnNode.getYesLen() == 0)
        {
            Server.getInstance().addToActiveLeafRegistry(reference, returnNode);  // if we've gotten this far, then 
        }                                                                         // add to registry if its a leaf
        
        // would like to add code here to remove from registry but can't due to time contraints
        // there is complexity related to multiple players having the node and not knowing if its
        // ok to remove it from registry because someone else might still be using it

        return returnNode;

    }
    
    public synchronized void writeNew_NodeToFile(LinkedTreeNode subject) throws IOException {

        // Write the node object to end of the file
        long position = raf.length();
        raf.seek(position);
        

        // now identify the total byte length of the node
        byte[] nodeBuf = subject.getData().getBytes();
        long length = (nodeBuf.length * 2) + 72;            // * 2 bc java stores chars as 2 bytes
                                                            // 72 = 8 bytes * 9 long data points

        // Save information about where/how the node object lives in the file
        subject.setLength(length);
        subject.setFilePostion(position);
        

        // write the data to the file
        raf.writeLong(subject.getLength());
        raf.writeLong(subject.getFilePosition());
        raf.writeLong(subject.getAuthorID());
        raf.writeLong(subject.getYesLen());
        raf.writeLong(subject.getYesPos());
        raf.writeLong(subject.getNoLen());
        raf.writeLong(subject.getNoPos());
        raf.writeLong(subject.getReferenceLen());
        raf.writeLong(subject.getReferencePos());

        // write the data last because it does not have a consistent length
        raf.writeChars(subject.getData());
    }

    public synchronized void updateExisting_NodeToFile(LinkedTreeNode subject) throws IOException{

        // find where the object lives in the file
        raf.seek(subject.getFilePosition());

        // Write the node object to the file
        raf.writeLong(subject.getLength());
        raf.writeLong(subject.getFilePosition());
        raf.writeLong(subject.getAuthorID());
        raf.writeLong(subject.getYesLen());
        raf.writeLong(subject.getYesPos());
        raf.writeLong(subject.getNoLen());
        raf.writeLong(subject.getNoPos());
        raf.writeLong(subject.getReferenceLen());
        raf.writeLong(subject.getReferencePos());

        // write the data last because it does not have a consistent length
        raf.writeChars(subject.getData());
    }

    // this method is invoke immediatly upon entering the synch block of the node's
    // execute method. It is meant to refresh the variables of the current node
    // thread just in case it's values had changed while it was waiting. 
    public void refreshCurrent_NodeFromFile(LinkedTreeNode subject) throws IOException, ClassNotFoundException{

        // find where the object lives in the file
        raf.seek(subject.getFilePosition());                                    
        
        // first identify the node length which is found in the first 8 bytes
        // this node tells us how many bytes long the node is
        long length = raf.readLong();

        // read from file and populate the node object variable
        subject.setLength(length);                                              // thisLen
        subject.setFilePostion(raf.readLong());                                 // thisPos
        
        subject.setAuthorID(raf.readLong());                                    // authorID
        
        subject.setYes(raf.readLong(),                                          // yesLen
                       raf.readLong());                                         // yesPos
        subject.setNo(raf.readLong(),                                           // noLen
                      raf.readLong());                                          // noPos
        subject.setReference(raf.readLong(),                                    // redLen
                             raf.readLong());                                   // refPos

        // read and then writing data attribute is complicated
        // you must build up the string by reading chars 
        int increment = 72;
        StringBuilder sb = new StringBuilder();
        while(increment < length)
        {
            sb.append(raf.readChar());
            increment += 2;
        }
        subject.setData(sb.toString());  
    }

}
