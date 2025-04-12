import com.oocourse.elevator3.UpdateRequest;

public class UpdateTask implements Task {
    private final UpdateRequest updateRequest;

    public UpdateTask(UpdateRequest updateRequest) {
        this.updateRequest = updateRequest;
    }

    public UpdateRequest getRequest() {
        return updateRequest;
    }
}
