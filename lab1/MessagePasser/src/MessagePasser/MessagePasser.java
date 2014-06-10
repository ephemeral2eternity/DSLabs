package MessagePasser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import org.yaml.snakeyaml.Yaml;

public class MessagePasser {
	
	//=============================================================================
	//=========================== Public Static Variables =========================
	//=============================================================================
	public static MessageUser curUser;
	public static Hashtable<String, MessageUser> friends;
	public static ConcurrentHashMap<String, ArrayList<MessageRules>> ruleMap = 
	                new ConcurrentHashMap<String, ArrayList<MessageRules>>();
	public static BlockingQueue<Message> sendQueue;		// chenw-2014-0125
	public static BlockingQueue<Message> recvQueue;
	public static BlockingQueue<Message> recvDelayQueue;				// delayed messages in receiving queue.
	public static BlockingQueue<Message> sendDelayQueue;

    private static int seq = 0;
    private static int logSeq = 0;
    
    public static int totalHosts, id;  
    public static ClockService clock = null;
    
    public static int getClockType() {
        if (clock == null)
            return 0;
        else if (clock instanceof LogicalClock)
            return 1;
        else
            return 2;
    }
    
    private static class Listener implements Runnable {
        @Override
        public void run() {
            try {
                ServerSocket server = new ServerSocket(curUser.getPortNumber());
                System.out.println("Start accepting requests");
                
                while(true) {
                    Socket sock = server.accept();
                    System.out.println("Got a new sender!");
                    Thread receiver = new Thread(new Receiver(sock));
                    receiver.start();
                }
            } catch (IOException e) {
                System.err.println("FATAL: " + e.getMessage());
                System.exit(-3);
            }
        }
    }
    
    private static class Receiver implements Runnable {
        private Socket sock;
        private MessageUser node = null;
        
        public Receiver(Socket sock) {
            this.sock = sock;
        }
        
        @Override
        public void run() {
            InputStream is;
            ObjectInputStream ois;
            
            try {
                is = sock.getInputStream();
                ois = new ObjectInputStream(is);
            } catch (IOException e) {
                System.err.println("Receiver of " + node + " encounts an error");
                e.printStackTrace();
                return;
            }
            
            while (true) {
                Message msg;
				try {
					msg = (Message) ois.readObject();
	                node = friends.get(msg.getSource());
	                MessageRules match = MessageRules.matchesRule(msg, MessagePasser.ruleMap.get("recv"));
	                
	                if (match == null)
	                {
	                	recvQueue.add(msg);
	                }
	                else if (match.getAction().equals("drop"))
	                {
                        System.out.println("Match found, dropping Message");
                        continue;
	                }
	                else if (match.getAction().equals("duplicate"))
	                {
	                	recvQueue.add(msg);
	                	Message dup = ((Message) msg.clone());
	                	recvQueue.add(dup);
	                }
	                else if (match.getAction().equals("delay"))
	                {
	                	recvDelayQueue.add(msg);
	                	continue;
	                }
	                
	                while (!recvDelayQueue.isEmpty()) {
	                    Message delay = recvDelayQueue.take();
	                    recvQueue.add(delay);
	                }
				} catch(InterruptedException e) {
                    System.err.println("Receiver of " + node + " encounts an error");
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                	System.err.println("Receiver of " + node + " encounts an error");
					e.printStackTrace();
					return;
				} catch (ClassNotFoundException e2) {
					System.err.println("Receiver of " + node + " encounts an error");
					e2.printStackTrace();
					return;
				}
                                  
            }
        }
    }

    public MessagePasser( String configuration_filename, String local_Name ) {
    	
		// Read Partner's port and IP addresses from configuration file.
		friends = new Hashtable<String, MessageUser>();
		
		/* Call the Function to parse config file here*/
		try{
			configParser(configuration_filename, local_Name);
		} catch (FileNotFoundException e1)
		{
			System.out.print("[chenw-debug]Can not read the file " + configuration_filename);
		}

        Scanner scanner = new Scanner(System.in);
        System.out.println("Specify clockType: Logical, Vector, None:");
        String clockType = scanner.nextLine();
        if (clockType.startsWith("l"))
            MessagePasser.clock = ClockServiceFactory.createClockService("logical");
        else if (clockType.startsWith("v"))
            MessagePasser.clock = ClockServiceFactory.createClockService("vector");
        
		// Initialize local user info by looking up the Hashmap
		if (friends.get(local_Name) != null)
		{
			MessagePasser.curUser = friends.get(local_Name);			
		}
		else
		{
			System.out.println("[vgardiyar-debug]Invalid User Entered!!!");
			return;
		}
		
		try {
			configRules(configuration_filename);
		} catch (FileNotFoundException e1)
		{
            e1.printStackTrace();
		}
        
        sendQueue = new LinkedBlockingDeque<Message>();
        recvQueue = new LinkedBlockingQueue<Message>();
        recvDelayQueue = new LinkedBlockingQueue<Message>();
        sendDelayQueue = new LinkedBlockingQueue<Message>();
        
        Thread listener = new Thread(new Listener());
        listener.start();
        
        System.out.println("MessagePasser initialized");
		System.out.println("You have following friends:");
		
		for ( String key: friends.keySet())
		{
			// if (!key.equals(local_Name))
			System.out.println(key);
		}	
        
		// Initialize config file checker
		ConfigChecker checker = new ConfigChecker(configuration_filename);
		checker.start();
    }
    
    public void send( Message message ) throws IllegalArgumentException, FileNotFoundException, InterruptedException {
        message.set_seqNum(++seq);
        message.set_source(curUser.getUserName());
        message.set_duplicate(false);
        
        int type = getClockType();
        if (type == 1) {
            clock.addTimestamp((TimeStampedMessage) message);
            System.out.println("Timestamp: " + ((TimeStampedMessage) message).logicalTime);
        } else if (type == 2){
            clock.addTimestamp((TimeStampedMessage) message);
            System.out.print("Timestamp: ");
            for (int t : ((TimeStampedMessage) message).vectorTime)
                System.out.print(t + " ");
            System.out.println();
        }
        
        MessageRules match = MessageRules.matchesRule(message, MessagePasser.ruleMap.get("send"));
        
        if (match == null)
        {
        	sendQueue.add(message);
            while (!sendDelayQueue.isEmpty()) {
                Message delay = sendDelayQueue.take();
                sendQueue.add(delay);
            }
        }
        else if (match.getAction().equals("drop"))
        {
            return;
        }
        else if (match.getAction().equals("duplicate"))
        {
        	sendQueue.add(message);
        	
        	Message dup = ((Message) message.clone());
            dup.set_duplicate(true);
            sendQueue.add(dup);

            while (!sendDelayQueue.isEmpty()) {
                Message delay = sendDelayQueue.take();
                sendQueue.add(delay);
            }
        }
        else if (match.getAction().equals("delay"))
        {
        	sendDelayQueue.add(message);
        	return;
        }
    
        while (!sendQueue.isEmpty()) {
            Message msg = sendQueue.take();
            
            String dest = msg.getDest();
            MessageUser node = friends.get(dest);
            
            if (node == null) {
                System.err.println("The destination does not exist!");
                continue;
            }

            genericSend(msg);
        }
    }
    
    public Message receive() throws InterruptedException {
        Message message = recvQueue.take();
        
        int type = getClockType();
        if (type == 1) {
            clock.setTime((TimeStampedMessage) message);
            System.out.println("Timestamp: " + ((TimeStampedMessage) message).logicalTime);
        } else if (type == 2) {
            clock.setTime((TimeStampedMessage) message);
            System.out.print("Timestamp: ");
            for (int t : ((TimeStampedMessage) message).vectorTime)
                System.out.print(t + " ");
            System.out.println();
        }
        
        return message;
    }
    
    public boolean canRecv() {
        return !recvQueue.isEmpty();
    }
    
	/**
	 * configParser: Implementation of a parser for YAML file
	 *				 Update friends' ip, port and info in friends in MessagePasser
	 * @version 1.0
	 * @author	Vignesh Gadiyar
	 */
	//=========================================================================
	//@author  parse the YAML configurations ===================
	//=========================================================================
	public static void configParser(String fileName, String local_name) throws FileNotFoundException
	{
		InputStream input = new FileInputStream(new File(fileName));
		Yaml yaml = new Yaml();
		Map<String, Object> config = (Map<String, Object>) yaml.load(input) ;

		ArrayList<HashMap<String, Object>> configArray = (ArrayList<HashMap<String, Object>>) config.get("configuration");
		
		// Friends' Array Population
		String localName = null;
		String localIp = null;
		Integer localport = 0;
		
		totalHosts = configArray.size();
		
		try{
			for (int a = 0; a < configArray.size(); a ++)
			{
				for (String key:configArray.get(a).keySet())
				{
					if (key.equals("name"))
						localName = (String) configArray.get(a).get(key);
					else if (key.equals("ip"))
						localIp = (String) configArray.get(a).get(key);
					else if (key.equals("port"))
						localport = (Integer) configArray.get(a).get(key);
					else
						System.out.println("[chenw-debug]Unknow field definition " + key + " in configuration file!");
				}
				
				if (localName.equals(local_name))
				    id = a;
				
				friends.put(localName, new MessageUser(localName, localIp, localport));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// Parsing the sendRules and Receive Rules
	public static void configRules(String fileName) throws FileNotFoundException
	{
		InputStream input = new FileInputStream(new File(fileName));
		Yaml yaml = new Yaml();
		Map config = (Map) yaml.load(input);
		
		//=========================================================================
		//====================== Send Rules Population ============================
		//=========================================================================
		ArrayList<HashMap<String, Object>> sendArray = (ArrayList) config.get("sendRules");
	    ArrayList<MessageRules> sendRules = new ArrayList<MessageRules>();

		try
		{
			for(int a = 0; a < sendArray.size(); a++)
			{
				MessageRules rule = new MessageRules((String)sendArray.get(a).get("action"));

				// Parse the rule and add them in the MessageRules object.
				for (String key:sendArray.get(a).keySet())
				{
					if (key.equals("action"))
						rule.setAction((String) sendArray.get(a).get(key));
					else if (key.equals("src") )
						rule.setSrc((String) sendArray.get(a).get(key));
					else if (key.equals("dest"))
						rule.setDst((String) sendArray.get(a).get(key));
					else if (key.equals("kind"))
						rule.setKind((String) sendArray.get(a).get(key));
					else if (key.equals("seqNum"))
						rule.setSeqnum((Integer) sendArray.get(a).get(key));
					else if (key.equals("duplicate"))
						rule.setDuplicate(Boolean.parseBoolean((String) sendArray.get(a).get(key)));
					else
						System.out.print("[chenw-debug]Unknown Rule Action field!!");
				}
				sendRules.add(rule);
			}
		}
		catch (Exception e)
		{
			// e.printStackTrace();
			System.out.println("[vgardiyar-debug] Sending Rule Set is empty!");
		}
		
		//=========================================================================
		//==================== Receive Rules Population ===========================
		//=========================================================================
		ArrayList<HashMap<String, Object>> receiveArray = (ArrayList) config.get("receiveRules");
		ArrayList<MessageRules> receiveRules = new ArrayList<MessageRules>();
		try
		{
			for(int a = 0; a < receiveArray.size(); a++)
			{
				MessageRules rule = new MessageRules((String)receiveArray.get(a).get("action"));

				// Parse the rule and add them in the MessageRules object.
               for (String key:receiveArray.get(a).keySet())
               {
                       if (key.equals("action"))
                               rule.setAction((String) receiveArray.get(a).get(key));
                       else if (key.equals("src") )
                               rule.setSrc((String) receiveArray.get(a).get(key));
                       else if (key.equals("dest"))
                               rule.setDst((String) receiveArray.get(a).get(key));
                       else if (key.equals("kind"))
                               rule.setKind((String) receiveArray.get(a).get(key));
                       else if (key.equals("seqNum"))
                               rule.setSeqnum((Integer) receiveArray.get(a).get(key));
                       else if (key.equals("duplicate"))
                               rule.setDuplicate(Boolean.parseBoolean((String) receiveArray.get(a).get(key)));
               }
               receiveRules.add(rule);
			}
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			System.out.println("[vgardiyar-debug] Receiving Rule Set is empty!");
		}
		
		ruleMap.put("send", sendRules);
        ruleMap.put("recv", receiveRules);
	}
	

	public static void log(TimeStampedMessage msg) {
	    TimeStampedMessage logMsg = new TimeStampedMessage("Logger", "Log", "");
	    logMsg.set_duplicate(false);
	    logMsg.set_seqNum(logSeq++);
	    logMsg.set_source(curUser.getUserName());

	    logMsg.logicalTime = msg.logicalTime;
	    logMsg.vectorTime = msg.vectorTime;
	    
        genericSend(logMsg);
        
        System.out.println(logMsg.getSource() + "-" + logMsg.getSequenceNo() + " sent to Logger");
	}
	
   public static void log(int time) {
        TimeStampedMessage logMsg = new TimeStampedMessage("Logger", "Log", "");
        logMsg.set_duplicate(false);
        logMsg.set_seqNum(logSeq++);
        logMsg.set_source(curUser.getUserName());

        logMsg.logicalTime = time;
        
        genericSend(logMsg);
        
        System.out.println(logMsg.getSource() + "-" + logMsg.getSequenceNo() + " sent to Logger");
    }
   
   public static void log(int[] time) {
       TimeStampedMessage logMsg = new TimeStampedMessage("Logger", "Log", "");
       logMsg.set_duplicate(false);
       logMsg.set_seqNum(logSeq++);
       logMsg.set_source(curUser.getUserName());

       logMsg.vectorTime = time;
   
       genericSend(logMsg);
   
       System.out.println(logMsg.getSource() + "-" + logMsg.getSequenceNo() + " sent to Logger");
   }

	/**
	 * Send method for generic messages that are without special actions.
	 * @param: message: the message to be sent out.
	 */
	public static void genericSend(Message message)
	{
		// Destination info
		ObjectOutputStream oos;
		MessageUser remoteFriend = MessagePasser.friends.get(message.getDest());
		String destName = message.getDest();
		String destHost = remoteFriend.getHostName();
		int destPort = remoteFriend.getPortNumber();
		Socket sendSc = remoteFriend.getSendSocket();
		
		if (sendSc == null)
		{
			try {
				 sendSc = (new Socket(destHost, destPort));
				 MessagePasser.friends.get(destName).setSendSocket(sendSc);				 
				 oos = new ObjectOutputStream(sendSc.getOutputStream());	
				 remoteFriend.oos = oos;
			} catch (IOException e1) {
				// Failed to create a socket
				System.out.println("[chenw-debug]Creating object writing of the socket to " + destHost + " failed!");
				return;
				//e.printStackTrace();
			}
		}
		else
		{
			oos = remoteFriend.oos;
		}

		try {	
			remoteFriend.addSendMsgCount();
			oos.reset();
			oos.writeObject(message);
			oos.flush();
		
			//System.out.println("[chenw-debug]Successfully send msg to" + destName);
		} catch (IOException e) {
			System.out.println("[chenw-debug]Message Object writing failed!");
		}	
	}
}
