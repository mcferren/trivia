import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LinkedTreeNode {

        private String data;
        private long[] yes = new long[2];                   // index 0 is the length in the file
        private long[] no = new long[2];                    // index 1 is the position of bytes in the file
        private long[] reference = new long[2];
        private long[] fileDetails = new long[2];
        private PrintWriter out;
        private long authorID;
        private Lock lock = new ReentrantLock();
        

        LinkedTreeNode(String name) throws IOException {
            
            setData(name);
            setYes(0, 0);
            setNo(0, 0);
            setReference(0, 0);
        }

        boolean isLeaf() {             // A leaf node is always a FINAL question and spurs creation of new children
            return yes[0] == 0;        // Need to use index 0 here instead of index 1 bc original root needs to be a leaf
        }
        
        boolean isReference() {        // A reference node is just a pass through to another particular node
            return reference[0] != 0;  // A reference node DOES NOT have a zero value
        }

        // conditional tests for three different types of nodes: REFERENCE, QUESTION, and LEAF
        public LinkedTreeNode execute(Connection connection) throws IOException, ClassNotFoundException, InterruptedException {
            
            if(isReference())                                                   // means that its a REFERENCE node
            {                                                                   // meant for pass-through
                return connection.getTree().readExisting_NodeFromFile(getReferencePos());            
                                                                                // keep traversing through QUESTION nodes                                   
            }                                                                   // until you reach a QUESTION node
            
            else if (!isLeaf()) {						// means that its a QUESTION node
                connection.send(getData());                                     // meant for pass-through
                char reply = connection.getYN();                                
                if (reply == 'Y') {                                             // print out <<leading>> question and ask for response
                    return connection.getTree().readExisting_NodeFromFile(getYesPos());              
                } else {
                    return connection.getTree().readExisting_NodeFromFile(getNoPos());               
                }								// keep traversing through QUESTION nodes 
            }                                                                   // until you reach a leaf
            
            else {                                                              // if No, then it means that its a LEAF node
                //http://stackoverflow.com/questions/5715235/java-set-timeout-on-a-certain-block-of-code
                if (lock.tryLock(10000, TimeUnit.MILLISECONDS)) {
                    try {       
                        connection.getTree().refreshCurrent_NodeFromFile(this);     // first refresh to make sure that the node didn't change while 
                                                                                    // the thread waited to enter the synch block. The after effect
                                                                                    // is the same node object but the variables will have changed

                                                                                    // For example, if the thread was in fact waiting to enter the
                                                                                    // synch block, after entry, there will be a referenceLen and 
                                                                                    // referencePos when there wasn't before. This will force the 
                                                                                    // current node to return. If it wasn't waiting, there's no change

                        if(isReference())                                           // if after the refresh, the node's reference variable has value, 
                        {                                                           // then skip to that node ... redundency here is for legibility 
                            return connection.getTree().readExisting_NodeFromFile(getReferencePos());                   
                        }                                                           // we don't actually need the ifReference conditional found above                  

                        connection.send("Is your celebrity " + getData() + "?");
                        char reply = connection.getYN(); 				// prompt <<final>> question
                        if (reply == 'Y') 
                        {                                                   
                            connection.send("Yes!  I'm pretty smart!");             // if Yes, then prompt Congrats, then end

                            if(Server.getInstance().checkActiveConnectionList(getAuthorID()) == true      // if author is active
                               && getAuthorID() != connection.getAuthorID())                              // & author isn't the 
                            {                                                                             // same user as the guesser
                                Connection hisConnection = Server.getInstance()
                                                             .connectionRetrieval(getAuthorID());  // get the author's socket

                                String msg = String.format("%s has guessed your %s leaf",          // build message to send
                                                            connection.getAuthorName(),
                                                            getData());

                                hisConnection.addToMessageQueue(msg);
                            }
                        } 
                        else 
                        {                                
                            connection.send("What celebrity are you thinking of?");              // 1) ask for a new y/n question
                            String newName = connection.receive();                               // 2) convert current node to a <<leading>> question
                            connection.send("Give me a yes or no question "                      // 3) create two children FINAL nodes
                                + "to distinguish between " + getData() + " and " + newName);    // 4) use data from current (former) to populate
                            String newQuestion = connection.receive();                           //    either y or n answer to the new question
                            connection.send("If someone answers yes, would "                     // 5) use answer to new question to populate
                                        + "that be " + newName + "?");                           //    either y or n answer to the new question
                            reply = connection.getYN();
                            connection.send("Thank you for adding " + newName 
                                                                     + " to the game.");

                            // create two new celebrity nodes
                            LinkedTreeNode newCelebrityNode = new LinkedTreeNode(newName);
                            LinkedTreeNode oldCelebrityNode = new LinkedTreeNode(getData());
                            oldCelebrityNode.setAuthorID(getAuthorID());                         // be sure we don't overwrite old data here
                            newCelebrityNode.setAuthorID(connection.getAuthorID());              // attribute author for new node here
                            connection.getTree().writeNew_NodeToFile(newCelebrityNode);
                            connection.getTree().writeNew_NodeToFile(oldCelebrityNode);

                            // create new question node
                            LinkedTreeNode questionNode = new LinkedTreeNode(newQuestion);
                            connection.getTree().writeNew_NodeToFile(questionNode);


                            // make the file positions (of the new celebrity nodes) 
                            // the children of your new question node
                            if (reply == 'Y') {
                                questionNode.setYes(newCelebrityNode.getLength(),
                                                    newCelebrityNode.getFilePosition());
                                questionNode.setNo(oldCelebrityNode.getLength(),
                                                   oldCelebrityNode.getFilePosition());
                            } 
                            else 
                            {
                                questionNode.setYes(oldCelebrityNode.getLength(),
                                                    oldCelebrityNode.getFilePosition());
                                questionNode.setNo(newCelebrityNode.getLength(),
                                                   newCelebrityNode.getFilePosition());
                            }

                            // now we no longer care what the values are for the 
                            // current node's data, yes and no variables they 
                            // will be ignored going forward bc the current node
                            // will now have a reference value. So set the reference 
                            // of the current node to the fileposition new question node
                            setReference(questionNode.getLength(), questionNode.getFilePosition());

                            // now just update all of the nodes you've just worked on
                            connection.getTree().updateExisting_NodeToFile(this);
                            connection.getTree().updateExisting_NodeToFile(questionNode);
                        }
                    } finally {
                        //System.out.println("You got the boot for taking too long");
                        lock.unlock();
                    }
                }
            }
            
            return null;
        }
        
        public String getData(){
            return data;
        }
        
        public long getYesPos(){
            return yes[1];
        }
        
        public long getYesLen(){
            return yes[0];
        }
        
        public long getNoPos(){
            return no[1];
        }
        
        public long getNoLen(){
            return no[0];
        }
        
        public long getReferencePos(){
            return reference[1];
        }

        public long getReferenceLen() {
            return (int) reference[0];
        }

        public long getFilePosition() {
            return fileDetails[1];
        }

        public long getLength() {
            return fileDetails[0];
        }
        
        public void setReference(long length, long position){
            
            reference[0] = length;
            reference[1] = position;
        }
        
        public void setYes(long position, long length){
            
            yes[0] = length;
            yes[1] = position;
        }
        
        public void setNo(long position, long length){
            
            no[0] = length;
            no[1] = position;
        }
        
        public void setData(String d){
            data = d; // won't change after object is first created
        }
        
        public void setFilePostion(long fP){
            fileDetails[1] = fP; // won't change after object is first created
        }
        
        public void setLength(long len){
            fileDetails[0] = len; // won't change after object is first created
        }

        public long getAuthorID() {
            return authorID;
        }

        public void setAuthorID(long aID) {
            authorID = aID;
        }
        
    }