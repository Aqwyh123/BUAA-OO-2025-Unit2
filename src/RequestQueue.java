import com.oocourse.elevator2.Request;

import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;

public class RequestQueue implements Iterable<Request> {
    private final PriorityBlockingQueue<Request> queue;

    public RequestQueue() {
        this.queue = new PriorityBlockingQueue<>(100, RequestComparator.priorityComparator);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public Request peek() {
        return queue.peek();
    }

    public void put(Request request) {
        queue.put(request);
    }

    public Request poll() {
        return queue.poll();
    }

    public boolean remove(Request request) {
        return queue.remove(request);
    }

    @Override
    public Iterator<Request> iterator() {
        return queue.iterator();
    }
}
