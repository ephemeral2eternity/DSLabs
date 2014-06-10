package MessagePasser;

import java.io.ObjectOutputStream;
import java.net.Socket;


public class MessageUser {
	
	private String userName = "";
	private String hostName = "";
	private int portNumber = 15000;
	private Socket sendSocket;
	public ObjectOutputStream oos;
	
	private int sendMsgCount = 0;
	private int recMsgCount = 0;
	

	// Construct the MessageUser object.
	public MessageUser(String userName, String hostName, int portNumber) {
		super();
		this.userName = userName;
		this.hostName = hostName;
		this.portNumber = portNumber;
		//MessagePasser.sendThreads.add(this.sendConn); //chenw-2014-0124
		
		// chenw-2014-0124, not necessary
		/* this.recConn = new MsgConnector("Receive Connection from " + this.userName, false);
		this.recConn.start();
		MessagePasser.receiveThreads.add(this.recConn);*/
	}
	
	// Getters and Setters for available variables
	
	
	public String getUserName() {
		return userName;
	}
	
	
	public int getSendMsgCount() {
		return sendMsgCount;
	}
	
	public int getRecMsgCount() {
		return recMsgCount;
	}

	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public int getPortNumber() {
		return portNumber;
	}
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}
	
	public void addSendMsgCount()
	{
		this.sendMsgCount ++;
	}
	
	public void addRecMsgCount()
	{
		this.recMsgCount ++;
	}

	public Socket getSendSocket() {
		return sendSocket;
	}

	public void setSendSocket(Socket sendSocket) {
		this.sendSocket = sendSocket;
	}

}
