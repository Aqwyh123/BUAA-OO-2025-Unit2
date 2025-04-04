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
                if (shouldContinue) {
                    barrier.reset();
                } else {
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
            Integer[] ids = locks.keySet().toArray(new Integer[0]);
            int tryCount = 0;
            while (!locks.get(ids[index]).tryLock() && tryCount < locks.size()) {
                index = (index + 1) % locks.size();
                tryCount++;
            }
            if (tryCount == locks.size()) {
                locks.get(ids[index]).lock();
            }
            int personId = ((PersonRequest) request).getPersonId();
            TimableOutput.println(String.format("RECEIVE-%d-%d", personId, ids[index]));
            waitingQueues.get(ids[index]).put(request);
            locks.get(ids[index]).unlock();
            index = (index + 1) % locks.size();
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
