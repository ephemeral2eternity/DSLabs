/*
    File: TimestampedMessage.java
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: Lab1 timestamped message extension
*/

package MessagePasser;

public class TimeStampedMessage extends Message {

    public TimeStampedMessage(String dest, String kind, Object data) {
        super(dest, kind, data);
    }

    public void setTimeStamp(TimeStamp time) {
        timeStamp = time;
    }

    public TimeStamp getTimeStamp() {
        return timeStamp;
    }

    public TimeStampedMessage copy() {
        TimeStampedMessage ret = new TimeStampedMessage(super.getDst(), super.getKind(), super.getData());
        ret.setSeqNum(super.getSeqNum());
        ret.setDuplicate(true);
        ret.setSource(super.getSrc());
        ret.setCC(super.getCC());
        ret.setTimeStamp(timeStamp);

        return ret;
    }

    public String toString() {
        String ret = super.toString();
        if (timeStamp != null) {
            ret += "Timestamp: "  + timeStamp.toString() + "\n";
        }
        return ret;
     }

    private TimeStamp timeStamp = null;
}
