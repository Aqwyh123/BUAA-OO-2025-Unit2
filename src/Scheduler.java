import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class Scheduler implements Runnable {
    private int position;
    private boolean direction;
    private final PriorityBlockingQueue<Request> waitingQueue;
    private final PriorityQueue<Request> takingQueue;
    private final LinkedBlockingQueue<Response> responseQueue;

    public Scheduler(int initialPosition, PriorityBlockingQueue<Request> waitingQueue,
                     LinkedBlockingQueue<Response> responseQueue) {
        this.position = initialPosition;
        this.direction = true;
        this.waitingQueue = waitingQueue;
        this.takingQueue = new PriorityQueue<>(new PriorityComparator());
        this.responseQueue = responseQueue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                boolean loading = load();
                boolean unloading = unload();
                if (!loading && !unloading) {
                    break;
                }
                move();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean load() throws InterruptedException {
        boolean continueLoading = true;
        if (takingQueue.isEmpty()) {
            Request request = waitingQueue.take();
            if (request instanceof PersonRequest) {
                PersonRequest personRequest = (PersonRequest) request;
                int fromPosition = Elevator.availablePositions.get(personRequest.getFromFloor());
                direction = fromPosition >= position;
                while (position != fromPosition) {
                    move();
                }
                responseQueue.offer(new LoadResponse(request));
                takingQueue.add(request);
            } else {
                responseQueue.offer(new StopResponse());
                continueLoading = false;
            }
        } else if (takingQueue.size() < Elevator.ratedLoad) {
            LinkedList<Request> loadRequests = new LinkedList<>();
            for (Request request : waitingQueue) {
                if (request instanceof PersonRequest) {
                    PersonRequest personRequest = (PersonRequest) request;
                    int fromPosition = Elevator.availablePositions.get(personRequest.getFromFloor());
                    int toPosition = Elevator.availablePositions.get(personRequest.getToFloor());
                    boolean requestDirection = toPosition >= position;
                    if (fromPosition == position && requestDirection == direction) {
                        responseQueue.offer(new LoadResponse(request));
                        loadRequests.add(request);
                        takingQueue.add(request);
                    }
                }
            }
            waitingQueue.removeAll(loadRequests);
        }
        return continueLoading;
    }

    private boolean unload() {
        LinkedList<Request> unloadRequests = new LinkedList<>();
        for (Request request : takingQueue) {
            if (request instanceof PersonRequest) {
                PersonRequest personRequest = (PersonRequest) request;
                int targetPosition = Elevator.availablePositions.get(personRequest.getToFloor());
                if (targetPosition == position) {
                    responseQueue.offer(new UnloadResponse(request));
                    unloadRequests.add(request);
                }
            }
        }
        takingQueue.removeAll(unloadRequests);
        return !takingQueue.isEmpty();
    }

    private void move() {
        responseQueue.offer(new MoveResponse(direction));
        position = direction ? position + 1 : position - 1;
    }
}
