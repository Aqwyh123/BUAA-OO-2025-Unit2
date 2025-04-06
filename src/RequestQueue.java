import com.oocourse.elevator2.Request;

import java.util.concurrent.PriorityBlockingQueue;

public class RequestQueue extends PriorityBlockingQueue<Request> {
    public RequestQueue() {
        super(100, RequestComparator.priorityComparator);
    }
}
