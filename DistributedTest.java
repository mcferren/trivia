import java.io.*;
import java.net.*;

public class DistributedTest {


    public static void main(String[] args) throws IOException {
        System.setProperty("line.separator", "\r\n");
        if (args.length != 1) {
            System.err.println("Usage: java FileClient <port>");
            System.exit(1);
        }
        DistributedTest server = null;
        try {
            int port = Integer.parseInt(args[0]);
            Server.getInstance().run(port);
        } catch (NumberFormatException ex) {
            System.err.println("Port agurment is not a number: " + args[0]);
        } catch (IOException ex) {
            System.err.println("IOException occured.");
        } finally {
            Server.getInstance().close();
        }
    }
}

// bm
