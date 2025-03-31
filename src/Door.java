import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Door {
    private static final long minPauseTime = (long) (0.4 * 1000);
    private long openNanoTime;
    private boolean doorState;

    public Door(boolean initialState) {
        this.openNanoTime = System.nanoTime();
        this.doorState = initialState;
    }

    public boolean open() {
        if (doorState) {
            return false;
        }
        doorState = true;
        openNanoTime = System.nanoTime();
        return true;
    }

    public boolean close() {
        if (!doorState) {
            return false;
        }
        long pauseNanoTime = TimeUnit.MILLISECONDS.toNanos(minPauseTime) - (System.nanoTime() - openNanoTime);
        if (pauseNanoTime > 0) {
            LockSupport.parkNanos(pauseNanoTime);
        }
        doorState = false;
        return true;
    }
}
