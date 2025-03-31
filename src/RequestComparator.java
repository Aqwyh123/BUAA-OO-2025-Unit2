import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;

import java.util.Comparator;

public class RequestComparator {
    public static final Comparator<Request> priorityComparator = new PriorityComparator();

    private static class PriorityComparator implements Comparator<Request> {
        @Override
        public int compare(Request o1, Request o2) {  // Higher priority first
            if (o1 instanceof PersonRequest && o2 instanceof PersonRequest) {
                PersonRequest request1 = (PersonRequest) o1;
                PersonRequest request2 = (PersonRequest) o2;
                return Integer.compare(request2.getPriority(), request1.getPriority());
            } else if (o1 instanceof PersonRequest) {
                return -1;
            } else if (o2 instanceof PersonRequest) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
