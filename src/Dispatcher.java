import com.oocourse.elevator2.PersonRequest;
import com.oocourse.elevator2.ScheRequest;
import com.oocourse.elevator2.Request;

import java.util.HashMap;

public class Dispatcher implements Runnable {
    private int index = 0;
    private final RequestQueue scanQueue;
    private final HashMap<Integer, RequestQueue> dispatchQueues;

    Dispatcher(RequestQueue scanQueue, HashMap<Integer, RequestQueue> dispatchQueues) {
        this.scanQueue = scanQueue;
        this.dispatchQueues = dispatchQueues;
    }

    @Override
    public void run() {
        while (true) {
            if (scanQueue.isEmpty() && Monitor.instance.isEnd()) {
                break;
            } else {
                Request request = scanQueue.poll();
                if (request != null) {
                    Monitor.instance.setDispatcherEnd(false);
                    int dispatchId = dispatch(request);
                    Monitor.instance.signalForExecute(dispatchId);
                } else {
                    Monitor.instance.setDispatcherEnd(true);
                    Monitor.instance.waitToDispatch();
                }
            }
        }
    }

    private int dispatch(Request request) {
        if (request instanceof PersonRequest) {
            int dispatchId = MainClass.IDS[index];
            dispatchQueues.get(dispatchId).put(request);
            index = (index + 1) % MainClass.IDS.length;
            return dispatchId;
        } else if (request instanceof ScheRequest) {
            ScheRequest scheRequest = (ScheRequest) request;
            int elevatorId = scheRequest.getElevatorId();
            dispatchQueues.get(elevatorId).put(scheRequest);
            return elevatorId;
        }
        return -1;
    }
}
