package MessagePasser;

import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

public class TestLogger {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		/*int[][] dat = new int[][]{{1, 0, 0},
				                {1, 1, 0},
				                {2, 0, 0},
				                {1, 2, 0},
				                {1, 2, 1},
				                {3, 0, 0},
				                {3, 2, 0},
				                {2, 2, 2}};
		
		Map<String, int[]> logs = new TreeMap<String, int[]>();
		
		logs.put("a0", dat[0]);
		logs.put("b0", dat[1]);
		logs.put("a1", dat[2]);
		logs.put("b1", dat[3]);
		logs.put("c0", dat[4]);
		logs.put("a2", dat[5]);
		logs.put("b2", dat[6]);
		logs.put("c1", dat[7]);
		
		Logger.sortVecLogs(logs);
		
		Map<String, int[]> sortedVecLogs;
		sortedVecLogs = Logger.sortVecLogs(logs);
		Logger.printVecLogs(sortedVecLogs);
		
		Logger.compareEvents(sortedVecLogs);*/
		
		// Logical Vector Tests.
		Map<String, Integer> logs = new TreeMap<String, Integer>();
		
		logs.put("a0", 1);
		logs.put("a1", 8);
		logs.put("b0", 2);
		logs.put("b1", 3);
		logs.put("b2", 6);
		logs.put("b3", 7);
		logs.put("c0", 4);
		logs.put("c1", 5);
		
		Map<String, Integer> sortedLogs = Logger.sortLogicLogs(logs);
		Logger.printLogicLogs(sortedLogs);

	}

}
