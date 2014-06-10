package MessagePasser;
/*
    File: 
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: 
*/

import java.util.Iterator;
import java.util.Map;

public class MutexMultiMessage extends GroupMessage {

    public MutexMultiMessage(String group, String kind) {
        super(group, kind, "");
    }

    public MutexMultiMessage copy() {
        MutexMultiMessage m = new MutexMultiMessage(getGroupName(), getKind());
        m.setSource(getSrc());
        m.setDst(getDst());
        m.setCC(getCC());
        m.setSeqNum(getSeqNum());
        m.setTimeStamp(getTimeStamp());
        m.setGroupStamp(getGroupStamp());
        m.setNack(getNack());
        m.setNackSrc(getNackSrc());
        m.setDuplicate(true);
        m.setMutexAction(mutexAction);
        Iterator iter = getAckMap().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Integer> pair = (Map.Entry) iter.next();
            m.addAck(pair.getKey(), pair.getValue());
        }
        return m;
    }

    public Mutex_type getMutexAction() {
        return mutexAction;
    }

    public void setMutexAction(Mutex_type action) {
        mutexAction = action;
    }

    Mutex_type mutexAction = Mutex_type.none;
}
