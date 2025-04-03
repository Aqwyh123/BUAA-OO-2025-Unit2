import com.oocourse.elevator2.PersonRequest;
import com.oocourse.elevator2.ScheRequest;
import com.oocourse.elevator2.Request;
import com.oocourse.elevator2.TimableOutput;

import java.util.HashMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

public class Dispatcher implements Runnable {
    private int index = 0;
    private final HashMap<Integer, ReentrantLock> locks;
    private final CyclicBarrier barrier;
    private final RequestQueue dispatchingQueue;
    private final HashMap<Integer, RequestQueue> waitingQueues;

    Dispatcher(HashMap<Integer, ReentrantLock> locks, CyclicBarrier barrier,
        RequestQueue dispatchingQueue, HashMap<Integer, RequestQueue> waitingQueues) {
        this.locks = locks;
        this.barrier = barrier;
        this.dispatchingQueue = dispatchingQueue;
        this.waitingQueues = waitingQueues;
    }

    @Override
    public void run() {
        while (true) {
            try {
                boolean shouldContinue = dispatch(dispatchingQueue.take());
                barrier.reset();
                if (!shouldContinue) {
                    barrier.await();
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (BrokenBarrierException ignored) {
                continue;
            }
        }
    }

    private boolean dispatch(Request request) {
        if (request instanceof PersonRequest) {
            PersonRequest personRequest = (PersonRequest) request;
            int id = (int) waitingQueues.keySet().toArray()[index];
            locks.get(id).lock();
            TimableOutput.println(String.format("RECEIVE-%d-%d", personRequest.getPersonId(), id));
            waitingQueues.get(id).put(personRequest);
            locks.get(id).unlock();
            index = (index + 1) % waitingQueues.size();
            return true;
        } else if (request instanceof ScheRequest) {
            ScheRequest scheRequest = (ScheRequest) request;
            int elevatorId = scheRequest.getElevatorId();
            waitingQueues.get(elevatorId).put(scheRequest);
            return true;
        } else {
            waitingQueues.forEach((i, waitingQueue) -> waitingQueue.setEnd());
            return false;
        }
    }
}
