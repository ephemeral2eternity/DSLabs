/*
    File: VectorTimeStamp.java
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: Lab1 vector timestamp
*/

package MessagePasser;

import java.io.Serializable;
import java.util.*;


public class VectorTimeStamp implements TimeStamp, Serializable {

    public VectorTimeStamp(Map<String, Integer> currentMap) {
        timeMap = new HashMap<String, Integer>(currentMap);
    }

    public int compare(TimeStamp tgt) throws TimeStampException {
        boolean greater = false;
        boolean less = false;
        boolean equal = true;
        if (!(tgt instanceof VectorTimeStamp)) {
            //comparing to a different type
            throw new TimeStampException("Comparing incompatible timestamp types");
        }

        HashSet<String> allKeys = new HashSet<String>(timeMap.keySet());
        allKeys.addAll(((VectorTimeStamp) tgt).getKeys());

        int local;
        int remote;
        for (String key : allKeys) {
            local = this.getValue(key);
            remote = ((VectorTimeStamp) tgt).getValue(key);

            if (local < remote) {
                equal = false;
                less = true;
            }
            else if (local > remote) {
                greater = true;
                equal = false;
            }
        }

        if (equal) {
            //all values were equal to each other
            return 0;
        }
        else if (less && !greater) {
            //all of our values were less or equal to theirs
            return -1;
        }
        else if (!less && greater) {
            //all of our values were greater or equal to theirs
            return 1;
        }
        else {
            //not equal, and both less and greater are true
            //concurrent case
            return 0;
        }

    }
    
    public boolean compareCausalOrder(VectorTimeStamp timeStamp, String src) {
        if (!timeMap.containsKey(src)) {
            return false;
        }
        
        if (timeMap.get(src) + 1 != timeStamp.getValue(src)) {
            return false;
        }
        
        HashSet<String> allKeys = new HashSet<String>(timeMap.keySet());
        allKeys.remove(src);
        for (String key: allKeys) {
            if (timeMap.get(key) < timeStamp.getValue(key)) {
                return false;
            }
        }
        return true;
    }

    public Set<String> getKeys() {
        return Collections.unmodifiableSet(timeMap.keySet());
    }

    public Collection<Integer> getValues() {
        return Collections.unmodifiableCollection(timeMap.values());
    }

    public int getValue(String key) {
        Integer ret = timeMap.get(key);
        if (ret != null) {
            return ret.intValue();
        }
        else {
            return 0;
        }
    }

    public String toString() {
        StringBuilder out = new StringBuilder("[ ");
        for (String s : timeMap.keySet()) {
            out.append(s);
            out.append(":");
            out.append(timeMap.get(s));
            out.append(" ");
        }

        out.append("]");
        return out.toString();
    }

    private Map<String, Integer> timeMap;
}
