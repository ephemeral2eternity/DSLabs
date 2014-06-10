package MessagePasser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;

public class Message implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private String source;
	private String dest;
	private int sequenceNo;
	private Boolean duplicate;
	private String kind;
	private Object data;

	
	public Message(String srcUser, String destUser, int seqNo, Boolean dup_flg, String msgKind, Object msgData) {
		// TODO Auto-generated constructor stub
		this.source = srcUser;
		this.dest = destUser;
		this.sequenceNo = seqNo;
		this.duplicate = dup_flg;
		this.kind = msgKind;
		this.data = msgData;
	}
	
	// Message Getters
	public int getSequenceNo() {
		return sequenceNo;
	}

	public String getKind() {
		return kind;
	}

	public Object getData() {
		return data;
	}

	public Boolean getDuplicate() {
		return duplicate;
	}

	public String getDest() {
		return this.dest;
	}
	
	public String getSource() {
		return source;
	}

	// Message Setters
	public void setSequenceNo(int sequenceNo) {
		this.sequenceNo = sequenceNo;
	}


	public void set_source(String source)
	{
		this.source = source;
		
	}
	
	public void set_seqNum(int sequenceNumber)
	{
		this.sequenceNo = sequenceNumber;
	}
	
	public void set_duplicate(Boolean dupe)
	{
		this.duplicate = dupe;
	}

	// Message toString
	public String toString()
	{
		String outString = new String("SENDER:" + this.source + ";" +
                "RECEIVER:" + this.dest + ";" +
                "SEQ_NO: " + this.sequenceNo + ";" +
                "DUP_FLAG: " + this.duplicate + ";" +
                "KIND:" + this.kind + ";" +
                "DATA:" + this.data + ";\n");;
		
		return outString;
	}

	public static Message ParseMessage(String recMsg) throws IOException {
		// TODO Auto-generated method stub
		String l = "";
		String[] elements = new String[5];
		
		BufferedReader reader = new BufferedReader(new StringReader(recMsg));
		int i = 0;
		while((l = reader.readLine())!= null)
		{
			elements[i] = l.split(":")[1];
			i ++;
		}
		
		Message parsedMsg = new Message(elements[0], elements[1], Integer.valueOf(elements[2]),
				                  Boolean.parseBoolean(elements[3]), elements[4], elements[5]);
		
		return parsedMsg;
	}

}
