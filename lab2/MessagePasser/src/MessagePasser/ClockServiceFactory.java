package MessagePasser;

public class ClockServiceFactory {
    public static ClockService createClockService(String type) {
        if (type.equals("logical"))
            return new LogicalClock();
        else if (type.equals("vector"))
            return new VectorClock(MessagePasser.totalHosts, MessagePasser.id);
        else
            throw new IllegalArgumentException("No such ClockService");
    }
}
