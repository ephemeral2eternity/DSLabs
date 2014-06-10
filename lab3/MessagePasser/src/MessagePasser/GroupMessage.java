package MessagePasser;

/*
    File: 
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: 
*/

import java.util.*;

public class GroupMessage extends TimeStampedMessage {

    public GroupMessage(String group, String kind, Object data) {
        super("", kind, data);
        groupName = group;
        acks = new HashMap<String, Integer>();
        nack = false;
    }

    public GroupMessage copy() {
        GroupMessage m = new GroupMessage(groupName, super.getKind(), super.getData());
        m.setSource(super.getSrc());
        m.setDst(super.getDst());
        m.setCC(super.getCC());
        m.setSeqNum(super.getSeqNum());
        m.setTimeStamp(super.getTimeStamp());
        m.setGroupStamp(groupStamp);
        m.setNack(nack);
        m.setNackSrc(nackSrc);
        m.setDuplicate(true);
        Iterator iter = acks.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Integer> pair = (Map.Entry) iter.next();
            m.addAck(pair.getKey(), pair.getValue());
        }
        return m;
    }

    public String toString() {
        String ret = super.toString();
        ret += "Group: " + groupName + "\n";
        return ret;
    }

    protected void addAck(String name, Integer seqNum) {
        acks.put(name, seqNum);
    }

    protected int getAck(String name) {
        Integer ret = acks.get(name);
        if (ret == null) {
            return 0;
        }
        else {
            return ret;
        }
    }

    protected Set<Map.Entry<String, Integer>> getAckMap() {
        return Collections.unmodifiableSet(acks.entrySet());
    }

    public String getGroupName() {
        return groupName;
    }

    protected void setNackSrc(String nack_src) {
        nackSrc = nack_src;
    }

    protected void setGroupStamp(VectorTimeStamp stamp) {
        groupStamp = stamp;
    }

    protected void setNack(boolean is_nack) {
        nack = is_nack;
    }

    public String getNackSrc() {
        return nackSrc;
    }

    public VectorTimeStamp getGroupStamp() {
        return groupStamp;
    }

    public boolean getNack() {
        return nack;
    }

    private String groupName = null;
    private String nackSrc = null;
    private VectorTimeStamp groupStamp = null;
    private Map<String, Integer> acks = null;
    private boolean nack = false;
}
