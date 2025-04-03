import com.oocourse.elevator2.PersonRequest;
import com.oocourse.elevator2.ScheRequest;
import com.oocourse.elevator2.Request;

import java.util.Comparator;

public class RequestComparator {
    private static final int MAX_PRIORITY = 100;
    private static final int MIN_PRIORITY = 1;
    public static final Comparator<Request> priorityComparator = new PriorityComparator();

    private static class PriorityComparator implements Comparator<Request> {
        @Override
        public int compare(Request o1, Request o2) {  // Higher priority first
            int priority1;
            if (o1 instanceof PersonRequest) {
                priority1 = ((PersonRequest) o1).getPriority();
            } else if (o1 instanceof ScheRequest) {
                priority1 = MAX_PRIORITY + 1;
            } else {
                priority1 = MIN_PRIORITY - 1;
            }
            int priority2;
            if (o2 instanceof PersonRequest) {
                priority2 = ((PersonRequest) o2).getPriority();
            } else if (o2 instanceof ScheRequest) {
                priority2 = MAX_PRIORITY + 1;
            } else {
                priority2 = MIN_PRIORITY - 1;
            }
            return Integer.compare(priority2, priority1);
        }
    }
}
