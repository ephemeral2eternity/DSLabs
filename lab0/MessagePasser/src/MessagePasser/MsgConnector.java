package MessagePasser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * Connector: Represents one receiving connection from a message friend to current user
 *            or a sending connection from current user to a friend. 
 * 			  Used to support multithreading
 *
 * @version 1.0
 * @author	Chen Wang <chenw@andrew.cmu.edu>
 */
public class MsgConnector extends Thread {
	//=============================================================================
	//============================= V A R I A B L E S =============================
	//=============================================================================
	public Socket soc;
	// boolean isSend;		// chenw-2014-0124
	
	// For receivers.
	private ObjectInputStream ois;
	// private ArrayList<Message> receivedMsg;
	private int receiveNum = 0;
	
	// For sender
	// private ArrayList<Message> sendMsg;
	// private int sendNum = 0;
	// private ObjectOutputStream oos;

	//=============================================================================
	//=============================== M E T H O D S ===============================
	//=============================================================================	

	//========================== C O N S T R U C T O R S ==========================
	public MsgConnector(String name, boolean isSending) {
		super(name);
		soc = null;
		
		// chenw-2014-0124
		/* this.isSend = isSending;	
		if (this.isSend)
		{
			this.sendMsg = new ArrayList<Message>();
		}
		else
		{*/
			MessagePasser.receivedMsg = new ArrayList<Message>();
			MessagePasser.delayMsgs = new ArrayList<Message>();
	//	}


	}

	
	//========================= P U B L I C  M E T H O D S ========================
	@Override
	public synchronized void run() {

		while(true) {
			//Wait until socket is set (by setSocket)
			if(soc == null) {
				try {
					wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
			Message recMsg = receiveMessage();
			if (recMsg != null)
			{
				this.process(recMsg);
			}
			else
			{
				soc = null;
			}
			
			// Add back to the receiving thread pool. chenw-2014-0124
			MessagePasser.receive();
			if (MessagePasser.receiveThreads.size() < MessagerConstant.MAXCONN)
				MessagePasser.receiveThreads.add(this);
		}
	}
	
	/**
	 * Process the request asked for by a user
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public Message receiveMessage() {		
		Message recMsg = null;
		try {
				this.ois = new ObjectInputStream(this.soc.getInputStream());
				try {
					recMsg = (Message) ois.readObject();
					this.receiveNum ++;
					//System.out.println(recMsg);
					MessagePasser.receivedMsg.add(recMsg);
					
				} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("[chenw-debug]Can't read object from the accepted socket!");
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} 
		catch (IOException e) {
			/*try {
				this.ois.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				this.soc.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/
			System.out.println("[chenw-debug]Can't read from the accepted socket any more!");
		}
		return recMsg;
	}
	
	private synchronized void process(Message recMsg)
	{
		// Get the only one message and send ack Parse whether
		String localUsr = recMsg.getDest();
		String clientUsr = recMsg.getSource();
		
		MessagePasser.receivedMsg.add(recMsg);
		this.receiveNum ++;
		
		//System.out.print(recMsg);
		
		if (recMsg.getKind().equals("Request"))
		{
			MessageUser remoteFriend = MessagePasser.friends.get(clientUsr);
			int seq = remoteFriend.getSendMsgCount() + 1;
			Message ackMsg = new Message(localUsr, clientUsr, seq, false, "Ack", "");
			MessagePasser.send(ackMsg);
			System.out.println("[chenw-debug]Ack the request of client " + clientUsr);
		}
	}
	
	// Change Sender to the MessagePasser.jave chenw-2014-0124
	
	
	/* Getter and Setter for the socket of current connection*/
	public Socket getSoc() {
		return soc;
	}

	// Update for the existing message numbers
	/* chenw-2014-0124 unnecessary code
	public void setSoc(Socket soc) {
		this.soc = soc;
	}*/
	
	public int getReceiveNum() {
		return receiveNum;
	}

	public void setReceiveNum(int receiveNum) {
		this.receiveNum = receiveNum;
	}


/*	public int getSendNum() {
		return sendNum;
	}


	public void setSendNum(int sendNum) {
		this.sendNum = sendNum;
	}*/


	public synchronized void setSocket(Socket recSc) {
		// TODO Auto-generated method stub
		this.soc = recSc;
		try {
			this.soc.setSoTimeout(MessagerConstant.SC_READ_TIMEOUT);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println("[chenw-debug] Can't set reading socket time out.");
		}
		this.notify();
	}

}
