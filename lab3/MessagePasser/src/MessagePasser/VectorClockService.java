package MessagePasser;

/*
    File: VectorClockService.java
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: Vector clock implementation of clockservice
*/

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VectorClockService implements ClockService {

    public TimeStamp getTimeStamp() {
        VectorTimeStamp ret = null;
        synchronized (count) {
            ret = new VectorTimeStamp(count);
        }
        return ret;
    }

    public TimeStamp incrementAndGet(String name) {
        VectorTimeStamp ret = null;
        synchronized (count) {
            Integer val = count.get(name);
            if (val != null) {
                count.put(name, val+1);
            }
            else {
                count.put(name, 1);
            }
            ret = new VectorTimeStamp(count);
        }
        return ret;
    }

    public TimeStamp tempIncrementAndGet(String name) {
        VectorTimeStamp ret = null;
        synchronized (count) {
            Integer val = count.get(name);
            if (val != null) {
                count.put(name, val+1);
                ret = new VectorTimeStamp(count);
                count.put(name, val);
            }
            else {
                count.put(name, 1);
                ret = new VectorTimeStamp(count);
                count.put(name, 0);
            }
        }
        return ret;
    }

    public void increment(String name) {
        synchronized (count) {
            Integer val = count.get(name);
            if (val != null) {
                count.put(name, val+1);
            }
            else {
                count.put(name, 1);
            }
        }
    }

    public synchronized void updateTime(TimeStamp src, String name) throws TimeStampException {
        if (!(src instanceof VectorTimeStamp)) {
            throw new TimeStampException("Mismatched timestamp type");
        }

        //increment our own time on receive
        increment(name);

        //for each entry in the map, find the greater between local and new
        synchronized (count) {
            Set<String> keys = new HashSet<String>(count.keySet());
            keys.addAll(((VectorTimeStamp) src).getKeys());
            for (String s : keys) {
                Integer i = count.get(s);
                Integer j = ((VectorTimeStamp) src).getValue(s);

                if (j != null && i != null) {
                    if (i < j) count.put(s, j);
                }
                else if (j != null && i == null) {
                    count.put(s, j);
                }
            }
        }
    }

    //update time without incrementing our timestamp
    public synchronized void maxTime(VectorTimeStamp src) {

        //for each entry in the map, find the greater between local and new
        synchronized (count) {
            Set<String> keys = new HashSet<String>(count.keySet());
            keys.addAll(src.getKeys());
            for (String s : keys) {
                Integer i = count.get(s);
                Integer j = src.getValue(s);

                if (j != null && i != null) {
                    if (i < j) count.put(s, j);
                }
                else if (j != null && i == null) {
                    count.put(s, j);
                }
            }
        }
    }

    public void initNodes(Set<String> nodes) {
        for (String s : nodes) {
            count.put(s, 0);
        }
    }

    public void addNode(String node) {
        count.put(node, 0);
    }

    private ConcurrentHashMap<String, Integer> count = new ConcurrentHashMap<String, Integer>();
}
