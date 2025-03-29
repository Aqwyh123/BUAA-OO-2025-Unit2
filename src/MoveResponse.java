public class MoveResponse implements Response {
    private final boolean direction;

    public MoveResponse(boolean direction) {
        this.direction = direction;
    }

    public boolean getDirection() {
        return direction;
    }
}
