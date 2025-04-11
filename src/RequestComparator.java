import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.ScheRequest;
import com.oocourse.elevator3.Request;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

public class RequestComparator {
    private static final int MAX_PRIORITY = 100;
    private static final int MIN_PRIORITY = 1;
    public static final Comparator<Request> priorityComparator = new PriorityComparator();
    public static final ConcurrentHashMap<Request, Long> timeMap = new ConcurrentHashMap<>();

    private RequestComparator() {
    }

    private static class PriorityComparator implements Comparator<Request> {
        @Override
        public int compare(Request o1, Request o2) {  // Higher priority first, lower time first
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
            int priorityDiff = Integer.compare(priority2, priority1);
            if (priorityDiff != 0) {
                return priorityDiff;
            } else {
                int timeDiff = Long.compare(timeMap.get(o1), timeMap.get(o2));
                if (timeDiff != 0) {
                    return timeDiff;
                } else {
                    return o1.toString().compareTo(o2.toString());
                }
            }
        }
    }
}
