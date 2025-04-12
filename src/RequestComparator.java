import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.ScheRequest;
import com.oocourse.elevator3.Request;
import com.oocourse.elevator3.UpdateRequest;

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
        public int compare(Request request1, Request request2) {
            int priority1 = getPriority(request1);
            int priority2 = getPriority(request2);
            int priorityDiff = Integer.compare(priority2, priority1); // Higher priority first
            if (priorityDiff != 0) {
                return priorityDiff;
            } else {
                int timeDiff = Long.compare(timeMap.get(request1), timeMap.get(request2));
                if (timeDiff != 0) { // lower time first
                    return timeDiff;
                } else {
                    return request1.toString().compareTo(request2.toString()); // default comparison
                }
            }
        }

        private int getPriority(Request request) {
            int priority;
            if (request instanceof PersonRequest) {
                priority = ((PersonRequest) request).getPriority();
            } else if (request instanceof ScheRequest || request instanceof UpdateRequest) {
                priority = MAX_PRIORITY + 2;
            } else if (request instanceof TransferRequest) {
                priority = MAX_PRIORITY + 1;
            } else {
                priority = MIN_PRIORITY - 1;
            }
            return priority;
        }
    }
}
