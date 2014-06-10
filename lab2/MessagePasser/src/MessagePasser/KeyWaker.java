package MessagePasser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class KeyWaker extends Thread {
	
	/**
	 * Key Inputer: Scan the key board input and call the sender if "s" has been pressed.
	 *
	 * @version 1.0
	 * @author	Chen Wang <chenw@andrew.cmu.edu>
	 */
	
	//=============================================================================
	//============================= V A R I A B L E S =============================
	//=============================================================================
	private int inputKey;
	
	public KeyWaker(String str) {
		super(str);
		this.inputKey = -1;
	}
	
	//========================= P U B L I C  M E T H O D S ========================
	public synchronized void run() {

		while(true) {
			//Wait until socket is set (by setSocket)
			if(this.inputKey == -1) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else
			{

				cmdReader();
				// add MessageSend.
				this.inputKey = -1;
			}
		}
	}
	
	synchronized void readKey() throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		if (in.ready())
		{
			this.inputKey = 1;
			this.notify(); //chenw-2014-0124
		}
	}

	// Start reading interactive input from system.in
	private synchronized void cmdReader()
	{
		String friendName = "";
		String msgKind = "";
		String msgData = "";
		int seq_no;
		
		// Prompt the user to enter the friend name to send a message.
		// System.out.println("The sender thread has been invoked because you pressed key " 
		// + MessagePasser.readCmdInput());
		
		System.out.print("Who do you want to send a message to: ");
		// friendName = MessagePasser.readCmdInput();
		
		// Prompt the user to enter the type of message he wants to send
		System.out.print("Who kind of message do you want to sent to " + friendName + ": ");
		// msgKind = MessagePasser.readCmdInput();
		
		// Prompt the user to enter the friend to send a message.
		System.out.print("What content of message do you want to send: ");
		// msgData = MessagePasser.readCmdInput();
		
		if (MessagePasser.friends.get(friendName) != null)
		{
			seq_no = MessagePasser.friends.get(friendName).getSendMsgCount() + 1;
		
			// Message newMsg = new Message(MessagePasser.curUser.getUserName(), friendName, seq_no, 
				// false, msgKind, (Object)msgData);
			
			// Message Sender
			// MessagePasser.send(newMsg);
		}
		else
		{
			System.out.println("[chenw-debug]You type a WRONG friend name, not send to anyone!!");
		}
	}
	

}
