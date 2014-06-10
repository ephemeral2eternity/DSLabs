/*
    File: TimeStamp.java
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: Lab1 abstract timestamp, override with vector or logical
*/

package MessagePasser;

interface TimeStamp {

    //This function should return 0 on same time (or exception for logical),
    //1 if this is later, -1 if earlier, 5 if concurrent
    public int compare(TimeStamp tgt) throws TimeStampException;

    public String toString();
}
