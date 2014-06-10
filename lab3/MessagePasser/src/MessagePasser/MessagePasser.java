/*
    File: MessagePasser.java
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: Lab1 messagepasser with timestamp extensions
*/

package MessagePasser;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MessagePasser {

	public MessagePasser(String config_file, String local_name, String clock_type)
            throws FileNotFoundException, IllegalArgumentException {
        InputStream inp = null;
        Map cfg = null;
        name = local_name;
        config_path = config_file;
        seqNum = new AtomicInteger(1);
        configModTime = 0;

        //Initialize queues here
        sendQueue = new ConcurrentLinkedQueue<Message>();
        receiveQueue = new LinkedBlockingQueue<Message>();
        delayRQueue = new ConcurrentLinkedQueue<Message>();
        delaySQueue = new ConcurrentLinkedQueue<Message>();

        //Initialize rule vectors, populate during parsing
        incoming = Collections.synchronizedList(new ArrayList<Rule>());
        outgoing = Collections.synchronizedList(new ArrayList<Rule>());

        //hashmap for nodes
        nodes = new ConcurrentHashMap<String, Node>();
        //hashmap for groups
        groups = new ConcurrentHashMap<String, Group>();

        //populate nodes/incoming/outgoing with parsed config values
        try {
            File tmp = new File(config_path);
            configModTime = tmp.lastModified();
            inp = new FileInputStream(tmp);
            Yaml yaml = new Yaml();
            cfg = (Map) yaml.load(inp);
        } finally {
            try {
                inp.close();
            } catch (IOException e) {

            }
        }

        if (cfg != null) {
            if (cfg.containsKey("configuration")) {
                if (cfg.get("configuration") instanceof ArrayList) {
                    parseConfiguration(
                            (ArrayList<LinkedHashMap<String, Object>>) cfg.get("configuration"));
                }
            }

            if (cfg.containsKey("groups")) {
                if (cfg.get("groups") instanceof ArrayList) {
                    parseGroups(
                            (ArrayList<LinkedHashMap<String, Object>>) cfg.get("groups"));
                }
            }

            if (cfg.containsKey("sendRules")) {
                if (cfg.get("sendRules") instanceof ArrayList) {
                    parseOutRules(
                            (ArrayList<LinkedHashMap<String, Object>>) cfg.get("sendRules"));
                }
            }

            if (cfg.containsKey("receiveRules")) {
                if (cfg.get("receiveRules") instanceof ArrayList) {
                    parseInRules(
                            (ArrayList<LinkedHashMap<String, Object>>) cfg.get("receiveRules"));
                }
            }
        }

        //now that we have parsed out the config, check input name
        if (!(nodes.containsKey(name))) {
            throw new IllegalArgumentException("Invalid name input");
        }

        //Initialize clock type
        if (clock_type != null) {
            clockService = ClockServiceFactory.getService(clock_type);
            if (clockService != null) {
                clockService.initNodes(nodes.keySet());
            }
        }

        //Start listening on config port
        listen();
    }

    public MessagePasser(String config_file, String local_name)
            throws FileNotFoundException, IllegalArgumentException {
        this(config_file, local_name, null);
    }
    
    public String getName() {
		return name;
	}

    //get group which this node belongs to
    public String getNodeGroup(String node) {
        if (nodes.containsKey(node)) {
            return nodes.get(node).getMemberOf();
        }
        else {
            return null;
        }
    }

    public Set<String> getGroupMembers(String tgt) {
        if (groups.containsKey(tgt)) {
            return groups.get(tgt).getMembers();
        }
        else {
            return null;
        }
    }

    synchronized void updateRules() {
        InputStream inp = null;
        Map cfg = null;
        File config_file = new File(config_path);

        if (config_file.lastModified() == configModTime) {
            return;
        }

        configModTime = config_file.lastModified();

        try {
            inp = new FileInputStream(config_file);
            Yaml yaml = new Yaml();
            cfg = (Map) yaml.load(inp);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                inp.close();
            } catch (IOException e) {

            }
        }

        synchronized (incoming) {
            synchronized (outgoing) {
                if (cfg != null) {
                    incoming.clear();
                    outgoing.clear();
                    if (cfg.containsKey("sendRules")) {
                        if (cfg.get("sendRules") instanceof ArrayList) {
                            parseOutRules(
                                    (ArrayList<LinkedHashMap<String, Object>>) cfg.get("sendRules"));
                        }
                    }

                    if (cfg.containsKey("receiveRules")) {
                        if (cfg.get("receiveRules") instanceof ArrayList) {
                            parseInRules(
                                    (ArrayList<LinkedHashMap<String, Object>>) cfg.get("receiveRules"));
                        }
                    }
                }
            }
        }
    }

    /* public send/receive methods */

    synchronized boolean send(Message message) {

        if ((message.getDst() == null) || (nodes.get(message.getDst()) == null)) {
            return false;
        }

        //update rules
        updateRules();

        //check the initializable fields, initialize any unitialized ones
        if (message.getSrc() == null) {
            message.setSource(name);
        }

        if (clockService != null) {
            if (message instanceof TimeStampedMessage
                    && ((TimeStampedMessage) message).getTimeStamp() == null) {
                ((TimeStampedMessage) message).setTimeStamp(clockService.incrementAndGet(name));
            }
        }

        if (message.getSeqNum() == -2) {
            message.setSeqNum(seqNum.getAndIncrement());
        }

        //check rules in order we added them, exit on first match
        message.setAction(checkOutgoingRules(message));
        switch (message.getAction()) {
            case none:
                sendQueue.add(message);
                break;
            case duplicate:
                sendQueue.add(message);
                sendQueue.add(message.copy());
                break;
            case delay:
                delaySQueue.add(message);
                return false;
            case drop:
                return false;
            default:
                break;
        }

        internalSend();

        return true;
    }

    synchronized boolean sendGroup(GroupMessage message) {
        Set<String> dests = null;
        List<Message> output = null;
        if (!groups.containsKey(message.getGroupName())) {
            //invalid group
            return false;
        }
        Group g = groups.get(message.getGroupName());
        //return if group or member doesnt exist
        if (g == null  || (g.getMember(name) == null)) {
            return false;
        }

        dests = g.getMembers();
        output = new LinkedList<Message>();

        // update rules
        updateRules();
        
        // set clock if this isnt a nack
        if (!message.getNack()) {
            message.setGroupStamp(g.incrementAndGetTimeStamp(name));
        }

        // set parent timestamp
        if (clockService != null) {
            if (message instanceof TimeStampedMessage
                    && message.getTimeStamp() == null) {
                message.setTimeStamp(clockService.incrementAndGet(name));
            }
        }
        
        //set source
        if (message.getSrc() == null) {
            message.setSource(name);
        }

        //if this isnt a nack, assign it a sequence number
        if (!message.getNack()) {
            message.setSeqNum(g.incrementAndGetSeqNum());
        }

        //set up acks list, ack latest received for all nodes
        for (String s : dests) {
            GroupNode gn;
            if ((gn = g.getMember(s)) != null) {
                message.addAck(s, gn.getSeqNum());
            }
        }

        //check rules in order we added them, exit on first match
        for (String s : dests) {
            GroupMessage tmsg = message.copy();
            tmsg.setDst(s);
            tmsg.setDuplicate(false);
            tmsg.setAction(checkOutgoingRules(tmsg));
            switch (tmsg.getAction()) {
                case none:
                    output.add(tmsg);
                    break;
                case duplicate:
                    output.add(tmsg);
                    output.add(tmsg.copy());
                    break;
                case delay:
                    output.add(tmsg);
                    break;
                default:
                    break;
            }
        }

        //at this point we have the list of messages with actions, dests, and acks set
        if (output.size() > 0) {
            Iterator<Message> iter = output.iterator();
            while (iter.hasNext()) {
                Message m = iter.next();
                if (m.getAction() != Rule_action.delay) {
                    sendQueue.add(m);
                    iter.remove();
                }
            }

            //all messages are in queue that should be sent, call internal
            internalSend();

            //we come back from send, remainders of output should go into delay queue
            for (Message m: output) {
                delaySQueue.add(m);
            }
        }

        return true;
    }

    Message receive() {
        return receiveQueue.poll();
    }

    Message blockingReceive() {
        Message m;
        try {
            m =  receiveQueue.take();
        } catch (InterruptedException e) {
            m = null;
        }
        return m;
    }

    /* internal rule checking methods */

    private Rule_action checkOutgoingRules(Message message) {
        boolean rule_matched = false;
        Rule_action ret = Rule_action.none;

        synchronized (outgoing) {
            for (Rule rule : outgoing) {
                Rule_action chk = rule.checkMessage(message);
                switch(chk) {
                    case drop:
                        rule_matched = true;
                        ret = Rule_action.drop;
                        break;
                    case duplicate:
                        rule_matched = true;
                        ret = Rule_action.duplicate;
                        break;
                    case delay:
                        rule_matched = true;
                        ret = Rule_action.delay;
                        break;
                    default:
                        break;
                }

                if (rule_matched == true) {
                    break;
                }
            }
        }

        return ret;
    }

    private Rule_action checkIncomingRules(Message message) {
        boolean rule_matched = false;
        Rule_action ret = Rule_action.none;

        synchronized (incoming) {
            for (Rule rule : incoming) {
                Rule_action chk = rule.checkMessage(message);
                switch(chk) {
                    case drop:
                        rule_matched = true;
                        ret = Rule_action.drop;
                        break;
                    case duplicate:
                        rule_matched = true;
                        ret = Rule_action.duplicate;
                        break;
                    case delay:
                        rule_matched = true;
                        ret = Rule_action.delay;
                        break;
                    default:
                        break;
                }

                if (rule_matched == true) {
                    break;
                }
            }
        }

        return ret;
    }

    /* Internal sending/receiving methods */

    private synchronized void internalSend() {
        Message msg;
        Node n;

        while ((msg = sendQueue.poll()) != null) {
            //node destinations checked during send()
            n = nodes.get(msg.getDst());
            n.addQueue(msg);

            if (msg.getCC() != null && ((n = nodes.get(msg.getCC())) != null)) {
                n.addQueue(msg);
            }
        }

        //if we successfully added something to queue then clear the delay queue
        while ((msg = delaySQueue.poll()) != null) {
            //node destinations checked during send()
            n = nodes.get(msg.getDst());
            n.addQueue(msg);

            if (msg.getCC() != null && ((n = nodes.get(msg.getCC())) != null)) {
                n.addQueue(msg);
            }
        }


        //Now that we have distributed all the messages, send everything out
        for (Node nd : nodes.values()) {
            if (nd.hasQueue() && nd.sendQueue() == false) {
                //failed to send, try a reconnect and send again once
                nd.reconnect();
                nd.sendQueue();
            }
        }
    }

    private synchronized void internalReceive(Message message) {
        Message msg = null;
        List<Message> internalList = new LinkedList<Message>();

        //update rules
        updateRules();

        //we received a message, update our internal timestamp if it exists
        if (clockService != null && message instanceof TimeStampedMessage) {
            try {
                clockService.updateTime(((TimeStampedMessage) message).getTimeStamp(), name);
            } catch (TimeStampException e) {
                //we ran into some error with this timestamp, just ignore it since we cant inform user
            }
        }

        //check rules
        switch (checkIncomingRules(message)) {
            case none:
                internalList.add(message);
                break;
            case duplicate:
                internalList.add(message);
                internalList.add(message.copy());
                break;
            case delay:
                delayRQueue.add(message);
                return;
            case drop:
                return;
            default:
                break;
        }

        for (Message m : internalList) {
            if (m instanceof GroupMessage) {
                if (m instanceof MutexMultiMessage) {
                    groupReceive((MutexMultiMessage) m);
                }
                else {
                    groupReceive((GroupMessage) m);
                }
            }
            else {
                receiveQueue.add(m);
            }
        }

        //since we successfully received a message, move the delay queue over
        while ((msg = delayRQueue.poll()) != null) {
            if (msg instanceof GroupMessage) {
                groupReceive((GroupMessage) msg);
            }
            else {
                receiveQueue.add(msg);
            }
        }

        deliverGroupMessage();
    }

    private synchronized void groupReceive(GroupMessage message) {
        if (!groups.containsKey(message.getGroupName())) {
            //invalid group
            return;
        }
        Group g = groups.get(message.getGroupName());
        if (g == null) {
            return;
        }

        GroupNode gn = g.getMember(message.getSrc());

        //check if this is a nack, if so then unicast to that node
        if (message.getNack()) {
            GroupMessage m = gn.getHoldbackMsg(message.getSeqNum());
            if (m != null) {
                //we still have the message saved, send it to the nacker
                m.setDst(message.getNackSrc());
                send(m);
            }
            return;
        }

        //this isnt a nack,check its sequence number and acks
        if (message.getSeqNum() <= gn.getSeqNum()) {
            //we have already seen this message
            return;
        }
        else if (message.getSeqNum() == (gn.getSeqNum() + 1)) {
            //latest thing we have seen from this node
            gn.setSeqNum(message.getSeqNum());
            gn.addHoldbackMsg(message.getSeqNum(), message);
        }
        else {
            //TODO: add case for missing intermediates
            gn.addHoldbackMsg(message.getSeqNum(), message);
        }

        int delay = 1;
        //check the acks for missing
        for (String member : g.getMembers()) {
            GroupNode gnloc = g.getMember(member);
            int local = gnloc.getSeqNum();
            int remote = message.getAck(member);
            if (remote > local) {
                for (int i = local + 1; i <= remote; i++) {
                    //send nacks for each missing message
                    //every time we send a nack, increment our delay by 1
                    if (!gnloc.hasHoldbackMsg(i)
                            && !gnloc.isNacked(i)) {
                        //we do not have this message in our holdback
                        delay++;
                        gnloc.addNacked(i, delay);
                        groupNack(message.getGroupName(), member, name, i);
                    }
                }
            }
        }
    }

    private synchronized void groupNack(String group, String member, String nacker, int index) {
        GroupMessage m = new GroupMessage(group, "nack", "");
        m.setSeqNum(index);
        m.setNack(true);
        m.setNackSrc(nacker);
        m.setSource(member);
        sendGroup(m);
    }
    
    // go through every group to check if can deliver a group message
    private synchronized void deliverGroupMessage() {
        Set<Map.Entry<String, Group>> groupSet = groups.entrySet();
        for (Map.Entry<String, Group> entry: groupSet) {
            Group group = entry.getValue();
            ArrayList<GroupMessage> toDeliverMsgs = group.deliverGroupMessage();
            if (!toDeliverMsgs.isEmpty()) {
                for (GroupMessage groupMessage: toDeliverMsgs) {
                    receiveQueue.add(groupMessage);
                }
            }
        }
    }

    /* Socket listener */

    private boolean listen() {
        //TODO: add more error handling
        Node n = nodes.get(name);
        if (n == null) {
            //cant find our connection info, bail out
            return false;
        }

        final int lport = n.getPort();

        Runnable listenTask = new Runnable() {
            @Override
            public void run() {
                try {
                    listener = new ServerSocket(lport);

                    while (true) {
                        Socket client = listener.accept();
                        ClientWorker worker = new ClientWorker(client);
                        Thread clientThread = new Thread(worker);
                        clientThread.start();
                    }
                } catch (IOException e) {
                    //failed to listen,need to handle this somehow
                    System.err.printf("Failed to listen on socket: %d", lport);
                }
            }
        };

        Thread serverThread = new Thread(listenTask);
        serverThread.start();
        return true;
    }

    /* Configuration Parsing methods */

    private void parseConfiguration(ArrayList<LinkedHashMap<String, Object>> config) {
        for (LinkedHashMap<String, Object> entry : config) {
            String name = null;
            String ip = null;
            Integer port = null;
            String memberOf = null;

            if (entry.get("name") instanceof String) {
                name = (String) entry.get("name");
            }

            if (entry.get("ip") instanceof String) {
                ip = (String) entry.get("ip");
            }

            if (entry.get("port") instanceof Integer) {
                port = (Integer) entry.get("port");
            }

            if (entry.get("memberOf") instanceof String) {
                memberOf = (String) entry.get("memberOf");
            }

            if (name != null && ip != null && port != null && memberOf != null) {
                Node n = null;
                try {
                    n = new Node(name, ip, port, memberOf);
                } catch (UnknownHostException e) {
                    //invalid host
                    n = null;
                }

                if (n != null) {
                    nodes.put(name, n);
                }
            }
        }
    }

    private void parseGroups(ArrayList<LinkedHashMap<String, Object>> groupList) {
        for (LinkedHashMap<String, Object> entry : groupList) {
            String name = null;
            ArrayList<String> members = null;

            if (entry.get("name") instanceof String) {
                name = (String) entry.get("name");
            }

            if (entry.get("members") instanceof ArrayList) {
                members = new ArrayList<String>();
                for (String s : (ArrayList<String>) entry.get("members")) {
                    members.add(s);
                }
            }

            if (name != null && members != null) {
                Group g = new Group(name);
                for (String s : members) {
                    g.addMember(s);
                }
                groups.put(name, g);
            }
        }
    }

    private synchronized void parseInRules(ArrayList<LinkedHashMap<String, Object>> rules) {
        for (LinkedHashMap<String, Object> entry : rules) {
            String action = null;
            String src = null;
            String dest = null;
            String kind = null;
            Integer seqNum = null;
            Boolean duplicate = null;

            if (entry.get("action") instanceof String) {
                action = (String) entry.get("action");
            }

            if (entry.get("src") instanceof String) {
                src = (String) entry.get("src");
            }
            
            if (entry.get("dest") instanceof String) {
                dest = (String) entry.get("dest");
            }
            
            if (entry.get("kind") instanceof String) {
                kind = (String) entry.get("kind");
            }

            if (entry.get("seqNum") instanceof Integer) {
                seqNum = (Integer) entry.get("seqNum");
            }

            if (entry.get("duplicate") instanceof Boolean) {
                duplicate = (Boolean) entry.get("duplicate");
            }

            Rule r = null;
            try {
                r = new Rule(action, src, dest, kind, seqNum, duplicate);
            } catch (IllegalArgumentException e) {
                r = null;
            }

            if (r != null) {
                incoming.add(r);
            }
        }
    }

    private synchronized void parseOutRules(ArrayList<LinkedHashMap<String, Object>> rules) {
        for (LinkedHashMap<String, Object> entry : rules) {
            String action = null;
            String src = null;
            String dest = null;
            String kind = null;
            Integer seqNum = null;
            Boolean duplicate = null;

            if (entry.get("action") instanceof String) {
                action = (String) entry.get("action");
            }

            if (entry.get("src") instanceof String) {
                src = (String) entry.get("src");
            }

            if (entry.get("dest") instanceof String) {
                dest = (String) entry.get("dest");
            }

            if (entry.get("kind") instanceof String) {
                kind = (String) entry.get("kind");
            }

            if (entry.get("seqNum") instanceof Integer) {
                seqNum = (Integer) entry.get("seqNum");
            }

            if (entry.get("duplicate") instanceof Boolean) {
                duplicate = (Boolean) entry.get("duplicate");
            }

            Rule r = null;
            try {
                r = new Rule(action, src, dest, kind, seqNum, duplicate);
            } catch (IllegalArgumentException e) {
                r = null;
            }

            if (r != null) {
                outgoing.add(r);
            }
        }
    }

    /* Private classes, support internal mechanisms */

    private class Node {

        public Node(String in_name, String in_addr, int in_port, String group) throws UnknownHostException {
            name = in_name;
            address = InetAddress.getByName(in_addr);
            port = in_port;
            connected = false;
            memberOf = group;
            nsendQueue = new ConcurrentLinkedQueue<Message>();
        }

        public InetAddress getAddress() {
            return address; //May need to copy this out instead of returning static object
        }

        public int getPort() {
            return port;
        }

        public String getMemberOf() {
            return memberOf;
        }

        public void setAddress(String in_addr) throws UnknownHostException {
            address = InetAddress.getByName(in_addr);
        }

        public void setPort(int in_port) {
            port = in_port;
        }

        public void addQueue(Message message) {
            nsendQueue.add(message);
        }

        //returns true if successful, false if failed
        public boolean connect() {
            try {
                socket = new Socket(address, port);
                msgOut = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                connected = false;
                return false;
            }
            connected = true;
            return true;
        }

        public void disconnect() {
            if (socket != null) {
                try {
                    socket.close();
                     if (msgOut != null) {
                         msgOut.close();
                     }
                } catch (IOException e) {

                }
            }
            connected = false;
            msgOut = null;
        }

        public boolean reconnect() {
            disconnect();
            return connect();
        }

        public boolean hasQueue() {
            if (nsendQueue != null) {
                return !nsendQueue.isEmpty();
            }
            return false;
        }

        public boolean sendQueue() {
            Message msg = null;
            if (connected == false || msgOut == null) {
                return false;
            }

            try {
                while ((msg = nsendQueue.poll()) != null) {
                    msgOut.writeObject(msg);
                }
                msgOut.flush();
            } catch (IOException e) {
                return false;
            }

            return true;
        }

        private String name;
        private InetAddress address;
        private int port;
        private Socket socket;
        private ObjectOutputStream msgOut;
        private boolean connected;
        private String memberOf;

        private Queue<Message> nsendQueue;
    }

    private class Rule {

        //Handle optional parameters by passing in null or -1 for those not provided
        public Rule(String in_action, String in_src, String in_dst, String in_kind, Integer in_seqNum, Boolean in_dupe)
            throws IllegalArgumentException {
            try {
                action = Rule_action.valueOf(in_action);
            } catch (NullPointerException e) {
                //turn null ptr exception into illegal arg, we got a null string somehow
                throw new IllegalArgumentException();
            }

            src = in_src;
            dst = in_dst;
            kind = in_kind;
            seqNum = in_seqNum;
            duplicate = in_dupe;
        }

        public Rule_action checkMessage(Message message) {

            //if applicable, check message source
            if (src != null && !src.equals(message.getSrc())) return Rule_action.none;
            //check dst if applicable
            if (dst != null && !dst.equals(message.getDst())) return Rule_action.none;
            //check kind if applicable
            if (kind != null && !kind.equals(message.getKind())) return Rule_action.none;
            //check seqNum
            if (seqNum != null && !seqNum.equals(message.getSeqNum())) return Rule_action.none;
            //check duplicate flag
            if (duplicate != null && !duplicate.equals(message.getDuplicate())) return Rule_action.none;

            return action;
        }

        private Rule_action action;
        private String src = null;
        private String dst = null;
        private String kind = null;
        private Integer seqNum = null;
        private Boolean duplicate = null;
    }

    private class Group {

        public Group(String in_name) {
            gname = in_name;
            sendVector = (VectorClockService) ClockServiceFactory.getService("vector");
            groupVector = (VectorClockService) ClockServiceFactory.getService("vector");
            seqNum = new AtomicInteger(0);
            members = new ConcurrentHashMap<String, GroupNode>();
        }

        public void addMember(String name) {
            if (!members.containsKey(name)) {
                members.put(name, new GroupNode(name));
                groupVector.addNode(name);
                sendVector.addNode(name);
            }
        }

        public Set<String> getMembers() {
            return Collections.unmodifiableSet(members.keySet());
        }

        public GroupNode getMember(String name) {
            return members.get(name);
        }

        public int incrementAndGetSeqNum() {
            return seqNum.incrementAndGet();
        }

        public VectorTimeStamp incrementAndGetTimeStamp(String node) {
            return (VectorTimeStamp) sendVector.incrementAndGet(node);
        }
        
        public ArrayList<GroupMessage> deliverGroupMessage() {
            ArrayList<GroupMessage> toDeliverMsgs = new ArrayList<GroupMessage>();
            
            Set<Map.Entry<String, GroupNode>> nodeSet = members.entrySet();
            for (Map.Entry<String, GroupNode> entry: nodeSet) {
                GroupNode node = entry.getValue();
                while (true) {
                    VectorTimeStamp localGroupTimeStamp = (VectorTimeStamp) groupVector.getTimeStamp();
                    GroupMessage groupMsg = node.getToDeliverMessage(localGroupTimeStamp);

                    if (groupMsg == null) {
                        break;
                    }
                   
                    toDeliverMsgs.add(groupMsg);
                    groupVector.increment(groupMsg.getSrc());
                    sendVector.maxTime((VectorTimeStamp) groupVector.getTimeStamp());
                }
            }
            
            return toDeliverMsgs;
        }

        private String gname;
        private VectorClockService sendVector;
        private VectorClockService groupVector;
        private AtomicInteger seqNum;
        private Map<String, GroupNode> members;
    }

    private class GroupNode {

        public GroupNode(String in_name) {
            name = in_name;
            seqNum = new AtomicInteger();
            nacked = new ConcurrentSkipListMap<Integer, Integer>();
            holdbackQueue = new ConcurrentSkipListMap<Integer, GroupMessage>();
            lastDelivered = 0;
        }

        public int getSeqNum() {
            return seqNum.get();
        }

        public void setSeqNum(int in_value) {
            seqNum.set(in_value);
        }

        public GroupMessage getHoldbackMsg(int index) {
            return holdbackQueue.get(index);
        }

        public boolean hasHoldbackMsg(int index) {
            return holdbackQueue.containsKey(index);
        }

        public void removeHoldbackMsg(int index) {
            holdbackQueue.remove(index);
        }

        public void addHoldbackMsg(int index, GroupMessage msg) {
            holdbackQueue.put(index, msg);
        }

        public int getLowestHoldback() {
            return holdbackQueue.firstKey();
        }

        public void addNacked(int i, int delay) {
            nacked.put(i, delay);
        }

        public boolean isNacked(int i) {
            //count down last time we nacked, pretty much a timeout
            if (nacked.containsKey(i)) {
                int delay = nacked.get(i);
                delay -= 1;
                if (delay <= 0) {
                    nacked.remove(i);
                }
                else {
                    nacked.put(i, delay);
                }
                return true;
            }
            return false;
        }
        
        public GroupMessage getToDeliverMessage(VectorTimeStamp localGroupTimeStamp) {
            GroupMessage toDeliverMsg = null;
            
            // Go through holdbackQueue, check if can deliver one groupMessage
            ConcurrentNavigableMap<Integer, GroupMessage> keyValuePairView = holdbackQueue.tailMap(lastDelivered, false);
            Set<Map.Entry<Integer, GroupMessage>> keyValuePairSet = keyValuePairView.entrySet();
            for (Map.Entry<Integer, GroupMessage> entry : keyValuePairSet) {
                GroupMessage groupMsg = entry.getValue();
                if (localGroupTimeStamp.compareCausalOrder(groupMsg.getGroupStamp(),
                        groupMsg.getSrc())) {
                        toDeliverMsg = groupMsg;
                        lastDelivered = groupMsg.getSeqNum();
                        //holdbackQueue.remove(entry.getKey(), entry.getValue());
                        break ;
                }
            }
            
            return toDeliverMsg;
        }

        private String name;
        private AtomicInteger seqNum;
        private int lastDelivered;
        private ConcurrentSkipListMap<Integer, Integer> nacked;
        private ConcurrentSkipListMap<Integer, GroupMessage> holdbackQueue;
    }

    private class ClientWorker implements Runnable {

        ClientWorker(Socket in_client) {
            client = in_client;
        }

        public void run() {
            Message msg;
            ObjectInputStream msg_stream;
            try {
                msg_stream = new ObjectInputStream(client.getInputStream());
                while ((msg = (Message) msg_stream.readObject()) != null) {
                    //msg = (Message) msg_stream.readObject();
                    internalReceive(msg);
                }
            } catch (EOFException e) {
                //client closed the connection, we should just kill the thread and restart next time
                msg = null;
            } catch (IOException e) {
                //we failed to open the stream, kill the thread
                msg = null;
            } catch  (ClassNotFoundException e) {
                //could not find the message class for some reason, die
                msg = null;
            }

            try {
                client.close();
            } catch (IOException e) {
                //we failed to close somehow
            }
        }

        private Socket client;
    }

    //Private members
    private String name;
    private String config_path;
    private long configModTime;
    private AtomicInteger seqNum;
    private Map<String, Node> nodes; //Hashmap of names to node information
    private Map<String, Group> groups;
    private List<Rule> incoming; //Synchronized list of incoming rules
    private List<Rule> outgoing; //Synchronized list of outgoing rules
    private Queue<Message> sendQueue;    //queue for sending messages
    private LinkedBlockingQueue<Message> receiveQueue; //queue for received messages
    private Queue<Message> delayRQueue;  //queue for delayed receive messages
    private Queue<Message> delaySQueue;  //queue for delayed send messages
    private ServerSocket listener;
    private ClockService clockService;

}
