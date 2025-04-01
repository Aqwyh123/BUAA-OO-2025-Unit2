import com.oocourse.elevator1.Request;

import java.util.ArrayList;

public class OutTask implements Task {
    private final ArrayList<Request> outQueue;

    public OutTask(ArrayList<Request> outQueue) {
        this.outQueue = outQueue;
    }

    public ArrayList<Request> getOut() {
        return outQueue;
    }
}
