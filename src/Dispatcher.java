import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.ScheRequest;
import com.oocourse.elevator3.Request;
import com.oocourse.elevator3.UpdateRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Dispatcher implements Runnable {
    private int index = 0;
    private final RequestQueue scanQueue;
    private final Map<Integer, RequestQueue> dispatchQueues;
    private final HashMap<Integer, HashSet<Integer>> accessiblePositions = new HashMap<>();

    Dispatcher(RequestQueue scanQueue, Map<Integer, RequestQueue> dispatchQueues) {
        this.scanQueue = scanQueue;
        this.dispatchQueues = dispatchQueues;
        for (int id : dispatchQueues.keySet()) {
            accessiblePositions.put(id, new HashSet<>(Elevator.POSITIONS.values()));
        }
    }

    @Override
    public void run() {
        while (true) {
            Request request = scanQueue.poll();
            if (request != null) {
                dispatch(request);
            } else if (!Monitor.instance.tryAwaitToDispatch()) {
                break;
            }
        }
    }

    private void dispatch(Request request) {
        if (request instanceof PersonRequest) {
            while (true) {
                int elevatorId = Elevator.IDS[index];
                int fromPosition = Elevator.POSITIONS.get(((PersonRequest) request).getFromFloor());
                if (accessiblePositions.get(elevatorId).contains(fromPosition)) {
                    dispatchQueues.get(elevatorId).put(request);
                    Monitor.instance.signalForExecute(elevatorId);
                    break;
                }
                index = (index + 1) % Elevator.IDS.length;
            }
        } else if (request instanceof ScheRequest) {
            ScheRequest scheRequest = (ScheRequest) request;
            int elevatorId = scheRequest.getElevatorId();
            dispatchQueues.get(elevatorId).put(scheRequest);
            Monitor.instance.signalForExecute(elevatorId);
        } else if (request instanceof UpdateRequest) {
            UpdateRequest updateRequest = (UpdateRequest) request;
            final int elevatorId = updateRequest.getElevatorAId();
            final int shaftId = updateRequest.getElevatorBId();
            final int transferPosition = Elevator.POSITIONS.get(updateRequest.getTransferFloor());
            dispatchQueues.get(elevatorId).put(updateRequest);
            Monitor.instance.signalForExecute(elevatorId);
            dispatchQueues.get(shaftId).put(updateRequest);
            Monitor.instance.signalForExecute(shaftId);
            accessiblePositions.get(elevatorId).removeIf(position -> position < transferPosition);
            accessiblePositions.get(shaftId).removeIf(position -> position > transferPosition);
        }
    }
}
