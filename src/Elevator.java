import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.Request;
import com.oocourse.elevator3.ScheRequest;
import com.oocourse.elevator3.TimableOutput;
import com.oocourse.elevator3.UpdateRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Elevator implements Runnable {
    public static final int[] IDS = {1, 2, 3, 4, 5, 6};
    public static final List<String> FLOORS = Collections.unmodifiableList(
        Arrays.asList("B4", "B3", "B2", "B1", "F1", "F2", "F3", "F4", "F5", "F6", "F7"));
    public static final Map<String, Integer> POSITIONS = Collections.unmodifiableMap(
        FLOORS.stream().collect(Collectors.toMap(floor -> floor, FLOORS::indexOf)));
    private static final String INITIAL_FLOOR = "F1";
    private static final boolean INITIAL_DOOR_STATE = false;
    public static final long MIN_DOOR_PAUSE_TIME = (long) (0.4 * 1000);
    private static final long MIN_SCHE_DOOR_PAUSE_TIME = 1000;
    private static final long MIN_UPDATE_TIME = 1000;
    private static final long RATED_SPEED = (long) (0.4 * 1000);
    private static final long TWINS_SPEED = (long) (0.2 * 1000);
    public static final int RATED_LOAD = 6;

    private final int id;
    private int shaftId;
    private int twinsId = -1;
    private final HashSet<Integer> positions = new HashSet<>(POSITIONS.values());
    private int transferPosition = -1;

    private int position = POSITIONS.get(INITIAL_FLOOR);
    private int direction = 0;
    private long speed = RATED_SPEED;
    private long lastOpenTime;
    private boolean doorState = INITIAL_DOOR_STATE;

    private final RequestQueue scanQueue;
    private final Map<Integer, RequestQueue> dispatchQueues;
    private final RequestSet receiveSet;
    private final RequestSet takeSet;

    private static final Map<Integer, CyclicBarrier> updateBarriers = Collections.unmodifiableMap(
        Arrays.stream(IDS).boxed()
        .collect(Collectors.toMap(id -> id, id -> new CyclicBarrier(2)))
    );
    private static final Map<Integer, ReentrantLock> transferLocks = Collections.unmodifiableMap(
        Arrays.stream(IDS).boxed()
        .collect(Collectors.toMap(id -> id, id -> new ReentrantLock()))
    );

    public Elevator(int id, RequestQueue scanQueue, Map<Integer, RequestQueue> dispatchQueues) {
        this.id = id;
        this.shaftId = id;
        this.scanQueue = scanQueue;
        this.receiveSet = new RequestSet();
        this.takeSet = new RequestSet();
        this.dispatchQueues = dispatchQueues;
    }

    @Override
    public void run() {
        while (true) {
            Task task = Scheduler.getTask(positions, transferPosition,
                position, direction, doorState, dispatchQueues.get(id), receiveSet, takeSet);
            if (task instanceof PauseTask) {
                if (!Monitor.instance.tryAwaitToExecute(id)) {
                    if (doorState) {
                        execute(new CloseTask(MIN_DOOR_PAUSE_TIME));
                    }
                    break;
                }
            } else {
                execute(task);
            }
        }
    }

    private void delay(long time) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(time));
    }

    private long printf(String s, Object... args) {
        return TimableOutput.println(String.format(s, args));
    }

    private void execute(Task task) {
        if (task instanceof OpenTask) {
            lastOpenTime = printf("OPEN-%s-%d", FLOORS.get(position), id);
            doorState = true;
        } else if (task instanceof CloseTask) {
            CloseTask closeTask = (CloseTask) task;
            long pauseTime = closeTask.getPauseTime() - (System.currentTimeMillis() - lastOpenTime);
            if (pauseTime > 0) {
                delay(pauseTime);
            }
            printf("CLOSE-%s-%d", FLOORS.get(position), id);
            doorState = false;
        } else if (task instanceof InTask) {
            RequestSet inSet = ((InTask) task).getIn();
            for (Request request : inSet) {
                int personId = ((PersonRequest) request).getPersonId();
                printf("IN-%d-%s-%d", personId, FLOORS.get(position), id);
            }
            inSet.forEach(receiveSet::remove);
            takeSet.addAll(inSet);
        } else if (task instanceof OutTask) {
            RequestSet outSet = ((OutTask) task).getOut();
            for (Request request : outSet) {
                int personId = ((PersonRequest) request).getPersonId();
                if (position == POSITIONS.get(((PersonRequest) request).getToFloor())) {
                    printf("OUT-S-%d-%s-%d", personId, FLOORS.get(position), id);
                    Monitor.instance.decreaseRequestCount();
                } else {
                    printf("OUT-F-%d-%s-%d", personId, FLOORS.get(position), id);
                    int priority = ((PersonRequest) request).getPriority();
                    String fromFloor = Elevator.FLOORS.get(position);
                    String toFloor = ((PersonRequest) request).getToFloor();
                    Request newReq = new PersonRequest(fromFloor, toFloor, personId, priority);
                    RequestComparator.timeMap.put(newReq, RequestComparator.timeMap.get(request));
                    if (position == transferPosition) {
                        dispatchQueues.get(twinsId).put(newReq);
                        Monitor.instance.signalForExecute(twinsId);
                    } else {
                        scanQueue.add(newReq);
                        Monitor.instance.signalForDispatch();
                    }
                }
            }
            outSet.forEach(takeSet::remove);
        } else if (task instanceof TurnTask) {
            direction = ((TurnTask) task).getDirection();
        } else if (task instanceof ReceiveTask) {
            executeReceiveTask();
        } else if (task instanceof MoveTask) {
            executeMoveTask();
        } else if (task instanceof ScheTask) {
            executeScheTask((ScheTask) task);
        } else if (task instanceof UpdateTask) {
            executeUpdateTask((UpdateTask) task);
        }
    }

    private void executeReceiveTask() {
        for (Request request : dispatchQueues.get(id)) {
            if (request instanceof PersonRequest) {
                printf("RECEIVE-%d-%d", ((PersonRequest) request).getPersonId(), id);
                receiveSet.add(request);
            } else if (request instanceof TransferRequest) {
                if (position == transferPosition) {
                    int toDirection = id == shaftId ? -1 : 1;
                    if (toDirection != direction) {
                        execute(new TurnTask(toDirection));
                    }
                    execute(new MoveTask());
                }
                dispatchQueues.get(id).remove(request);
            }
        }
        dispatchQueues.get(id).removeAll(receiveSet);
    }

    private void executeMoveTask() {
        if (position + direction != transferPosition || transferLocks.get(shaftId).tryLock()) {
            delay(speed);
        } else {
            TransferRequest transferRequest = new TransferRequest();
            RequestComparator.timeMap.put(transferRequest, System.nanoTime());
            dispatchQueues.get(twinsId).put(transferRequest);
            Monitor.instance.signalForExecute(twinsId);
            transferLocks.get(shaftId).lock();
        }
        position = position + direction;
        printf("ARRIVE-%s-%d", FLOORS.get(position), id);
        if (position - direction == transferPosition) {
            transferLocks.get(shaftId).unlock();
        }
    }

    private void executeScheTask(ScheTask scheTask) {
        ScheRequest scheRequest = scheTask.getRequest();
        printf("SCHE-BEGIN-%d", id);
        int toPosition = POSITIONS.get(scheRequest.getToFloor());
        execute(new TurnTask(Integer.signum(toPosition - position)));
        long originSpeed = speed;
        speed = (long) (scheRequest.getSpeed() * 1000);
        while (position != toPosition) {
            execute(new MoveTask());
        }
        speed = originSpeed;
        execute(new OpenTask());
        execute(new OutTask((RequestSet) takeSet.clone()));
        scanQueue.addAll(receiveSet);
        receiveSet.clear();
        dispatchQueues.get(id).remove(scheRequest);
        Monitor.instance.decreaseRequestCount();
        Monitor.instance.signalForDispatch();
        execute(new CloseTask(MIN_SCHE_DOOR_PAUSE_TIME));
        printf("SCHE-END-%d", id);
    }

    private void executeUpdateTask(UpdateTask updateTask) {
        UpdateRequest updateRequest = updateTask.getRequest();
        final int elevatorId = updateRequest.getElevatorAId();
        shaftId = updateRequest.getElevatorBId();
        transferPosition = POSITIONS.get(updateRequest.getTransferFloor());
        speed = TWINS_SPEED;
        try {
            updateBarriers.get(shaftId).await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
        if (id == elevatorId) {
            twinsId = shaftId;
            positions.removeIf(position -> position < transferPosition);
            position = transferPosition + 1;
        } else if (id == shaftId) {
            printf("UPDATE-BEGIN-%d-%d", elevatorId, shaftId);
            twinsId = elevatorId;
            positions.removeIf(position -> position > transferPosition);
            position = transferPosition - 1;
            delay(MIN_UPDATE_TIME);
            printf("UPDATE-END-%d-%d", elevatorId, shaftId);
            Monitor.instance.decreaseRequestCount();
        }
        dispatchQueues.get(id).remove(updateRequest);
        try {
            updateBarriers.get(shaftId).await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }
}
