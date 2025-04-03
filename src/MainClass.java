import com.oocourse.elevator2.TimableOutput;

import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

public class MainClass {
    public static void main(String[] args) {
        TimableOutput.initStartTimestamp();

        RequestQueue dispatchingQueue = new RequestQueue();
        Thread scannerThread = new Thread(new RequestScanner(dispatchingQueue));
        scannerThread.setPriority(Thread.MAX_PRIORITY);
        scannerThread.start();

        HashMap<Integer, ReentrantLock> locks = new HashMap<>();
        CyclicBarrier barrier = new CyclicBarrier(Elevator.IDS.length + 1);

        HashMap<Integer, RequestQueue> waitingQueues = new HashMap<>();
        for (int id : Elevator.IDS) {
            locks.put(id, new ReentrantLock());
            waitingQueues.put(id, new RequestQueue());
        }
        Thread dispatcherThread = new Thread(new Dispatcher(locks, barrier,
            dispatchingQueue, waitingQueues));
        dispatcherThread.setPriority(Thread.MAX_PRIORITY - 1);
        dispatcherThread.start();

        for (int id : Elevator.IDS) {
            Thread elevatorThread = new Thread(new Elevator(id, locks.get(id), barrier,
                dispatchingQueue, waitingQueues.get(id)));
            elevatorThread.start();
        }
    }
}
