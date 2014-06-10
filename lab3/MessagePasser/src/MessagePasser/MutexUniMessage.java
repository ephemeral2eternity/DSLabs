package MessagePasser;
/*
    File: 
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: 
*/

public class MutexUniMessage extends TimeStampedMessage {

    public MutexUniMessage(String dest, String kind) {
        super(dest, kind, "");
    }

    public MutexUniMessage copy() {
        MutexUniMessage m = new MutexUniMessage(getDst(), getKind());
        m.setSeqNum(getSeqNum());
        m.setDuplicate(true);
        m.setSource(getSrc());
        m.setCC(getCC());
        m.setTimeStamp(getTimeStamp());
        m.setMutexAction(mutexAction);

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
