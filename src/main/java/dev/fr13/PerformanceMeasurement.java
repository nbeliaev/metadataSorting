package dev.fr13;

import java.util.concurrent.TimeUnit;

public class PerformanceMeasurement {

    private long startTime;

    public PerformanceMeasurement() {
        startTime = System.currentTimeMillis();
    }

    public void printSpentTime() {
        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Spent processing time: " + String.format("%d min, %d sec, %d msec",
                TimeUnit.MILLISECONDS.toMinutes(estimatedTime),
                TimeUnit.MILLISECONDS.toSeconds(estimatedTime),
                TimeUnit.MILLISECONDS.toMillis(estimatedTime)));
    }
}
