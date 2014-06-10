package MessagePasser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
//import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import org.yaml.snakeyaml.Yaml;



/**
 * MessageParser: Implementation of a simple multithreaded Message Passer 
 * to send and receive formatted messages.
 *
 * @version 1.0
 * @author	Chen Wang <chenw@andrew.cmu.edu>
 */
public class MessagePasser {

	//=============================================================================
	//=========================== Public Static Variables =========================
	//=============================================================================
	public static MessageUser curUser;
	public static ServerSocket receiveSocket = null;
	public static Hashtable<String, MessageUser> friends;
	// public static Vector<MsgConnector> sendThreads = new Vector<MsgConnector>();  //chenw-2014-0124, not necessary
	public static Vector<MsgConnector> receiveThreads = new Vector<MsgConnector>();
	public static ArrayList<MessageRules> sendRules;
	public static ArrayList<MessageRules> receiveRules;
	public static Vector<Message> delaySendMsgs = new Vector<Message>();		// chenw-2014-0125
	public static ArrayList<Message> receivedMsg;
	public static ArrayList<Message> delayMsgs;
	
	//=============================================================================
	//=============================== PUBLIC METHODS ==============================
	//=============================================================================
	public MessagePasser(String configuration_filename, String local_name)
	{
		// Read Partner's port and IP addresses from configuration file.
		friends = new Hashtable<String, MessageUser>();
		
		/* Call the Function to parse config file here*/
		try{
			configParser(configuration_filename);
		} catch (FileNotFoundException e1)
		{
			// TODO Auto-generated catch block
			System.out.print("[chenw-debug]Can not read the file " + configuration_filename);
		}

		// Initialize local user info by looking up the Hashmap
		if (friends.get(local_name) != null)
		{
			MessagePasser.curUser = friends.get(local_name);
		}
		else
		{
			System.out.println("[vgardiyar-debug]Invalid User Entered!!!");
			return;
		}
		
		/* Create new Rule Lists containing all the send and receive rules */
		sendRules = new ArrayList<MessageRules>();
		receiveRules = new ArrayList<MessageRules>();
		try {
			configRules(configuration_filename);
		} catch (FileNotFoundException e1)
		{
			// TODO Auto-generated catch block
            e1.printStackTrace();
		}
		
		
		// Open the port on this host to listen for accept connections from clients.
		try {
			MessagePasser.receiveSocket = new ServerSocket(MessagePasser.curUser.getPortNumber());
		}
		catch (IOException e)
		{
			System.out.println(e);
		}
		
		createReceiveThreads(receiveThreads);
		
		
		// System Print out to access interactive user control.
		System.out.println("The MessagePasser user " + MessagePasser.curUser.getUserName() 
				+ " started receiving messages at port: " + MessagePasser.curUser.getPortNumber());
		System.out.println("If you want to send a message to your friend, please press any key + ENTER and wait for the prompt");
		
		System.out.println("You have following friends:");
		
		for ( String key: friends.keySet())
		{
			if (!key.equals(local_name))
				System.out.println(key);
		}	
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
	public void configParser(String fileName) throws FileNotFoundException
	{
		InputStream input = new FileInputStream(new File(fileName));
		Yaml yaml = new Yaml();
		Map<String, Object> config = (Map<String, Object>) yaml.load(input) ;

		ArrayList<HashMap<String, Object>> configArray = (ArrayList<HashMap<String, Object>>) config.get("configuration");
		
		// Friends' Array Population
		String localName = null;
		String localIp = null;
		Integer localport = 0;
		
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
				friends.put(localName, new MessageUser(localName, localIp, localport));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}

	// Parsing the sendRules and Receive Rules
	public void configRules(String fileName) throws FileNotFoundException
	{
		InputStream input = new FileInputStream(new File(fileName));
		Yaml yaml = new Yaml();
		Map config = (Map) yaml.load(input);
		
		//=========================================================================
		//====================== Send Rules Population ============================
		//=========================================================================
		ArrayList<HashMap<String, Object>> sendArray = (ArrayList) config.get("sendRules");
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
	}
	
	//=========================================================================
	//======================== Interactive Tool ===============================
	//=========================================================================
	public void startApp()
	{		
		try {
			MessagePasser.receiveSocket.setSoTimeout(MessagerConstant.TIMEOUT);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		KeyWaker keyThread = new KeyWaker("KeyWaiter");
		keyThread.start();
		Socket recSc;
		MsgConnector c;
		
		while(true)
		{
			synchronized(keyThread){
				try{
					keyThread.readKey();
				}catch(IOException e)
				{
					e.printStackTrace();
				}
			}
			
			//chenw-2014-0124
			
			synchronized(receiveThreads)
			{
				try {
					recSc = MessagePasser.receiveSocket.accept();
					
					//Take threads from vector if not empty and run them
					if (!receiveThreads.isEmpty()) {
						c = receiveThreads.elementAt(0);
						receiveThreads.removeElementAt(0);
						c.setSocket(recSc);
					}
					//Otherwise create new threads
					else {
						c = new MsgConnector("Additional Receiver", false);
						c.start();
						c.setSocket(recSc);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
		}
		
	}
	
	
	//=========================================================================
	//========================= Message Sender ================================
	//=========================================================================
	public static void send(Message message)
	{
		
		// [vgadiyar-2014-0124] Sending Rules Filtering
		MessageRules match = MessageRules.matchesRule(message, MessagePasser.sendRules);
		
		// In case the previous socket expires.
		if (match == null)
		{
			System.out.println("No Matched Rules Found");
			genericSend(message);
			delaySend();
		}
		else if (match.getAction().equals("drop"))
		{
			System.out.println("Match Found, dropping Message");
			delaySend();
			return;
		}
		else if (match.getAction().equals("duplicate"))
		{
			System.out.println("Match found, duplicating packet");
			dupSend(message);
			delaySend();
		}
		else if (match.getAction().equals("delay"))
		{
			System.out.println("Match found, delaying message till the next non-delayed message");
			// Add the message to the delayedMessages Vector here.
			delaySendMsgs.add(message);
		}
	}
	
	/**
	 * Send method for duplicate messages.
	 * @param: message: the message to be sent out.
	 */
	public static void dupSend(Message message)
	{
		Message dupMsg = new Message(message.getSource(), message.getDest(), message.getSequenceNo(), true, message.getKind(), message.getData());
		genericSend(message);
		genericSend(dupMsg);
	}
	
	/**
	 * Send method for delayed messages.
	 * @param: message: the message to be sent out.
	 */
	public static void delaySend()
	{
		// Checking if there is delayed messages.
		if (delaySendMsgs.size() > 0)
		{
			for (Message dsMsg: delaySendMsgs)
			{
				friends.get(dsMsg.getDest()).addSendMsgCount();
				genericSend(dsMsg);
			}
			delaySendMsgs.clear();
		}
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
			} catch (UnknownHostException e1) {
				// The remote user is not online
				System.out.println("[chenw-debug]Remote friend " + destName + " is not online!");
				return;
			} catch (IOException e1) {
				// Failed to create a socket
				System.out.println("[chenw-debug]Creating object writing of the socket to " + destHost + " failed!");
				return;
				//e.printStackTrace();
			}
		}
		else
		{
			try {
				oos = new ObjectOutputStream(sendSc.getOutputStream());
			} catch (IOException e) {
				System.out.println("[chenw-debug]Creating object writing of the socket to " + destHost + " failed!");
				return;
			}
		}


		try {	
			remoteFriend.addSendMsgCount();
			oos.writeObject(message);
			oos.flush();
		
			System.out.println("[chenw-debug]Successfully send msg to" + destName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println("[chenw-debug]Message Object writing failed!");
			//return;ye
		}	
	}
	
	
   public static void printDelayedRecv()
   {                   
       if (!delayMsgs.isEmpty())
       {
           for (Message dmsg:delayMsgs)
           {
        	   System.out.println(dmsg);
           }
           delayMsgs.clear();
       }
   }
   
   
  public static void receive()
  {
          Message displayMessage;
          if (!receivedMsg.isEmpty()) {
                  
                  displayMessage = receivedMsg.get(receivedMsg.size() - 1);
                  // Receiving Rules Filtering
                  MessageRules match = MessageRules.matchesRule(displayMessage, MessagePasser.receiveRules);
                                  
                  if (match == null)
                  {
                          System.out.println("No Match found");
                          System.out.println(displayMessage);
                          receivedMsg.remove(receivedMsg.size() - 1);
                          printDelayedRecv();
                          
                  }
                  else if (match.getAction().equals("drop"))
                  {
                          System.out.println("Match found, dropping Message");
                          receivedMsg.remove(receivedMsg.size() - 1);
                          printDelayedRecv();
                          return;
                  }
                  else if (match.getAction().equals("duplicate"))
                  {
                          System.out.println("Match found, duplicating packet");
                          System.out.println(displayMessage);
                          System.out.println(displayMessage);
                          receivedMsg.remove(receivedMsg.size() - 1);
                          printDelayedRecv();
                  }
                  else if (match.getAction().equals("delay"))
                  {
                	  	  delayMsgs.add(displayMessage);
                          System.out.println("Match found, delaying message till the next non-delayed message");
                          return;
                  }                               
                  
                  
          }
          else
          {
                  System.out.println("No Messages to be received");
          }
          
  }

	
	/**
	 * A method to read input string from command line.
	 * @return input string.
	 */
	public static String readCmdInput()
	{
		// Open a standard input.
		Scanner in = new Scanner(System.in);
		
		// Read the file name of the configuration file from the command line.
		// Try/Catch are used with readLine() method.
		String inputString = "";
		
		inputString = in.nextLine();

		return inputString;
	}
	
	/**
	 * Create threads and add to given vector
	 * @param The vector to add new threads to
	 */
	public static void createReceiveThreads(Vector<MsgConnector> threads) {
		for(int i = 0; i < MessagerConstant.MAXCONN; i++) {
			MsgConnector c = new MsgConnector("Receiver "+i, false);
			c.start();
			threads.add(c);
		}
	}
	
	
	/**
	 * Main function
	 * Starts the interactive tool for receiving and sending messages.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Interactive Command Line Tool to get the parameters
		
		// Prompt the user to enter the configuration file name
		System.out.print("Enter the path of the configuration file: ");
		String configuration_filename = MessagePasser.readCmdInput();
		
		
		// Prompt the user to enter the unique name for this process
		System.out.print("Enter a unique name for this particular process: ");
		String process_name = MessagePasser.readCmdInput();
			
		MessagePasser msgPasser = new MessagePasser(configuration_filename, process_name);
		
		msgPasser.startApp();
		
		
		// Release the resources
		releaseSocket();
		
	}
	
	public static void releaseSocket() throws IOException
	{
		MsgConnector c;
		
		// Please Add friends send socket release. chenw-2014-0124
		
		for (int i = 0; i < receiveThreads.size(); i ++)
		{
			c = receiveThreads.get(i);
			c.soc.close();
		}
	}

}
