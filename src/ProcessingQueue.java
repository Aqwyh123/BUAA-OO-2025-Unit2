import com.oocourse.elevator1.Request;

import java.util.PriorityQueue;

public class ProcessingQueue extends PriorityQueue<Request> {
    public ProcessingQueue() {
        super(RequestComparator.priorityComparator);
    }
}
