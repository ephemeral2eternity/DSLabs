package MessagePasser;

abstract public class ClockService {
    abstract public void addTimestamp(TimeStampedMessage msg);    
    
    abstract public void setTime(TimeStampedMessage msg);
}
