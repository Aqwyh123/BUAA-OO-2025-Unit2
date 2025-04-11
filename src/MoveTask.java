public class MoveTask implements Task {
    private final long speed;

    public MoveTask(long speed) {
        this.speed = speed;
    }

    public long getSpeed() {
        return speed;
    }
}
