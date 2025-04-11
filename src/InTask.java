public class InTask implements Task {
    private final RequestSet inSet;

    public InTask(RequestSet inSet) {
        this.inSet = inSet;
    }

    public RequestSet getIn() {
        return inSet;
    }
}
