import com.oocourse.elevator2.PersonRequest;
import com.oocourse.elevator2.Request;

import java.util.Arrays;
import java.util.PriorityQueue;

public class ProcessingQueue extends PriorityQueue<PersonRequest> {
    public ProcessingQueue() {
        super(RequestComparator.priorityComparator);
    }

    @Override
    public Request[] toArray() {
        Request[] requests = super.toArray(new Request[0]);
        Arrays.sort(requests, RequestComparator.priorityComparator);
        return requests;
    }
}
