package MessagePasser;

/*
    File: ClockService.java
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: lab3 clock service interface
 */

import java.util.Set;

interface ClockService {

    //Copies current count into a timestamp and returns it
    public TimeStamp getTimeStamp();

    //Increments a counter and returns the modified timestamp
    public TimeStamp incrementAndGet(String name);

    //Temporarily return timestamp with the increment, but do not reflect changes internally
    public TimeStamp tempIncrementAndGet(String name);

    //Increments the timestamp counter
    public void increment(String name);

    //Timestamp to update internals to
    public void updateTime(TimeStamp src, String name) throws TimeStampException;

    //Initialize contents
    public void initNodes(Set<String> nodes);

    //add a single node
    public void addNode(String node);
}
