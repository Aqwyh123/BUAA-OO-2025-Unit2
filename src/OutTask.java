import com.oocourse.elevator2.PersonRequest;

import java.util.ArrayList;

public class OutTask implements Task {
    private final ArrayList<PersonRequest> outQueue;

    public OutTask(ArrayList<PersonRequest> outQueue) {
        this.outQueue = outQueue;
    }

    public ArrayList<PersonRequest> getOut() {
        return outQueue;
    }
}
