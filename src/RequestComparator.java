import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;

import java.util.Comparator;

public class RequestComparator {
    public static final Comparator<Request> priorityComparator = new PriorityComparator();

    private static class PriorityComparator implements Comparator<Request> {
        @Override
        public int compare(Request o1, Request o2) {  // Higher priority first
            if (o1 instanceof NullRequest && o2 instanceof NullRequest) {
                return 0;
            } else if (o1 instanceof NullRequest) {
                return 1;
            } else if (o2 instanceof NullRequest) {
                return -1;
            } else {
                PersonRequest personRequest1 = (PersonRequest) o1;
                PersonRequest personRequest2 = (PersonRequest) o2;
                return Integer.compare(personRequest2.getPriority(), personRequest1.getPriority());
            }
        }
    }
}
