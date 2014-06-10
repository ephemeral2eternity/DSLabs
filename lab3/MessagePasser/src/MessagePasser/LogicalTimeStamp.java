/*
    File: LogicalTimeStamp.java
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: Lab1 logical timestamp implementation
*/

package MessagePasser;

import java.io.Serializable;

public class LogicalTimeStamp implements TimeStamp, Serializable {

    public LogicalTimeStamp(int init) {
        time = init;
    }

    public int compare(TimeStamp tgt) throws TimeStampException {
        if (!(tgt instanceof LogicalTimeStamp)) {
            //comparing to a different type
            throw new TimeStampException("Comparing incompatible timestamp types");
        }

        if (time > ((LogicalTimeStamp) tgt).getTime()) {
            return 1;
        }

        else if (time < ((LogicalTimeStamp) tgt).getTime()) {
            return -1;
        }
        else {
            //the two timestamps have the same time, we don't know which happened first
            throw new TimeStampException("Unable to determine order");
        }
    }

    public int getTime() {
        return time;
    }

    public String toString() {
        return Integer.toString(time);
    }


    private int time;
}
