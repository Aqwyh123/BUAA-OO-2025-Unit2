public class OutTask implements Task {
    private final RequestSet outSet;

    public OutTask(RequestSet outSet) {
        this.outSet = outSet;
    }

    public RequestSet getOut() {
        return outSet;
    }
}
