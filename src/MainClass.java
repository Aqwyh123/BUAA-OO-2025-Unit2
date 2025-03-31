import com.oocourse.elevator1.TimableOutput;

import java.util.HashMap;

public class MainClass {
    public static void main(String[] args) {
        TimableOutput.initStartTimestamp();

        RequestQueue scanningQueue = new RequestQueue();
        Thread scannerThread = new Thread(new RequestScanner(scanningQueue));
        scannerThread.start();

        HashMap<Integer, RequestQueue> elevatorQueues = new HashMap<>();
        for (int id : Elevator.IDS) {
            elevatorQueues.put(id, new RequestQueue());
        }
        Thread dispatcherThread = new Thread(new Dispatcher(scanningQueue, elevatorQueues));
        dispatcherThread.start();

        for (int id : Elevator.IDS) {
            Thread elevatorThread = new Thread(new Elevator(id, elevatorQueues.get(id)));
            elevatorThread.start();
        }
    }
}
