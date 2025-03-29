import com.oocourse.elevator1.Request;

public class UnloadResponse implements Response {
    private final Request request;

    public UnloadResponse(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }
}
