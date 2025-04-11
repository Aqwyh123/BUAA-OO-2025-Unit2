import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Monitor implements Runnable {
    public static final Monitor instance = new Monitor();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final AtomicBoolean isScannerEnd = new AtomicBoolean(false);
    private final AtomicInteger requestCount = new AtomicInteger(0);

    private final ReentrantLock dispatchLock = new ReentrantLock();
    private final Condition dispatchCondition = dispatchLock.newCondition();

    private final ConcurrentHashMap<Integer, ReentrantLock> elevatorLocks;
    private final ConcurrentHashMap<Integer, Condition> elevatorConditions;

    private Monitor() {
        elevatorLocks = new ConcurrentHashMap<>();
        elevatorConditions = new ConcurrentHashMap<>();
        for (int id : MainClass.IDS) {
            elevatorLocks.put(id, new ReentrantLock());
            elevatorConditions.put(id, elevatorLocks.get(id).newCondition());
        }
    }

    @Override
    public void run() {
        while (true) {
            if (!tryWaitToMonitor()) {
                signalForDispatch();
                for (int id : MainClass.IDS) {
                    signalForExecute(id);
                }
                break;
            }
        }
    }

    public void setScannerEnd() {
        isScannerEnd.set(true);
        signalForMonitor();
    }

    public void increaseRequestCount() {
        if (requestCount.incrementAndGet() == 0) {
            signalForMonitor();
        }
    }

    public void decreaseRequestCount() {
        if (requestCount.decrementAndGet() == 0) {
            signalForMonitor();
        }
    }

    private boolean tryWaitToMonitor() {
        lock.lock();
        boolean isContinue = !(isScannerEnd.get() && requestCount.get() == 0);
        try {
            if (isContinue) {
                condition.await();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        return isContinue;
    }

    private void signalForMonitor() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean tryWaitToDispatch() {
        dispatchLock.lock();
        boolean isContinue = !(isScannerEnd.get() && requestCount.get() == 0);
        try {
            if (isContinue) {
                dispatchCondition.await();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            dispatchLock.unlock();
        }
        return isContinue;
    }

    public void signalForDispatch() {
        dispatchLock.lock();
        try {
            dispatchCondition.signalAll();
        } finally {
            dispatchLock.unlock();
        }
    }

    public boolean tryWaitToExecute(int id) {
        ReentrantLock elevatorLock = elevatorLocks.get(id);
        elevatorLock.lock();
        boolean isContinue = !(isScannerEnd.get() && requestCount.get() == 0);
        try {
            if (isContinue) {
                elevatorConditions.get(id).await();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            elevatorLock.unlock();
        }
        return isContinue;
    }

    public void signalForExecute(int id) {
        ReentrantLock elevatorLock = elevatorLocks.get(id);
        elevatorLock.lock();
        try {
            elevatorConditions.get(id).signalAll();
        } finally {
            elevatorLock.unlock();
        }
    }
}
