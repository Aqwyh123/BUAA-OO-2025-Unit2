import com.oocourse.elevator2.PersonRequest;

import java.util.PriorityQueue;

public class ProcessingQueue extends PriorityQueue<PersonRequest> {
    public ProcessingQueue() {
        super(RequestComparator.priorityComparator);
    }
}
