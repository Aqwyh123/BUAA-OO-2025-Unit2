import com.oocourse.elevator1.ElevatorInput;
import com.oocourse.elevator1.Request;

import java.io.IOException;
import java.util.concurrent.PriorityBlockingQueue;

public class RequestScanner implements Runnable {
    private final PriorityBlockingQueue<Request> requests;

    public RequestScanner(PriorityBlockingQueue<Request> requests) {
        this.requests = requests;
    }

    @Override
    public void run() {
        ElevatorInput elevatorInput = new ElevatorInput(System.in);
        while (true) {
            Request request = elevatorInput.nextRequest();
            if (request == null) {
                requests.put(new NullRequest());
                break;
            } else {
                requests.put(request);
            }
        }
        try {
            elevatorInput.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
