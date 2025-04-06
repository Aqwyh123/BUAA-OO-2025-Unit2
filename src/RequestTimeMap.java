import com.oocourse.elevator2.Request;

import java.util.concurrent.ConcurrentHashMap;

public class RequestTimeMap {
    public static final ConcurrentHashMap<Request, Long> instance = new ConcurrentHashMap<>();

    private RequestTimeMap() {}
}
