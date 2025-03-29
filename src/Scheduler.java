import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class Scheduler implements Runnable {
    private int position;
    private boolean direction;
    private boolean taking;
    private boolean ended;
    private final PriorityBlockingQueue<Request> waitingQueue;
    private final PriorityQueue<Request> takingQueue;
    private final LinkedBlockingQueue<Response> responseQueue;

    public Scheduler(int initialPosition, PriorityBlockingQueue<Request> waitingQueue,
                     LinkedBlockingQueue<Response> responseQueue) {
        this.position = initialPosition;
        this.direction = true;
        this.taking = false;
        this.ended = false;
        this.waitingQueue = waitingQueue;
        this.takingQueue = new PriorityQueue<>(new PriorityComparator());
        this.responseQueue = responseQueue;
    }

    @Override
    public void run() {
        while (true) {
            load();
            unload();
            if (takingQueue.isEmpty() && ended) {
                responseQueue.offer(new StopResponse());
                break;
            } else if (!takingQueue.isEmpty() || taking) {
                move();
            }
        }
    }

    private void load() {
        if (takingQueue.isEmpty()) {
            Request request = waitingQueue.peek();
            if (request == null) {
                return;
            }
            if (request instanceof PersonRequest) {
                PersonRequest personRequest = (PersonRequest) request;
                int fromPosition = Elevator.availablePositions.get(personRequest.getFromFloor());
                int toPosition = Elevator.availablePositions.get(personRequest.getToFloor());
                boolean fromDirection = fromPosition >= position;
                boolean toDirection = toPosition >= position;
                if (position != fromPosition) {
                    direction = fromDirection;
                    taking = true;
                } else {
                    responseQueue.offer(new LoadResponse(request));
                    waitingQueue.poll();
                    takingQueue.add(request);
                    direction = toDirection;
                    taking = false;
                }
            } else {
                waitingQueue.poll();
                takingQueue.add(request);
            }
        } else if (takingQueue.size() < Elevator.ratedLoad) {
            Iterator<Request> iterator = waitingQueue.iterator();
            while (iterator.hasNext()) {
                Request request = iterator.next();
                if (request instanceof PersonRequest) {
                    PersonRequest personRequest = (PersonRequest) request;
                    int fromPosition = Elevator.availablePositions.get(personRequest.getFromFloor());
                    int toPosition = Elevator.availablePositions.get(personRequest.getToFloor());
                    boolean toDirection = toPosition >= position;
                    if (fromPosition == position && toDirection == direction) {
                        responseQueue.offer(new LoadResponse(request));
                        iterator.remove();
                        takingQueue.add(request);
                    }
                } else {
                    iterator.remove();
                    takingQueue.add(request);
                }
            }
        }
    }

    private void unload() {
        Iterator<Request> iterator = takingQueue.iterator();
        while (iterator.hasNext()) {
            Request request = iterator.next();
            if (request instanceof PersonRequest) {
                PersonRequest personRequest = (PersonRequest) request;
                int targetPosition = Elevator.availablePositions.get(personRequest.getToFloor());
                if (targetPosition == position) {
                    responseQueue.offer(new UnloadResponse(request));
                    iterator.remove();
                }
            } else {
                iterator.remove();
                ended = true;
            }
        }
    }

    private void move() {
        responseQueue.offer(new MoveResponse(direction));
        this.position = direction ? position + 1 : position - 1;
    }
}
