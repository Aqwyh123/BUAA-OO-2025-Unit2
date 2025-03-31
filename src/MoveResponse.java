public class MoveResponse implements Response {
    private final int direction;

    public MoveResponse(int direction) {
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }
}
