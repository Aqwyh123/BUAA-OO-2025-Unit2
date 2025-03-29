public class Door {
    private static final long minPauseTime = (long) (0.4 * 1000);
    private long openTime;
    private boolean doorState;

    public Door(boolean initialState) {
        this.openTime = System.currentTimeMillis();
        this.doorState = initialState;
    }

    public boolean getState() {
        return doorState;
    }

    public void open() {
        if (!doorState) {
            doorState = true;
            openTime = System.currentTimeMillis();
        }
    }

    public void close() {
        long sleepTime = System.currentTimeMillis() - openTime;
        if (sleepTime < minPauseTime) {
            try {
                Thread.sleep(minPauseTime - sleepTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        doorState = false;
    }
}
