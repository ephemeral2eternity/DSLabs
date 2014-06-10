package MessagePasser;
/*
    File: 
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: 
*/

public class MutexMessage extends GroupMessage {

    public MutexMessage(String group, String kind) {
        super(group, kind, "");
    }

    public LogicalTimeStamp getMutexTimeStamp() {
        return mutexTimeStamp;
    }

    public Mutex_action getMutexAction() {
        return mutexAction;
    }

    public void setMutexTimeStamp(LogicalTimeStamp stamp) {
        mutexTimeStamp = stamp;
    }

    public void setMutexAction(Mutex_action action) {
        mutexAction = action;
    }

    Mutex_action mutexAction = Mutex_action.none;
    LogicalTimeStamp mutexTimeStamp = null;
}
