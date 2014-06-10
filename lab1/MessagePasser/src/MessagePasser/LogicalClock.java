package MessagePasser;

public class LogicalClock extends ClockService {
    private static Integer timestamp;
    
    public LogicalClock() {
        timestamp = 0;
    }
    
    public void addTimestamp(TimeStampedMessage msg) {
        synchronized (timestamp) {
            ++timestamp;
            msg.logicalTime = timestamp;            
        }       
    }
    
    public int getTimestamp() {
        synchronized (timestamp) {
            ++timestamp;
            int ret = timestamp;            
            return ret;
        }
    }
    
    public void setTime(TimeStampedMessage msg) {
        synchronized (timestamp) {
            timestamp = Math.max(timestamp, msg.logicalTime) + 1;
            msg.logicalTime = timestamp;
        }
    }
}
