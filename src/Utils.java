import com.oocourse.elevator3.TimableOutput;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Utils {
    public static long printf(String format, Object... args) {
        return TimableOutput.println(String.format(format, args));
    }

    public static void sleep(long millis) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(millis));
    }
}
