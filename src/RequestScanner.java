import com.oocourse.elevator3.ElevatorInput;
import com.oocourse.elevator3.Request;

import java.io.IOException;

public class RequestScanner implements Runnable {
    private final RequestQueue scanQueue;

    public RequestScanner(RequestQueue scanQueue) {
        this.scanQueue = scanQueue;
    }

    @Override
    public void run() {
        ElevatorInput elevatorInput = new ElevatorInput(System.in);
        while (true) {
            Request request = elevatorInput.nextRequest();
            if (request != null) {
                RequestComparator.timeMap.put(request, System.nanoTime());
                scanQueue.put(request);
                Monitor.instance.increaseRequestCount();
                Monitor.instance.signalForDispatch();
            } else {
                Monitor.instance.setScannerEnd();
                break;
            }
        }
        try {
            elevatorInput.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

