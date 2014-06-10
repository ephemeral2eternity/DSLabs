package MessagePasser;

import java.io.*;

public class Main {

    private static final int RECEIVE_OPTION = 0; // used to parse receive option
    private static final int SEND_OPTION = 1;    // used to parse send option
    private static final int OTHER_OPTION = 2;   // used to parse other option
    private static final int LINE_SIZE = 80;     // set the line size to 80
    
    public static void main(String[] args) {

        // If the number of input args is not 3, exit
        if(args.length != 3)
        {
            System.err.println("Usage: <config_file_path> <local_host_name> <clock_type>");
            System.exit(1);
        }

        Console c = System.console();
        if (c == null) {
            System.err.println("Failed to retrieve console object.\n");
            System.exit(-1);
        }
        
        // Parse arguments
        String configFilePath = args[0]; // parse config file path
        String localHostName = args[1]; // parse local host name
        String clockType = args[2]; // parse clock type
        c.printf("Config: %s, Name: %s, Clock: %s\n", configFilePath, localHostName, clockType);
   
        // Initiate the message parser
        MessagePasser mp = null;
        try {
            mp = new MessagePasser(configFilePath, localHostName, clockType);
        } catch (FileNotFoundException e) {
            System.err.println("File not found.\n");
            System.exit(-1);
        }
        if (mp == null) {
            System.err.println("MessagePasser failed to initialize.\n");
            return;
        }

        String menuEntry = "%1$10s: %2$10s\n";
        int option = -1;
        while (true) {
            c.printf("\n");
            c.printf(menuEntry, "Entry", "Description");
            c.printf(menuEntry, "1", "Send message");
            c.printf(menuEntry, "2", "Send group message");
            c.printf(menuEntry, "3", "Receive message");
            c.printf(menuEntry, "0", "Shutdown");
            String opt = c.readLine("Choose an option: ");

            try {
                option = Integer.parseInt(opt);
            } catch (NumberFormatException e) {
                option = -1;
            }

            Message m;
            switch (option) {
                case 0:
                    //TODO: handle shutdown of everything
                    System.exit(0);
                    break;
                case 1:
                    //Send a message
                    m = createMessage(c);
                    if (mp.send((TimeStampedMessage) m)) {
                        c.printf("Message sent\n");
                    } else {
                        c.printf("Message not sent (delayed or dropped)");
                    }
                    break;
                case 2:
                    //send a group message
                    m = createGroupMessage(c);
                    if (mp.sendGroup((GroupMessage) m)) {
                        c.printf("Message sent\n");
                    } else {
                        c.printf("Message not sent (delayed or dropped)");
                    }
                    break;
                case 3:
                    //Receive a message
                    m = mp.receive();
                    if (m != null) {
                        printMessage(c, m);
                        c.printf("Message complete\n");
                    }
                    else {
                        c.printf("No messages to receive\n");
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

    private static GroupMessage createGroupMessage(Console c) {
        GroupMessage m;
        c.printf("\nSend group message\n");
        String dst = c.readLine("Enter destination group: ");
        String kind = c.readLine("Enter message kind: ");
        String data = c.readLine("Enter message body (data): ");
        m = new GroupMessage(dst, kind, data);
        return m;
    }

    private static void printMessage(Console c, Message m) {
        c.printf("\nReceived message\n");
        c.printf(m.toString());
    }
}
