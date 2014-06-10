package MessagePasser;

/*
    File: LogicalClockService.java
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: Logical clock implementation of clockservice
*/

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class LogicalClockService implements ClockService {

    public TimeStamp getTimeStamp() {
        return new LogicalTimeStamp(count.get());
    }

    public TimeStamp incrementAndGet(String name) {
        return new LogicalTimeStamp(count.incrementAndGet());
    }

    public TimeStamp tempIncrementAndGet(String name) {
        return new LogicalTimeStamp(count.get() + 1);
    }

    public void increment(String name) {
        count.incrementAndGet();
    }

    public synchronized void updateTime(TimeStamp src, String name) throws TimeStampException {
        if (!(src instanceof LogicalTimeStamp)) {
            throw new TimeStampException("Mismatched timestamp type");
        }

        int remote = ((LogicalTimeStamp) src).getTime();

        if (remote <=  count.get()) {
            // internal clock is ahead of received
            count.incrementAndGet();
        }
        else {
            // received occurred after internal clock
            count.set(remote+1);
        }
    }

    public void initNodes(Set<String> nodes) {
        return;
    }

    public void addNode(String node) {
        return;
    }

    private AtomicInteger count = new AtomicInteger();
}
