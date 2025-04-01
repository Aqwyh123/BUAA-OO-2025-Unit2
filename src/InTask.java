import com.oocourse.elevator1.Request;

import java.util.ArrayList;

public class InTask implements Task {
    private final ArrayList<Request> inQueue;

    public InTask(ArrayList<Request> inQueue) {
        this.inQueue = inQueue;
    }

    public ArrayList<Request> getIn() {
        return inQueue;
    }
}
