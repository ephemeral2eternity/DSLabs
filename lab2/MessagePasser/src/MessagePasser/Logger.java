package MessagePasser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;

public class Logger {

//	static Queue<TimeStampedMessage> logMsgVector = new LinkedList<TimeStampedMessage>();
	static MessagePasser passer;
	
	// Method to process the message and push log into log vector
	
	// Method to dump log vector, inspect the logs in log vector and show which events are concurrent
	public static void logDumper() throws InterruptedException
	{
	    System.out.println("Dumping...");
	    Boolean isLogic = false;
	    TimeStampedMessage tmpMsg;
	    CompareLogic cmpLogic = new CompareLogic();
	    
		Map<String, int[]> vecLogs = new TreeMap<String, int[]>();
		Map<String, int[]> sortedVecLogs;
		Map<String, Integer> logicLogs = new TreeMap<String, Integer>();
		Map<String, Integer> sortedLogicLogs;
		Hashtable<String, TimeStampedMessage> logsMsgs = new Hashtable<String, TimeStampedMessage>();
		
		while (passer.canRecv())
		{
		    tmpMsg = (TimeStampedMessage) passer.receive();
			logsMsgs.put(tmpMsg.getSource() + "-" + tmpMsg.getSequenceNo(), tmpMsg);
			isLogic = tmpMsg.isLogical;
			
		    if (isLogic)
		    {
		    	logicLogs.put(tmpMsg.getSource()  + "-" +  tmpMsg.getSequenceNo(), tmpMsg.logicalTime);
		    }
		    else
		    {
		    	vecLogs.put(tmpMsg.getSource()  + "-" +  tmpMsg.getSequenceNo(), tmpMsg.vectorTime);
		    }
		}
		
		if (!logsMsgs.isEmpty())
		{
			if (isLogic)
			{
				sortedLogicLogs = sortLogicLogs(logicLogs);
				printLogicLogs(sortedLogicLogs);
			}
			else
			{
				sortedVecLogs = sortVecLogs(vecLogs);
				printVecLogs(sortedVecLogs);
				
				compareEvents(sortedVecLogs);
			}
		}
		logsMsgs.clear();
		vecLogs.clear();
		logicLogs.clear();
	}

	public static void printLogicLogs(Map<String, Integer> sortedLogicLogs) {
		System.out.println("[Warnings]The sequence of the events sorted by logical timestamp do not "
				+ "definitly indicate causal sequence of events!");
		System.out.println("[Warnings]If we sort events according to logical timestamp,  list can be seen as follows:");
		
        for (Map.Entry<String, Integer> entry : sortedLogicLogs.entrySet()) {
            System.out.println("Event " + entry.getKey() + ", Logical TimeStamp = " +
                               entry.getValue());
        }
	}
	
	public static void printVecLogs(Map<String, int[]> sortedVecLogs) {
		
		System.out.println("The Sorted Events according to Vector TimeStamp can be shown as follows:");
		Iterator<Map.Entry<String, int[]>> entries =
				sortedVecLogs.entrySet().iterator();
		Map.Entry<String, int[]> cur_entry;
		Map.Entry<String, int[]> pre_entry = null;
	        while (entries.hasNext()) {
	            cur_entry = entries.next();
	            System.out.println("Event " + cur_entry.getKey() + ", Vector TimeStame = " +
	            		intArray2String(cur_entry.getValue()));
	        }
	}

	public static Map<String, Integer> sortLogicLogs(Map<String, Integer> logicLogs) {		
	    CompareLogic cmpLogical = new CompareLogic();
	    
	    LinkedList<Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(logicLogs.entrySet());
	    Collections.sort(list, cmpLogical);
	    
	    Map<String, Integer> result = new LinkedHashMap();
        for (Map.Entry<String, Integer> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
		return result;
	}

	// Method to sort all the vector logs.
	public static Map<String, int[]> sortVecLogs(Map<String, int[]> logs)
	{	
	    CompareVec cmpVec = new CompareVec();
	    
	    LinkedList<Entry<String, int[]>> list = new LinkedList<Map.Entry<String, int[]>>(logs.entrySet());
	    Collections.sort(list, cmpVec);
	    
	    Map<String, int[]> result = new LinkedHashMap();
        for (Map.Entry<String, int[]> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
		return result;
	}

	public static void compareEvents(Map<String, int[]> logs) {
		
		LinkedList<Entry<String, int[]>> list = new LinkedList<Map.Entry<String, int[]>>(logs.entrySet());
		ArrayList<String[]> concurrentLogs = new ArrayList<String[]>();
		ArrayList<String[]> causalLogs = new ArrayList<String[]>();
		
		CompareVec cmpVecObj = new CompareVec();

		// Find all causal edges and concurrent pairs.
		for (int i = 0; i < logs.size(); i ++)
		{
			Map.Entry<String, int[]> e1 = list.get(i);
			for (int j = i + 1; j < logs.size(); j ++)
			{
				Map.Entry<String, int[]> e2 = list.get(j);
				if (cmpVecObj.compare(e1, e2) == 0)
				{
					String[] str = {e1.getKey(), e2.getKey()};
					concurrentLogs.add(str);
				}
				else if(cmpVecObj.compare(e1, e2) == -1)
				{
					String[] str = {e1.getKey(), e2.getKey()};
					causalLogs.add(str);
				}
				else if(cmpVecObj.compare(e1, e2) == 1)
				{
					String[] str = {e2.getKey(), e1.getKey()};
					causalLogs.add(str);	
				}
				
			}
		}
		
		printLogs(logs, concurrentLogs, "Concurrent");
		printLogs(logs, causalLogs, "Causal");
	}
	
	public static void printLogs(Map<String, int[]> logs, ArrayList<String[]> pairs, String type)
	{	
		String srcVertex, destVertex;
		int[] srcTS, destTS;
		String symbol = "";
		if (type.equals("Causal"))
		{
			symbol = " ---> ";
		}
		else if (type.equals("Concurrent"))
		{
			symbol = "||";
		}
		
		for (int i = 0; i < pairs.size(); i ++)
		{
			srcVertex = pairs.get(i)[0];
			destVertex = pairs.get(i)[1];
			srcTS = logs.get(srcVertex);
			destTS = logs.get(destVertex);
			System.out.println("Relations : " + srcVertex + symbol + destVertex
					         + " (" + intArray2String(srcTS) + symbol + intArray2String(destTS)+ ")");
		}
	}
	
	static String intArray2String(int[] array)
	{
		String str = "<";
		for (int i = 0; i < array.length - 1; i ++)
		{
			str = str + array[i] + ", ";
		}
		str = str + array[array.length - 1] + ">";
		return str;
	}
	
	public static void main(String[] args) throws InterruptedException {
	    String configFilename = null;
        String localName = null;
        
        if (args.length != 1)
            System.err.println("Please input Config File Name:");
        
        else {
            configFilename = args[0];
            localName = "Logger";
            System.out.println("Config File Name [" + configFilename +
                                "] and Local Name [" + localName + "] read from arguments.");
        }
        
        if( configFilename == null || localName == null ) System.exit(-1);
        
        passer = new MessagePasser(configFilename, localName);
        
        Scanner scanner = new Scanner(System.in);
                        
        while (true) {
            String cmd = scanner.nextLine();
            if (cmd.startsWith("d"))
                logDumper();
        }
	}
}

class CompareLogic implements Comparator<Map.Entry<String,Integer>> {
    public int compare(Map.Entry<String,Integer> e1, Map.Entry<String,Integer> e2) {
        if (e1.getValue() < e2.getValue()){
            return -1;
        } else if (e1.getValue() == e2.getValue()) {
            return 0;
        } else {
            return 1;
        }
    }
}

class CompareVec implements Comparator<Map.Entry<String, int[]>> {
    public int compare(Map.Entry<String, int[]> e1, Map.Entry<String, int[]> e2) {
        if (cmpVec(e1.getValue(), e2.getValue())){
            return -1;
        } else if (cmpVec(e2.getValue(), e1.getValue())) {
            return 1;
        } else{
            return 0;
        }
    }
    
	static boolean cmpVec(int[] tsVec, int[] otherVec) {
		for (int i = 0; i < tsVec.length; i ++)
		{
			if (tsVec[i] > otherVec[i])
			{
				return false;
			}
		}
		return true;
	}
}