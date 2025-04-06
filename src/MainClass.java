import com.oocourse.elevator2.TimableOutput;

import java.util.HashMap;

public class MainClass {
    public static final int[] IDS = {1, 2, 3, 4, 5, 6};

    public static void main(String[] args) {
        TimableOutput.initStartTimestamp();

        Thread monitorThread = new Thread(Monitor.instance);
        monitorThread.start();

        RequestQueue scanQueue = new RequestQueue();
        Thread scannerThread = new Thread(new RequestScanner(scanQueue));
        scannerThread.start();

        HashMap<Integer, RequestQueue> dispatchQueues = new HashMap<>();
        for (int id : IDS) {
            dispatchQueues.put(id, new RequestQueue());
        }
        Thread dispatcherThread = new Thread(new Dispatcher(scanQueue, dispatchQueues));
        dispatcherThread.start();

        for (int id : IDS) {
            Thread elevatorThread = new Thread(new Elevator(id, scanQueue, dispatchQueues.get(id)));
            elevatorThread.start();
        }
    }
}
