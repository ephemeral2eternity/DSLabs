package MessagePasser;

import java.io.Console;

public class MutexMain {
    
    public static void main(String[] args) {

        // If the number of input args is not 3, exit
        if(args.length != 2)
        {
            System.err.println("Usage: <config_file_path> <local_host_name>");
            System.exit(1);
        }

        Console c = System.console();
        if (c == null) {
            System.err.println("Failed to retrieve console object.");
            System.exit(-1);
        }
        
        // Parse arguments
        String configFilePath = args[0]; // parse config file path
        String localHostName = args[1]; // parse local host name
        c.printf("Config: %s, Name: %s\n", configFilePath, localHostName);
   
        // Initiate the message parser
        MutexService ms = null;
        try {
            ms = new MutexService(configFilePath, localHostName);
        } catch (MutexException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (ms == null) {
            System.err.println("MutexService failed to initialize.");
            return;
        }

        String menuEntry = "%1$10s: %2$10s\n";
        int option = -1;
        while (true) {
            c.printf("\n");
            c.printf(menuEntry, "Entry", "Description");
            c.printf(menuEntry, "1", "Request the common resource");
            c.printf(menuEntry, "2", "Release the held resource");
            c.printf(menuEntry, "3", "Get counts");
            c.printf(menuEntry, "4", "Reset all counters");
            c.printf(menuEntry, "5", "Send normal message");
            c.printf(menuEntry, "0", "Shutdown");
            String opt = c.readLine("Choose an option: \n");

            try {
                option = Integer.parseInt(opt);
            } catch (NumberFormatException e) {
                option = -1;
            }

            switch (option) {
                case 0:
                    //TODO: handle shutdown of everything
                    System.exit(0);
                    break;
                case 1:
                    // Request the common resource
                    if (!ms.requestResource())
                    {
                        c.printf("Failed to request resource\n");
                    } else {
                        c.printf("Now holding resource\n");
                    }
                    break;
                case 2:
                    // Release held resource.
                    ms.releaseResource();
                    break;
                case 3:
                    c.printf("Total sent: %d, Remote sent: %d\nTotal received: %d, Remote received: %d\n",
                            ms.getTotalSent(), ms.getRemoteSent(), ms.getTotalRec(), ms.getRemoteRec());
                    break;
                case 4:
                    ms.resetAll();
                    break;
                case 5:
                    //Send a message
                    Message m = createMessage(c);
                    if (ms.sendMessage(m)) {
                        c.printf("Message sent\n");
                    } else {
                        c.printf("Message not sent (delayed or dropped)");
                    }
                    break;
                default:
                    //invalid entry
                    c.printf("Invalid entry\n");
                    break;
            }
        }
    }

    private static TimeStampedMessage createMessage(Console c) {
        TimeStampedMessage m;
        c.printf("\nSend message\n");
        String dst = c.readLine("Enter destination node: ");
        String kind = c.readLine("Enter message kind: ");
        String data = c.readLine("Enter message body (data): ");
        m = new TimeStampedMessage(dst, kind, data);
        return m;
    }

}
