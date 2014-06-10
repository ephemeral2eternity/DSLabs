package MessagePasser;

public class TimeStampedMessage extends Message {
    private static final long serialVersionUID = 1L;

    public int logicalTime;
    public int[] vectorTime;
    
    public boolean isLogical = false;
    
    public TimeStampedMessage(String destUser, String msgKind, Object msgData) {
        super(destUser, msgKind, msgData);
        
        if (MessagePasser.getClockType() == 1)
            isLogical = true;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (Exception e) {
            e.printStackTrace();
            return this;
        }
    }
}