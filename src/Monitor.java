import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class Monitor implements Runnable {
    public static final Monitor instance = new Monitor();

    private volatile boolean isEnd = false;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private volatile boolean isScannerEnd = false;

    private volatile boolean isDispatcherEnd = false;
    private final ReentrantLock dispatchLock = new ReentrantLock();
    private final Condition dispatchCondition = dispatchLock.newCondition();

    private final ConcurrentHashMap<Integer, Boolean> isElevatorEnd;
    private final ConcurrentHashMap<Integer, ReentrantLock> elevatorLocks;
    private final ConcurrentHashMap<Integer, Condition> elevatorCondition;

    private Monitor() {
        isElevatorEnd = new ConcurrentHashMap<>();
        elevatorLocks = new ConcurrentHashMap<>();
        elevatorCondition = new ConcurrentHashMap<>();
        for (int id : MainClass.IDS) {
            isElevatorEnd.put(id, false);
            elevatorLocks.put(id, new ReentrantLock());
            elevatorCondition.put(id, elevatorLocks.get(id).newCondition());
        }
    }

    @Override
    public void run() {
        while (true) {
            if (canSetEnd()) {
                isEnd = true;
                do {
                    signalForDispatch();
                    for (int id : MainClass.IDS) {
                        signalForExecute(id);
                    }
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
                } while (!canSetEnd());
                break;
            } else {
                waitToEnd();
            }
        }
    }

    public boolean isEnd() {
        return isEnd;
    }

    private boolean canSetEnd() {
        return isScannerEnd && isDispatcherEnd && isElevatorEnd.values().stream().allMatch(e -> e);
    }

    public void setScannerEnd(boolean isEnd) {
        boolean before = isScannerEnd;
        isScannerEnd = isEnd;
        if (!before && isEnd) {
            signalForEnd();
        }
    }

    public void setDispatcherEnd(boolean isEnd) {
        boolean before = isDispatcherEnd;
        isDispatcherEnd = isEnd;
        if (!before && isEnd) {
            signalForEnd();
        }
    }

    public void setElevatorEnd(int id, boolean isEnd) {
        boolean before = isElevatorEnd.get(id);
        isElevatorEnd.put(id, isEnd);
        if (!before && isEnd) {
            signalForEnd();
        }
    }

    private void waitToEnd() {
        lock.lock();
        try {
            condition.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private void signalForEnd() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void waitToDispatch() {
        dispatchLock.lock();
        try {
            dispatchCondition.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            dispatchLock.unlock();
        }
    }

    public void signalForDispatch() {
        dispatchLock.lock();
        try {
            dispatchCondition.signalAll();
        } finally {
            dispatchLock.unlock();
        }
    }

    public void waitToExecute(int id) {
        ReentrantLock elevatorLock = elevatorLocks.get(id);
        Condition elevatorCondition = this.elevatorCondition.get(id);
        elevatorLock.lock();
        try {
            elevatorCondition.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            elevatorLock.unlock();
        }
    }

    public void signalForExecute(int id) {
        ReentrantLock elevatorLock = elevatorLocks.get(id);
        Condition elevatorCondition = this.elevatorCondition.get(id);
        elevatorLock.lock();
        try {
            elevatorCondition.signalAll();
        } finally {
            elevatorLock.unlock();
        }
    }
}
