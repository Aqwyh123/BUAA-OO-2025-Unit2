import com.oocourse.elevator2.ElevatorInput;
import com.oocourse.elevator2.Request;

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
                scanQueue.put(request);
                MainClass.monitor.signalForDispatch();
            } else {
                MainClass.monitor.setScannerEnd(true);
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
