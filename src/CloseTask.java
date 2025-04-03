public class CloseTask implements Task {
    private final long pauseTime;

    public CloseTask(long pauseTime) {
        this.pauseTime = pauseTime;
    }

    public long getPauseTime() {
        return pauseTime;
    }
}
