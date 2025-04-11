import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.Request;
import com.oocourse.elevator3.ScheRequest;

public class Scheduler {
    public static Task getTask(int position, int direction, boolean state,
        RequestQueue dispatchQueue, RequestSet receiveSet, RequestSet takeSet) {
        if (state) {
            if (takeSet.isEmpty() && receiveSet.isEmpty() && dispatchQueue.isEmpty()) {
                return new PauseTask();
            } else if (hasOut(position, takeSet)) {
                return new OutTask(getOut(position, takeSet));
            } else if (dispatchQueue.peek() instanceof ScheRequest) {
                return new CloseTask(Elevator.MIN_DOOR_PAUSE_TIME);
            } else if (!dispatchQueue.isEmpty()) {
                return new ReceiveTask();
            } else if (hasIn(position, direction, receiveSet, takeSet)) {
                return new InTask(getIn(position, direction, takeSet.size(), receiveSet));
            } else {
                return new CloseTask(Elevator.MIN_DOOR_PAUSE_TIME);
            }
        } else {
            if (takeSet.isEmpty() && receiveSet.isEmpty() && dispatchQueue.isEmpty()) {
                return new PauseTask();
            } else if (hasOut(position, takeSet)) {
                return new OpenTask();
            } else if (dispatchQueue.peek() instanceof ScheRequest) {
                return new ScheTask((ScheRequest) dispatchQueue.peek());
            } else if (!dispatchQueue.isEmpty()) {
                return new ReceiveTask();
            } else if (direction == 0) {
                return new TurnTask(getNewDirection(position, receiveSet));
            } else if (hasIn(position, direction, receiveSet, takeSet)) {
                return new OpenTask();
            } else if (!takeSet.isEmpty()) {
                return new MoveTask(Elevator.RATED_SPEED);
            } else {
                int newDirection = getNewDirection(position, receiveSet);
                if (newDirection == direction) {
                    return new MoveTask(Elevator.RATED_SPEED);
                } else {
                    return new TurnTask(newDirection);
                }
            }
        }
    }

    private static boolean hasIn(int position, int direction,
        RequestSet receiveSet, RequestSet takeSet) {
        if (takeSet.size() >= Elevator.RATED_LOAD) {
            return false;
        }
        for (Request request : receiveSet) {
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

    private static boolean hasOut(int position, RequestSet takeSet) {
        for (Request request : takeSet) {
            int toPosition = Elevator.POSITIONS.get(((PersonRequest) request).getToFloor());
            if (toPosition == position) {
                return true;
            }
        }
        return false;
    }

    private static int getNewDirection(int position, RequestSet receiveSet) {
        PersonRequest request = (PersonRequest) receiveSet.first();
        int fromPosition = Elevator.POSITIONS.get(request.getFromFloor());
        int toPosition = Elevator.POSITIONS.get(request.getToFloor());
        if (position != fromPosition) {
            return Integer.signum(fromPosition - position);
        } else {
            return Integer.signum(toPosition - position);
        }
    }

    private static RequestSet getIn(int position, int direction, int size, RequestSet receiveSet) {
        RequestSet inSet = new RequestSet();
        for (Request request : receiveSet) {
            if (size + inSet.size() >= Elevator.RATED_LOAD) {
                break;
            }
            PersonRequest personRequest = (PersonRequest) request;
            int fromPosition = Elevator.POSITIONS.get(personRequest.getFromFloor());
            int toPosition = Elevator.POSITIONS.get(personRequest.getToFloor());
            if (fromPosition == position && direction * (toPosition - position) > 0) {
                inSet.add(personRequest);
            }
        }
        return inSet;
    }

    private static RequestSet getOut(int position, RequestSet takeSet) {
        RequestSet outQueue = new RequestSet();
        for (Request request : takeSet) {
            int toPosition = Elevator.POSITIONS.get(((PersonRequest) request).getToFloor());
            if (toPosition == position) {
                outQueue.add(request);
            }
        }
        return outQueue;
    }
}
