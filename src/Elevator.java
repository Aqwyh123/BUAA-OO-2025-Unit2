import com.oocourse.elevator2.PersonRequest;
import com.oocourse.elevator2.Request;
import com.oocourse.elevator2.ScheRequest;
import com.oocourse.elevator2.TimableOutput;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
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
    private static final long MIN_DOOR_PAUSE_TIME = (long) (0.4 * 1000);
    private static final long SCHE_STOP_TIME = 1000;
    public static final long RATED_SPEED = (long) (0.4 * 1000);
    public static final int RATED_LOAD = 6;

    private final int id;
    private final ReentrantLock lock;
    private final CyclicBarrier barrier;

    private int position;
    private int direction;
    private long lastOpenTime;
    private boolean doorState;

    private final RequestQueue dispatchingQueue;
    private final RequestQueue waitingQueue;
    private final ProcessingQueue takingQueue;

    public Elevator(int id, ReentrantLock lock, CyclicBarrier barrier,
        RequestQueue dispatchingQueue, RequestQueue waitingQueue) {
        this.id = id;
        this.lock = lock;
        this.barrier = barrier;
        this.position = POSITIONS.get(INITIAL_FLOOR);
        this.direction = 0;
        this.doorState = INITIAL_DOOR_STATE;
        this.dispatchingQueue = dispatchingQueue;
        this.waitingQueue = waitingQueue;
        this.takingQueue = new ProcessingQueue();
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (waitingQueue.isEmpty() && takingQueue.isEmpty()) {
                    barrier.await();
                    break;
                }
                Task task = Scheduler.getTask(position, direction, doorState,
                    waitingQueue, takingQueue);
                boolean shouldContinue = execute(task);
                if (!shouldContinue) {
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

    private void delay(long time) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(time));
    }

    private long output(String content) {
        return TimableOutput.println(content);
    }

    private void redispatch(Request request) {
        dispatchingQueue.put(request);
        barrier.reset();
    }

    private boolean execute(Task task) {
        if (task instanceof OpenTask) {
            lastOpenTime = output(String.format("OPEN-%s-%d", FLOORS.get(position), id));
            doorState = true;
        } else if (task instanceof CloseTask) {
            long pauseTime = MIN_DOOR_PAUSE_TIME - (System.currentTimeMillis() - lastOpenTime);
            if (pauseTime > 0) {
                delay(pauseTime);
            }
            output(String.format("CLOSE-%s-%d", FLOORS.get(position), id));
            doorState = false;
        } else if (task instanceof InTask) {
            ArrayList<PersonRequest> inQueue = ((InTask) task).getIn();
            for (PersonRequest request : inQueue) {
                int personId = request.getPersonId();
                output(String.format("IN-%d-%s-%d", personId, FLOORS.get(position), id));
            }
            waitingQueue.removeAll(inQueue);
            takingQueue.addAll(inQueue);
        } else if (task instanceof OutTask) {
            ArrayList<PersonRequest> outQueue = ((OutTask) task).getOut();
            for (PersonRequest request : outQueue) {
                int personId = request.getPersonId();
                int toPosition = POSITIONS.get(request.getToFloor());
                if (position == toPosition) {
                    output(String.format("OUT-S-%d-%s-%d", personId, FLOORS.get(position), id));
                } else {
                    output(String.format("OUT-F-%d-%s-%d", personId, FLOORS.get(position), id));
                    redispatch(request);
                }
            }
            takingQueue.removeAll(outQueue);
        } else if (task instanceof MoveTask) {
            delay(((MoveTask) task).getSpeed());
            position = position + direction;
            output(String.format("ARRIVE-%s-%d", FLOORS.get(position), id));
        } else if (task instanceof TurnTask) {
            direction = ((TurnTask) task).getDirection();
        } else if (task instanceof ScheTask) {
            ScheRequest scheRequest = ((ScheTask) task).getRequest();
            lock.lock();
            output(String.format("SCHE-BEGIN-%d", id));
            int toPosition = POSITIONS.get(scheRequest.getToFloor());
            direction = Integer.signum(toPosition - position);
            while (position != toPosition) {
                execute(new MoveTask((long) (scheRequest.getSpeed() * 1000)));
            }
            execute(new OpenTask());
            delay(SCHE_STOP_TIME);
            execute(new CloseTask());
            output(String.format("SCHE-END-%d", id));
            lock.unlock();
            waitingQueue.remove(scheRequest);
        }
        return !(task instanceof StopTask);
    }
}
