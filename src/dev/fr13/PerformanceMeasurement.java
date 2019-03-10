package dev.fr13;

import java.util.concurrent.TimeUnit;

public class PerformanceMeasurement {

    private static long startTime;

    public static void setStartTime() {
        startTime = System.currentTimeMillis();
    }

    public static void printSpentTime() {
        long estimatedTime  = System.currentTimeMillis() - startTime;
        System.out.println("Spent processing time: " + String.format(
                "%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(estimatedTime),
                TimeUnit.MILLISECONDS.toSeconds(estimatedTime)));
    }
}
