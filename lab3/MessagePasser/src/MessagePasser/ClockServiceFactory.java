/*
    File: ClockServiceFactory.java
    Author: Justin Loo (jloo@andrew.cmu.edu)
    Brief: Lab1 factory for producing logical or vector clock services
*/

package MessagePasser;

class ClockServiceFactory {

    //use naive implementation for now, maybe replace with a more robust one later
    public static ClockService getService(String id) {
        if (id.equalsIgnoreCase("logical")) {
            return new LogicalClockService();
        }
        else if (id.equalsIgnoreCase("vector")) {
            return new VectorClockService();
        }

        return null;
    }

}
