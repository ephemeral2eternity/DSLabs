package MessagePasser;

import java.io.File;
import java.io.FileNotFoundException;

public class ConfigChecker extends Thread {
    private static String configFile;
    private static long timestamp;
    
    ConfigChecker(String configFile) {
        this.configFile = configFile;
        File file = new File(configFile);
        timestamp = file.lastModified();
    }
    
    @Override
    public void run() {
        while (true) {
            File file = new File(configFile);
            Long newTimestamp = file.lastModified();
            
            if(timestamp < newTimestamp) {
                timestamp = newTimestamp;

                try {
                    MessagePasser.configRules(configFile);
                    System.out.println("Send/Recv rules updated!");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.err.println("Config file checker failed!");
                    System.exit(-1);
                }
            }
            
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.err.println("Config file checker failed!");
                System.exit(-1);
            }            
        }
    }
}
