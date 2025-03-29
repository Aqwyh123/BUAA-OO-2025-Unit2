import com.oocourse.elevator1.Request;
import com.oocourse.elevator1.TimableOutput;

import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class MainClass {
    public static void main(String[] args) {
        TimableOutput.initStartTimestamp();

        PriorityBlockingQueue<Request> scanningQueue = new PriorityBlockingQueue<>(100,
                new PriorityComparator());
        Thread scannerThread = new Thread(new RequestScanner(scanningQueue));
        scannerThread.start();

        HashMap<Integer, PriorityBlockingQueue<Request>> elevatorQueues = new HashMap<>();
        for (int id : Elevator.availableIds) {
            elevatorQueues.put(id, new PriorityBlockingQueue<>(100, new PriorityComparator()));
        }
        Thread dispatcherThread = new Thread(new Dispatcher(scanningQueue, elevatorQueues));
        dispatcherThread.start();

        for (int id : Elevator.availableIds) {
            Thread elevatorThread = new Thread(new Elevator(id, elevatorQueues.get(id)));
            elevatorThread.start();
        }
    }
}
