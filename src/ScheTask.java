import com.oocourse.elevator2.ScheRequest;

public class ScheTask implements Task {
    private final ScheRequest request;

    public ScheTask(ScheRequest request) {
        this.request = request;
    }

    public ScheRequest getRequest() {
        return request;
    }
}
