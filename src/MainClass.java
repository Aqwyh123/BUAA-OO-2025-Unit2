import com.oocourse.elevator3.TimableOutput;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class MainClass {
    public static void main(String[] args) {
        TimableOutput.initStartTimestamp();

        new Thread(Monitor.instance).start();

        RequestQueue scanQueue = new RequestQueue();
        new Thread(new RequestScanner(scanQueue)).start();

        Map<Integer, RequestQueue> dispatchQueues = Collections.unmodifiableMap(
            Arrays.stream(Elevator.IDS).boxed()
            .collect(Collectors.toMap(id -> id, id -> new RequestQueue()))
        );
        new Thread(new Dispatcher(scanQueue, dispatchQueues)).start();

        for (int id : Elevator.IDS) {
            new Thread(new Elevator(id, scanQueue, dispatchQueues)).start();
        }
    }
}
