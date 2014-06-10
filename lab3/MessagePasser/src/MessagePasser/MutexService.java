package MessagePasser;

import java.io.FileNotFoundException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MutexService {

    public MutexService(String configFilePath, String localHostName) throws MutexException {
        name = localHostName;
        try {
            mp = new MessagePasser(configFilePath, localHostName, "logical");
        } catch (FileNotFoundException e) {
            throw new MutexException("File not found.\n");
        }
        if (mp == null) {
            throw new MutexException("MessagePasser failed to initialize.\n");
        }

        waitQueue = new LinkedBlockingQueue<MutexMultiMessage>();
        mutexVoted = false;
        localGroup = mp.getNodeGroup(name);
        ackMap = new ConcurrentHashMap<String, Boolean>();
        totalSent = new AtomicInteger();
        remoteSent = new AtomicInteger();
        totalRec = new AtomicInteger();
        remoteRec = new AtomicInteger();

        Set<String> gmembers = mp.getGroupMembers(localGroup);
        if (gmembers !=  null) {
            for (String member : gmembers) {
                ackMap.put(member, false);
            }
        }

        startReceive();
    }

    public boolean sendMessage(Message m) {
        return mp.send(m);
    }


    public int getTotalSent() {
        return totalSent.get();
    }

    public int getRemoteSent() {
        return remoteSent.get();
    }

    public int getTotalRec() {
        return totalRec.get();
    }

    public int getRemoteRec() {
        return remoteRec.get();
    }

    public void resetAll() {
        totalSent.set(0);
        remoteSent.set(0);
        totalRec.set(0);
        remoteRec.set(0);
    }

    //request the resource, true if successfully received the resource false otherwise?
    public synchronized boolean requestResource() {
        if (!localGroup.isEmpty())
        {
            MutexMultiMessage msg = new MutexMultiMessage(localGroup, "MUTEX");
            msg.setMutexAction(Mutex_type.request);
            mp.sendGroup(msg);
            incrementGroupSent();

            //block until we get the resource
            if (locked.get() == false) {
                try {
                    wait();
                } catch (InterruptedException e) {

                }
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    //release the held resource
    public synchronized void releaseResource() {

        for (String s : ackMap.keySet()) {
            ackMap.put(s, false);
        }

        if (!localGroup.isEmpty())
        {
            MutexMultiMessage msg = new MutexMultiMessage(localGroup, "MUTEX");
            msg.setMutexAction(Mutex_type.release);
            mp.sendGroup(msg);
            incrementGroupSent();
        }

        locked.set(false);
    }

    //process multicast message types
    private synchronized void processRequest(Message generic_msg) {

        totalRec.incrementAndGet();
        if (!generic_msg.getSrc().equals(name)) {
            remoteRec.incrementAndGet();
        }

        if (generic_msg instanceof MutexMultiMessage) {
            MutexMultiMessage multi_msg = (MutexMultiMessage) generic_msg;

            if (multi_msg.getMutexAction().equals(Mutex_type.request))
            {
                if (mutexVoted)
                {
                    waitQueue.add(multi_msg);
                    System.out.println("Node " + multi_msg.getSrc() + " request queued");
                }
                else
                {
                    MutexUniMessage ack = new MutexUniMessage(multi_msg.getSrc(), "MUTEX-ACK");
                    ack.setMutexAction(Mutex_type.ack);
                    mp.send(ack);
                    mutexVoted = true;

                    totalSent.incrementAndGet();
                    if (!multi_msg.getSrc().equals(name)) {
                        remoteSent.incrementAndGet();
                    }
                    System.out.println("Node " + multi_msg.getSrc() + " request serviced");
                }
            }
            else if (multi_msg.getMutexAction().equals(Mutex_type.release))
            {
                System.out.println("Node " + multi_msg.getSrc() + " release serviced");
                mutexVoted = false;

                if (!waitQueue.isEmpty())
                {
                    MutexMultiMessage mmsg = waitQueue.poll();
                    this.processRequest(mmsg);
                }
            }
        } else if (generic_msg instanceof MutexUniMessage) {
            MutexUniMessage uni_msg = (MutexUniMessage) generic_msg;

            if (uni_msg.getMutexAction() == Mutex_type.ack) {
                String src = uni_msg.getSrc();

                if (ackMap.containsKey(src)) {
                    ackMap.put(src, true);
                }

                System.out.println("ACK from " + uni_msg.getSrc());

                if (checkAcks()) {
                    locked.set(true);
                    notifyAll();
                }
            }
        }
    }

    private void incrementGroupSent() {
        for (String s : mp.getGroupMembers(localGroup)) {
            totalSent.incrementAndGet();
            if (!s.equals(name)) {
                remoteSent.incrementAndGet();
            }
        }
    }

    private void startReceive() {

        Runnable listenTask = new Runnable() {

            public void run() {
                Message msg;

                while (true) {
                    msg = mp.blockingReceive();

                    if (msg instanceof MutexMultiMessage ||
                            msg instanceof MutexUniMessage) {
                        processRequest(msg);
                    }
                }
            }
        };

        Thread receiveThread = new Thread(listenTask);
        receiveThread.start();
    }

    private boolean checkAcks() {
        boolean res = true;
        for (Boolean b : ackMap.values()) {
            res &= b;
        }

        return res;
    }

    String name = null;
    String localGroup = null;
    MessagePasser mp = null;
    BlockingQueue<MutexMultiMessage> waitQueue = null;
    ConcurrentHashMap<String, Boolean> ackMap = null;
    AtomicInteger totalSent = null;
    AtomicInteger remoteSent = null;
    AtomicInteger totalRec = null;
    AtomicInteger remoteRec = null;
    final static AtomicBoolean locked = new AtomicBoolean(false);
    boolean mutexVoted = false;
}
