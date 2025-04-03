import com.oocourse.elevator2.PersonRequest;
import com.oocourse.elevator2.Request;
import com.oocourse.elevator2.ScheRequest;

import java.util.ArrayList;

public class Scheduler {
    public static Task getTask(int position, int direction, boolean state,
        RequestQueue waitingQueue, ProcessingQueue takingQueue) {
        if (state) {
            if (takingQueue.isEmpty() && waitingQueue.isEmpty() && waitingQueue.isEnd()) {
                return new CloseTask(Elevator.MIN_DOOR_PAUSE_TIME);
            } else if (waitingQueue.peek() instanceof ScheRequest) {
                return new CloseTask(Elevator.MIN_DOOR_PAUSE_TIME);
            } else if (hasOut(position, takingQueue)) {
                return new OutTask(getOut(position, takingQueue));
            } else if (hasIn(position, direction, waitingQueue, takingQueue)) {
                return new InTask(getIn(position, direction, takingQueue.size(), waitingQueue));
            } else {
                return new CloseTask(Elevator.MIN_DOOR_PAUSE_TIME);
            }
        } else {
            if (takingQueue.isEmpty() && waitingQueue.isEmpty() && waitingQueue.isEnd()) {
                return new StopTask();
            } else if (waitingQueue.peek() instanceof ScheRequest) {
                return new ScheTask((ScheRequest) waitingQueue.peek());
            } else if (hasOut(position, takingQueue)) {
                return new OpenTask();
            } else if (direction == 0) {
                return new TurnTask(getNewDirection(position, waitingQueue));
            } else if (hasIn(position, direction, waitingQueue, takingQueue)) {
                return new OpenTask();
            } else if (!takingQueue.isEmpty()) {
                return new MoveTask(Elevator.RATED_SPEED);
            } else {
                int newDirection = getNewDirection(position, waitingQueue);
                if (newDirection == direction) {
                    return new MoveTask(Elevator.RATED_SPEED);
                } else {
                    return new TurnTask(newDirection);
                }
            }
        }
    }

    private static int getNewDirection(int position, RequestQueue waitingQueue) {
        Request request = waitingQueue.peek();
        if (request instanceof PersonRequest) {
            PersonRequest personRequest = (PersonRequest) request;
            int fromPosition = Elevator.POSITIONS.get(personRequest.getFromFloor());
            int toPosition = Elevator.POSITIONS.get(personRequest.getToFloor());
            if (fromPosition == position) {
                return Integer.signum(toPosition - position);
            } else {
                return Integer.signum(fromPosition - position);
            }
        } else {
            return 0;
        }
    }

    private static boolean hasIn(int position, int direction,
        RequestQueue waitingQueue, ProcessingQueue takingQueue) {
        if (takingQueue.size() >= Elevator.RATED_LOAD) {
            return false;
        }
        for (Request request : waitingQueue) {
            if (request instanceof PersonRequest) {
                PersonRequest personRequest = (PersonRequest) request;
                int fromPosition = Elevator.POSITIONS.get(personRequest.getFromFloor());
                int toPosition = Elevator.POSITIONS.get(personRequest.getToFloor());
                if (fromPosition == position && direction * (toPosition - position) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasOut(int position, ProcessingQueue takingQueue) {
        for (PersonRequest request : takingQueue) {
            int toPosition = Elevator.POSITIONS.get(request.getToFloor());
            if (toPosition == position) {
                return true;
            }
        }
        return false;
    }

    private static ArrayList<PersonRequest> getIn(int position, int direction, int size,
        RequestQueue waitingQueue) {
        ArrayList<PersonRequest> inQueue = new ArrayList<>();
        Request[] waitingList = waitingQueue.toArray();
        for (Request request : waitingList) {
            if (size + inQueue.size() >= Elevator.RATED_LOAD) {
                break;
            }
            if (request instanceof PersonRequest) {
                PersonRequest personRequest = (PersonRequest) request;
                int fromPosition = Elevator.POSITIONS.get(personRequest.getFromFloor());
                int toPosition = Elevator.POSITIONS.get(personRequest.getToFloor());
                if (fromPosition == position && direction * (toPosition - position) > 0) {
                    inQueue.add(personRequest);
                }
            }
        }
        return inQueue;
    }

    private static ArrayList<PersonRequest> getOut(int position, ProcessingQueue takingQueue) {
        ArrayList<PersonRequest> outQueue = new ArrayList<>();
        Request[] takingList = takingQueue.toArray();
        for (Request request : takingList) {
            if (request instanceof PersonRequest) {
                PersonRequest personRequest = (PersonRequest) request;
                int toPosition = Elevator.POSITIONS.get(personRequest.getToFloor());
                if (toPosition == position) {
                    outQueue.add(personRequest);
                }
            }
        }
        return outQueue;
    }
}
