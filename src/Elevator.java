import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.TimableOutput;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public class Elevator implements Runnable {
    public static final int[] IDS = {1, 2, 3, 4, 5, 6};
    public static final List<String> FLOORS = Collections.unmodifiableList(Arrays.asList("B4", "B3", "B2", "B1", "F1", "F2", "F3", "F4", "F5", "F6", "F7"));
    public static final Map<String, Integer> POSITIONS = Collections.unmodifiableMap(FLOORS.stream().collect(Collectors.toMap(floor -> floor, FLOORS::indexOf)));
    public static final String initialFloor = "F1";
    public static final boolean initialDoorState = false;
    public static final int ratedLoad = 6;
    private static final long ratedSpeed = (long) (0.4 * 1000);

    private final int id;
    private int position;
    private final Door door;
    private final Scheduler scheduler;
    private final ResponseQueue responseQueue = new ResponseQueue();

    public Elevator(int id, RequestQueue waitingQueue) {
        this.id = id;
        this.position = POSITIONS.get(initialFloor);
        this.door = new Door(initialDoorState);
        this.scheduler = new Scheduler(position, waitingQueue, responseQueue);
    }

    @Override
    public void run() {
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.start();
        while (true) {
            try {
                Response action = responseQueue.take();
                if (!execute(action)) {
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean execute(Response response) {
        if (response instanceof MoveResponse) {
            MoveResponse moveResponse = (MoveResponse) response;
            if (door.close()) {
                TimableOutput.println(String.format("CLOSE-%s-%d", FLOORS.get(position), id));
            }
            int toPosition = position + moveResponse.getDirection();
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(ratedSpeed));
            TimableOutput.println(String.format("ARRIVE-%s-%d", FLOORS.get(toPosition), id));
            position = toPosition;
        } else if (response instanceof LoadResponse) {
            LoadResponse loadResponse = (LoadResponse) response;
            if (door.open()) {
                TimableOutput.println(String.format("OPEN-%s-%d", FLOORS.get(position), id));
            }
            int personId = ((PersonRequest) loadResponse.getRequest()).getPersonId();
            TimableOutput.println(String.format("IN-%d-%s-%d", personId, FLOORS.get(position), id));
        } else if (response instanceof UnloadResponse) {
            UnloadResponse unloadResponse = (UnloadResponse) response;
            if (door.open()) {
                TimableOutput.println(String.format("OPEN-%s-%d", FLOORS.get(position), id));
            }
            int personId = ((PersonRequest) unloadResponse.getRequest()).getPersonId();
            TimableOutput.println(String.format("OUT-%d-%s-%d", personId, FLOORS.get(position), id));
        }
        return !(response instanceof StopResponse);
    }
}
