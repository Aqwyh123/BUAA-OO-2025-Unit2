import com.oocourse.elevator1.TimableOutput;

import java.util.concurrent.locks.LockSupport;

public class Door {
    private final int id;
    private static final long minPauseTime = (long) (0.4 * 1000);
    private long openTime;
    private boolean doorState;

    public Door(int id, boolean initialState) {
        this.id = id;
        this.openTime = System.currentTimeMillis();
        this.doorState = initialState;
    }

    public boolean getState() {
        return doorState;
    }

    public void open(String floor) {
        if (doorState) {
            return;
        }
        TimableOutput.println(String.format("OPEN-%s-%d", floor, id));
        openTime = System.currentTimeMillis();
        doorState = true;
    }

    public void close(String floor) {
        if (!doorState) {
            return;
        }
        doorState = false;
        long pauseTime = minPauseTime - (System.currentTimeMillis() - openTime);
        if (pauseTime > 0) {
            LockSupport.parkNanos(pauseTime * 1_000_000);
        }
        TimableOutput.println(String.format("CLOSE-%s-%d", floor, id));
    }
}
