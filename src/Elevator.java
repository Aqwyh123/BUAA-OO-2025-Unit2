import com.oocourse.elevator2.PersonRequest;
import com.oocourse.elevator2.Request;
import com.oocourse.elevator2.ScheRequest;
import com.oocourse.elevator2.TimableOutput;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public class Elevator implements Runnable {
    public static final List<String> FLOORS = Collections.unmodifiableList(
        Arrays.asList("B4", "B3", "B2", "B1", "F1", "F2", "F3", "F4", "F5", "F6", "F7"));
    public static final Map<String, Integer> POSITIONS = Collections.unmodifiableMap(
        FLOORS.stream().collect(Collectors.toMap(floor -> floor, FLOORS::indexOf)));
    private static final String INITIAL_FLOOR = "F1";
    private static final boolean INITIAL_DOOR_STATE = false;
    public static final long MIN_DOOR_PAUSE_TIME = (long) (0.4 * 1000);
    public static final long MIN_SCHE_STOP_TIME = 1000;
    public static final long RATED_SPEED = (long) (0.4 * 1000);
    public static final int RATED_LOAD = 6;

    private final int id;

    private int position;
    private int direction;
    private long lastOpenTime;
    private boolean doorState;

    private final RequestQueue scanQueue;
    private final RequestQueue dispatchQueue;
    private final RequestSet receiveSet;
    private final RequestSet takeSet;

    public Elevator(int id, RequestQueue scanQueue, RequestQueue dispatchQueue) {
        this.id = id;
        this.position = POSITIONS.get(INITIAL_FLOOR);
        this.direction = 0;
        this.doorState = INITIAL_DOOR_STATE;
        this.scanQueue = scanQueue;
        this.dispatchQueue = dispatchQueue;
        this.receiveSet = new RequestSet();
        this.takeSet = new RequestSet();
    }

    @Override
    public void run() {
        while (true) {
            Task task = Scheduler.getTask(position, direction, doorState,
                dispatchQueue, receiveSet, takeSet);
            if (task instanceof StopTask) {
                Monitor.instance.setElevatorEnd(id, true);
                break;
            } else if (task instanceof PauseTask) {
                Monitor.instance.setElevatorEnd(id, true);
                Monitor.instance.waitToExecute(id);
            } else {
                Monitor.instance.setElevatorEnd(id, false);
                execute(task);
            }
        }
    }

    private void delay(long time) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(time));
    }

    private long output(String content) {
        return TimableOutput.println(content);
    }

    private void execute(Task task) {
        if (task instanceof OpenTask) {
            lastOpenTime = output(String.format("OPEN-%s-%d", FLOORS.get(position), id));
            doorState = true;
        } else if (task instanceof CloseTask) {
            CloseTask closeTask = (CloseTask) task;
            long pauseTime = closeTask.getPauseTime() - (System.currentTimeMillis() - lastOpenTime);
            if (pauseTime > 0) {
                delay(pauseTime);
            }
            output(String.format("CLOSE-%s-%d", FLOORS.get(position), id));
            doorState = false;
        } else if (task instanceof InTask) {
            RequestSet inSet = ((InTask) task).getIn();
            for (Request request : inSet) {
                int personId = ((PersonRequest) request).getPersonId();
                output(String.format("IN-%d-%s-%d", personId, FLOORS.get(position), id));
            }
            inSet.forEach(receiveSet::remove);
            takeSet.addAll(inSet);
        } else if (task instanceof OutTask) {
            RequestSet outSet = ((OutTask) task).getOut();
            for (Request request : outSet) {
                int personId = ((PersonRequest) request).getPersonId();
                if (position == POSITIONS.get(((PersonRequest) request).getToFloor())) {
                    output(String.format("OUT-S-%d-%s-%d", personId, FLOORS.get(position), id));
                } else {
                    output(String.format("OUT-F-%d-%s-%d", personId, FLOORS.get(position), id));
                }
            }
            outSet.forEach(takeSet::remove);
        } else if (task instanceof MoveTask) {
            delay(((MoveTask) task).getSpeed());
            position = position + direction;
            output(String.format("ARRIVE-%s-%d", FLOORS.get(position), id));
        } else if (task instanceof TurnTask) {
            direction = ((TurnTask) task).getDirection();
        } else if (task instanceof ReceiveTask) {
            for (Request request : dispatchQueue) {
                if (request instanceof PersonRequest) {
                    PersonRequest personRequest = (PersonRequest) request;
                    output(String.format("RECEIVE-%d-%d", personRequest.getPersonId(), id));
                    receiveSet.add(personRequest);
                }
            }
            dispatchQueue.removeAll(receiveSet);
        } else if (task instanceof ScheTask) {
            executeScheTask(((ScheTask) task).getRequest());
        }
    }

    private void executeScheTask(ScheRequest scheRequest) {
        output(String.format("SCHE-BEGIN-%d", id));
        int toPosition = POSITIONS.get(scheRequest.getToFloor());
        execute(new TurnTask(Integer.signum(toPosition - position)));
        while (position != toPosition) {
            execute(new MoveTask((long) (scheRequest.getSpeed() * 1000)));
        }
        execute(new OpenTask());

        RequestSet redispatchSet = new RequestSet();
        for (Request request : takeSet) {
            if (position != POSITIONS.get(((PersonRequest) request).getToFloor())) {
                redispatchSet.add(request);
            }
        }
        redispatchSet.addAll(receiveSet);
        execute(new OutTask((RequestSet) takeSet.clone()));
        for (Request request : redispatchSet) {
            PersonRequest personRequest = (PersonRequest) request;
            String fromFloor = personRequest.getFromFloor();
            String toFloor = personRequest.getToFloor();
            int personId = personRequest.getPersonId();
            int priority = personRequest.getPriority();
            scanQueue.put(new PersonRequest(fromFloor, toFloor, personId, priority));
        }
        receiveSet.clear();
        Monitor.instance.signalForDispatch();

        dispatchQueue.remove(scheRequest);
        execute(new CloseTask(MIN_SCHE_STOP_TIME));
        output(String.format("SCHE-END-%d", id));
    }
}
