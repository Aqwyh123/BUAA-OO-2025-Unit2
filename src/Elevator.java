import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;
import com.oocourse.elevator1.TimableOutput;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

public class Elevator implements Runnable {
    public static final int[] availableIds = {1, 2, 3, 4, 5, 6};
    public static final List<String> availableFloors = Collections.unmodifiableList(Arrays.asList(
            "B4", "B3", "B2", "B1", "F1", "F2", "F3", "F4", "F5", "F6", "F7"));
    public static final Map<String, Integer> availablePositions = Collections.unmodifiableMap(
            availableFloors.stream()
                    .collect(Collectors.toMap(floor -> floor, availableFloors::indexOf)));
    private static final String initialFloor = "F1";
    private static final boolean initialDoorState = false;
    public static final int ratedLoad = 6;
    private static final long ratedSpeed = (long) (0.4 * 1000);

    private final int id;
    private int position;
    private final Door door;
    private final Scheduler scheduler;
    private final LinkedBlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();

    public Elevator(int id, PriorityBlockingQueue<Request> waitingQueue) {
        this.id = id;
        this.position = availablePositions.get(initialFloor);
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
            if (door.getState()) {
                door.close();
                TimableOutput.println(String.format("CLOSE-%s-%d",
                        availableFloors.get(position), id));
            }
            int targetPosition = moveResponse.getDirection() ? position + 1 : position - 1;
            try {
                Thread.sleep(ratedSpeed);
                TimableOutput.println(String.format("ARRIVE-%s-%d",
                        availableFloors.get(targetPosition), id));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            position = targetPosition;
        } else if (response instanceof LoadResponse) {
            LoadResponse loadResponse = (LoadResponse) response;
            if (!door.getState()) {
                door.open();
                TimableOutput.println(String.format("OPEN-%s-%d",
                        availableFloors.get(position), id));
            }
            TimableOutput.println(String.format("IN-%d-%s-%d",
                    ((PersonRequest) loadResponse.getRequest()).getPersonId(),
                    availableFloors.get(position), id));
        } else if (response instanceof UnloadResponse) {
            UnloadResponse unloadResponse = (UnloadResponse) response;
            if (!door.getState()) {
                door.open();
                TimableOutput.println(String.format("OPEN-%s-%d",
                        availableFloors.get(position), id));
            }
            TimableOutput.println(String.format("OUT-%d-%s-%d",
                    ((PersonRequest) unloadResponse.getRequest()).getPersonId(),
                    availableFloors.get(position), id));
        }
        return !(response instanceof StopResponse);
    }
}
