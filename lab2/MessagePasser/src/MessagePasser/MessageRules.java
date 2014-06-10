package MessagePasser;

import java.util.ArrayList;

public class MessageRules {
	private String action;
	private String src;
	private String dst;
	private String kind;
	private int seqNum;
	private boolean duplicate;
	
	public MessageRules(String action)
	{
		this.action = action;
		this.src = null;
		this.dst = null;
		this.kind = null;
		this.seqNum = -1;
		this.duplicate = false;
	}
	
	// Setters and Getters for private variables.
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setSrc(String src)
	{
		this.src = src;
	}
	
	public String getSrc()
    {
            return this.src;
    }

    public void setDst(String dst)
    {
            this.dst = dst;
    }

    public String getDst()
    {
            return this.dst;
    }

    public void setKind(String kind)
    {
            this.kind = kind;
    }

    public String getKind()
    {
            return this.kind;
    }

    public void setSeqnum(int seqNum)
    {
            this.seqNum = seqNum;
    }

    public int getSeqNum()
    {
            return this.seqNum;
    }

    public void setDuplicate(boolean duplicate)
    {
            this.duplicate = duplicate;
    }

    public boolean getDuplicate()
    {
            return this.duplicate;
    }

    /* Checks for passing rule and returns it
     * Input: Message and either sendRules or receiveRules
     * Output: rule if matched, else null
     */
    public static MessageRules matchesRule(Message message, ArrayList<MessageRules> inputRules)
    {
            for (MessageRules rule : inputRules)
            {
                    if (rule.getSrc() != null)
                    {
                    		if (!(rule.getSrc().equals(message.getSource())))
                    			continue;
                    }
                    if (rule.getDst() != null)
                    {
                    		if (!(rule.getDst().equals(message.getDest())))
                    			continue;
                    }
                    if (rule.getKind() != null)
                    {
                    	if (!(rule.getKind().equals(message.getKind())))
                    		continue;
                    }
                    if (rule.getSeqNum() != -1)
                    {
                    	if (rule.getSeqNum() != message.getSequenceNo())
                    		continue;
                    }
                    if (rule.getDuplicate() != false)
                    {
                            if (rule.getDuplicate() != message.getDuplicate())
                                    break;
                    }
                    /* Passes all rules, return rule object here */
                    return rule;
            }
            /* Doesn't pass any rule, return null */
            return null;
    }


}
