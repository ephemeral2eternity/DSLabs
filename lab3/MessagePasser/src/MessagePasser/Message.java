/*
    File: Message.java
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: Lab0 message class
*/

package MessagePasser;

import java.io.Serializable;

public class Message implements Serializable {

    public Message(String dest, String kind, Object data) {
        dst = dest;
        action = Rule_action.none;
        this.kind = kind;
        this.data = data; //potentially unsafe, caller may modify later
    }

    public Message copy() {
        Message ret = new Message(dst, kind, data);
        ret.setSeqNum(seqNum);
        ret.setDuplicate(true);
        ret.setSource(src);
        ret.setCC(cc);

        return ret;
    }

    public void setSource(String source) {
        src = source;
    }

    public void setDst(String dest) {
        dst = dest;
    }

    public void setCC(String new_cc) {
        cc = new_cc;
    }

    public void setAction(Rule_action inAction) {
        action = inAction;
    }

    public void setSeqNum(int sequenceNumber) {
        seqNum = sequenceNumber;
    }

    protected void setDuplicate(boolean dupe) {
        duplicate = dupe;
    }

    public String getSrc() {
        return src;
    }

    public String getCC() { return cc; }

    public String getDst() {
        return dst;
    }

    public String getKind() {
        return kind;
    }

    public Rule_action getAction() {
        return action;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public Boolean getDuplicate() {
        return duplicate; //potentially dangerous, giving a ref to internals?
    }

    public Object getData() {
        return data; //potentially dangerous, can modify internal Message state
    }

    public String toString() {
        return "Src: " + src + "\nDst: " + dst + "\nAction: " + action.name()
                + "\nSeqNum: " + seqNum + "\nKind: " + kind + "\nDup: " + duplicate + "\nData: " + data.toString() +"\n";
    }

    private String src = null;
    private String cc = null;
    private String dst = null;
    private String kind = null;
    private Rule_action action;
    private int seqNum = -2;
    private Boolean duplicate = false;
    private Object data;
}
