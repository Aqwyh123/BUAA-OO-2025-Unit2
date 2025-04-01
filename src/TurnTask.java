public class TurnTask implements Task {
    private final int direction;

    public TurnTask(int direction) {
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }
}
