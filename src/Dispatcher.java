import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;

import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class Dispatcher implements Runnable {
    private final RequestQueue scanningQueue;
    private final HashMap<Integer, RequestQueue> elevatorQueues;

    Dispatcher(RequestQueue scanningQueue, HashMap<Integer, RequestQueue> elevatorQueues) {
        this.scanningQueue = scanningQueue;
        this.elevatorQueues = elevatorQueues;
    }

    @Override
    public void run() {
        while (true) {
            Request request;
            try {
                request = scanningQueue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (!dispatch(request)) {
                break;
            }
        }
    }

    private boolean dispatch(Request request) {
        if (request instanceof PersonRequest) {
            PersonRequest personRequest = (PersonRequest) request;
            int elevatorId = personRequest.getElevatorId();
            elevatorQueues.get(elevatorId).put(personRequest);
            return true;
        } else {
            for (PriorityBlockingQueue<Request> waitingQueue : elevatorQueues.values()) {
                waitingQueue.put(new NullRequest());
            }
            return false;
        }
    }
}
