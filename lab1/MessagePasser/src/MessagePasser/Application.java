package MessagePasser;


import java.io.IOException;
import java.util.Scanner;

import MessagePasser.Message;
import MessagePasser.MessagePasser;

public class Application {

	 static MessagePasser passer;
	 private static final int argNum = 2;
	    
	    private static final String sendCommandStr = "send";
	    private static final String recvCommandStr = "recv";
	    
	    public static void main(String[] args) throws IllegalArgumentException, IOException, InterruptedException {
	        String configFilename = null;
	        String localName = null;
	        
	        if (args.length != argNum) {
	            System.err.println("Please input Config File Name:");
	            System.err.println("Please input Local Name:");
	        }
	        else {
	            configFilename = args[0];
	            localName = args[1];
	            System.out.println("Config File Name [" + configFilename +
	                                "] and Local Name [" + localName + "] read from arguments.");
	        }
	        
	        if( configFilename == null || localName == null ) System.exit(-1);
	        
	        try{
	            passer = new MessagePasser(configFilename, localName);
	        }
	        catch( Exception e ) {
	            System.out.println("Fail to initialize: " + e.getMessage() );
	            System.exit(-1);
	        }

	        Scanner scanner = new Scanner(System.in);
	        while( true ) {
	            System.out.println("Please input next command to execute:\n" +
	                               "( \"send\" to sent a message; \"recv\" to receive a message)");
	            String command = scanner.nextLine();
	            
	            if( sendCommandStr.startsWith(command) ) {
	                System.out.println("Please input the Local Name of receiver:");
	                String recvLocalName = scanner.nextLine();
	                MessageUser recvNode = passer.friends.get(recvLocalName);
	                if( recvNode == null ) {
	                    System.err.println("Node [" + recvLocalName + "] not found.");
	                    continue;
	                }

	                System.out.println("Please input the Kind of the message:");
	                String kindStr = scanner.nextLine();

	                System.out.println("Please input the Content of the message:");
	                String content = scanner.nextLine();
	                
	                Message toSend;
	                if (MessagePasser.getClockType() > 0)
	                    toSend = new TimeStampedMessage(recvLocalName, kindStr, content);
	                else
	                    toSend = new Message(recvLocalName, kindStr, content);
	                
	                passer.send(toSend);
	                
	                if (MessagePasser.getClockType() > 0) {
    	                System.out.println("Log this event?");
    	                String ans = scanner.nextLine();
    	                if (ans.startsWith("y"))
    	                    MessagePasser.log((TimeStampedMessage) toSend);
	                }
	            }
	            else if( recvCommandStr.startsWith(command) ) {
	                try {
	                    Message received = passer.receive();
	                    if( received == null )
	                        System.out.println("No new message to receive.");
	                    else
	                        System.out.println("Message received:\n" + received + "\n(End)");
	                    
	                    if (MessagePasser.getClockType() > 0) {
	                        System.out.println("Log this event?");
	                        String ans = scanner.nextLine();
	                        if (ans.startsWith("y"))
	                            MessagePasser.log((TimeStampedMessage) received);
	                    }
	                }
	                catch( Exception e ) {
	                    System.err.println("Failed to receive message: " + e.getMessage() );
	                }
	            }
	            else if (command.startsWith("t"))
	            {
	                if (MessagePasser.getClockType() == 1) {
	                    int time = ((LogicalClock) MessagePasser.clock).getTimestamp();
	                    System.out.println("Timestamp: " + time);
	                    
	                    System.out.println("Log this event?");
                        String ans = scanner.nextLine();
                        if (ans.startsWith("y"))
                            MessagePasser.log(time);
	                } else if (MessagePasser.getClockType() == 2) {
	                    int[] time = ((VectorClock) MessagePasser.clock).getTimestamp();
	                    System.out.print("Timestamp: ");
	                    for (int t : time)
	                        System.out.print(t + " ");
	                    System.out.println();
	                    
	                    System.out.println("Log this event?");
                        String ans = scanner.nextLine();
                        if (ans.startsWith("y"))
                            MessagePasser.log(time);
	                }
	            }
	            else {
	                System.err.println("Unrecognized command [" + command + "], try again.");
	                continue;
	            }
	        } 
	    }

}
