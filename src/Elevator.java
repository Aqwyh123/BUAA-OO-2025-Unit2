import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;
import com.oocourse.elevator1.TimableOutput;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public class Elevator implements Runnable {
    public static final int[] IDS = {1, 2, 3, 4, 5, 6};
    public static final List<String> FLOORS = Collections.unmodifiableList(
        Arrays.asList("B4", "B3", "B2", "B1", "F1", "F2", "F3", "F4", "F5", "F6", "F7"));
    public static final Map<String, Integer> POSITIONS = Collections.unmodifiableMap(
        FLOORS.stream().collect(Collectors.toMap(floor -> floor, FLOORS::indexOf)));
    private static final String INITIAL_FLOOR = "F1";
    private static final boolean INITIAL_STATE = false;
    private static final long RATED_SPEED = (long) (0.4 * 1000);
    public static final int RATED_LOAD = 6;

    private final int id;
    private int position;
    private int direction;
    private final Door door;
    private final RequestQueue waitingQueue;
    private final ProcessingQueue takingQueue;

    public Elevator(int id, RequestQueue waitingQueue) {
        this.id = id;
        this.position = POSITIONS.get(INITIAL_FLOOR);
        this.direction = 0;
        this.door = new Door(id, INITIAL_STATE);
        this.waitingQueue = waitingQueue;
        this.takingQueue = new ProcessingQueue();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Task task = Scheduler.getTask(position, direction, door.getState(),
                    waitingQueue, takingQueue);
                if (!execute(task)) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean execute(Task task) {
        if (task instanceof OpenTask) {
            door.open(FLOORS.get(position));
        } else if (task instanceof CloseTask) {
            door.close(FLOORS.get(position));
        } else if (task instanceof InTask) {
            ArrayList<Request> inQueue = ((InTask) task).getIn();
            for (Request request : inQueue) {
                if (request instanceof PersonRequest) {
                    PersonRequest personRequest = (PersonRequest) request;
                    int personId = personRequest.getPersonId();
                    TimableOutput.println(String.format("IN-%d-%s-%d", personId,
                        FLOORS.get(position), id));
                }
            }
            waitingQueue.removeAll(inQueue);
            takingQueue.addAll(inQueue);
        } else if (task instanceof OutTask) {
            ArrayList<Request> outQueue = ((OutTask) task).getOut();
            for (Request request : outQueue) {
                if (request instanceof PersonRequest) {
                    PersonRequest personRequest = (PersonRequest) request;
                    int personId = personRequest.getPersonId();
                    TimableOutput.println(String.format("OUT-%d-%s-%d", personId,
                        FLOORS.get(position), id));
                }
            }
            takingQueue.removeAll(outQueue);
        } else if (task instanceof MoveTask) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(RATED_SPEED));
            position = position + direction;
            TimableOutput.println(String.format("ARRIVE-%s-%d", FLOORS.get(position), id));
        } else if (task instanceof TurnTask) {
            direction = ((TurnTask) task).getDirection();
        } else if (task instanceof StopTask) {
            return false;
        }
        return true;
    }
}
