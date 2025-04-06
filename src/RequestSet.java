import com.oocourse.elevator2.Request;

import java.util.TreeSet;

public class RequestSet extends TreeSet<Request> {
    public RequestSet() {
        super(RequestComparator.priorityComparator);
    }
}
