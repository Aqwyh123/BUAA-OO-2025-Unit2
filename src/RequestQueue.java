import com.oocourse.elevator2.Request;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RequestQueue implements Iterable<Request> {
    private final PriorityBlockingQueue<Request> queue;
    private boolean isEnd;

    public RequestQueue() {
        this.queue = new PriorityBlockingQueue<>(100, RequestComparator.priorityComparator);
        this.isEnd = false;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setEnd() {
        this.isEnd = true;
    }

    public Request peek() {
        if (isEmpty() && isEnd) {
            return new EndRequest();
        } else {
            return queue.peek();
        }
    }

    public void put(Request request) {
        queue.put(request);
    }

    public Request take() {
        if (isEnd) {
            return new EndRequest();
        } else {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Request[] toArray() {
        Request[] array = queue.toArray(new Request[0]);
        Arrays.sort(array, RequestComparator.priorityComparator);
        return array;
    }

    public void remove(Request request) {
        queue.remove(request);
    }

    public void removeAll(Collection<? extends Request> c) {
        queue.removeAll(c);
    }

    public void removeIf(Predicate<? super Request> filter) {
        queue.removeIf(filter);
    }

    @Override
    public Iterator<Request> iterator() {
        return queue.iterator();
    }

    @Override
    public void forEach(Consumer<? super Request> consumer) {
        queue.forEach(consumer);
    }
}
