import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;

import java.util.ArrayList;

public class Scheduler implements Runnable {
    private int position;
    private int direction;
    private final RequestQueue waitingQueue;
    private final ProcessingQueue takingQueue;
    private final ResponseQueue actingQueue;

    public Scheduler(int initialPosition, RequestQueue waitingQueue, ResponseQueue actingQueue) {
        this.position = initialPosition;
        this.direction = 0;
        this.waitingQueue = waitingQueue;
        this.takingQueue = new ProcessingQueue();
        this.actingQueue = actingQueue;
    }

    @Override
    public void run() {
        ProcessingQueue bufferingQueue = new ProcessingQueue();
        ArrayList<Request> checkingQueue = new ArrayList<>();
        do {
            while (!takingQueue.isEmpty()) {
                Request request = takingQueue.peek();
                if (!unload(request)) {
                    checkingQueue.add(takingQueue.poll());
                }
            }
            takingQueue.addAll(checkingQueue);
            checkingQueue.clear();

            if (takingQueue.isEmpty() && waitingQueue.isEmpty()) {
                direction = 0;
            } else if (!takingQueue.isEmpty()) {
                PersonRequest personRequest = (PersonRequest) takingQueue.peek();
                int toPosition = Elevator.POSITIONS.get(personRequest.getToFloor());
                direction = Integer.signum(toPosition - position);
            } else if (waitingQueue.peek() instanceof NullRequest) {
                direction = 0;
            } else {
                PersonRequest personRequest = (PersonRequest) waitingQueue.peek();
                int fromPosition = Elevator.POSITIONS.get(personRequest.getFromFloor());
                int toPosition = Elevator.POSITIONS.get(personRequest.getToFloor());
                if (fromPosition == position) {
                    direction = Integer.signum(toPosition - position);
                } else {
                    direction = Integer.signum(fromPosition - position);
                }
            }

            if (takingQueue.size() < Elevator.ratedLoad) {
                if (takingQueue.isEmpty()) {
                    try {
                        bufferingQueue.add(waitingQueue.take());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                waitingQueue.drainTo(bufferingQueue);
                while (!bufferingQueue.isEmpty() && takingQueue.size() < Elevator.ratedLoad) {
                    Request request = bufferingQueue.poll();
                    if (load(request)) {
                        PersonRequest personRequest = (PersonRequest) takingQueue.peek();
                        int toPosition = Elevator.POSITIONS.get(personRequest.getToFloor());
                        direction = Integer.signum(toPosition - position);
                    } else {
                        waitingQueue.put(request);
                    }
                }
            }
        } while (move());
    }

    private boolean load(Request request) {
        if (request instanceof PersonRequest) {
            PersonRequest personRequest = (PersonRequest) request;
            int fromPosition = Elevator.POSITIONS.get(personRequest.getFromFloor());
            int toPosition = Elevator.POSITIONS.get(personRequest.getToFloor());
            int reqDir = toPosition - position;
            if (fromPosition == position && (takingQueue.isEmpty() || reqDir * direction >= 0)) {
                actingQueue.offer(new LoadResponse(request));
                takingQueue.add(request);
                return true;
            }
        }
        return false;
    }

    private boolean unload(Request request) {
        if (request instanceof PersonRequest) {
            PersonRequest personRequest = (PersonRequest) request;
            int toPosition = Elevator.POSITIONS.get(personRequest.getToFloor());
            if (toPosition == position) {
                actingQueue.offer(new UnloadResponse(request));
                takingQueue.remove(request);
                return true;
            }
        }
        return false;
    }

    private boolean move() {
        if (waitingQueue.peek() instanceof NullRequest && takingQueue.isEmpty()) {
            actingQueue.offer(new StopResponse());
            return false;
        } else {
            if (direction != 0) {
                actingQueue.offer(new MoveResponse(direction));
                this.position = position + direction;
            }
            return true;
        }
    }
}
