package MessagePasser;

public class VectorClock extends ClockService {
    int[] timestamp;
    int k;
    
    public VectorClock(int totalHosts, int id) {
        timestamp = new int[totalHosts]; 
        k = id;
    }
    
    public void addTimestamp(TimeStampedMessage msg) {
        synchronized (timestamp) {
            msg.vectorTime = new int[timestamp.length];
            ++timestamp[k];
            
            for (int i = 0; i < timestamp.length; ++i)
                msg.vectorTime[i] = timestamp[i];
        }
    }
    
    public int[] getTimestamp() {
        synchronized (timestamp) {        
            int[] ret = new int[timestamp.length];
            ++timestamp[k];
            
            for (int i = 0; i < timestamp.length; ++i)
                ret[i] = timestamp[i];
            return ret;
        }
    }
    
    public void setTime(TimeStampedMessage msg) {
        synchronized (timestamp) {
            for (int i = 0; i < timestamp.length; ++i) {
                timestamp[i] = Math.max(timestamp[i], msg.vectorTime[i]);
                if (i == k)
                    ++timestamp[i];
                
                msg.vectorTime[i] = timestamp[i];
            }
        }
    }
}
