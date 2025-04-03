import com.oocourse.elevator2.PersonRequest;

import java.util.ArrayList;

public class InTask implements Task {
    private final ArrayList<PersonRequest> inQueue;

    public InTask(ArrayList<PersonRequest> inQueue) {
        this.inQueue = inQueue;
    }

    public ArrayList<PersonRequest> getIn() {
        return inQueue;
    }
}
