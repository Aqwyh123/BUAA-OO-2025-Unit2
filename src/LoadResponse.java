import com.oocourse.elevator1.Request;

public class LoadResponse implements Response {
    private final Request request;

    public LoadResponse(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }
}
