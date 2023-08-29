package com.enderliquid.wallpaper.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FixedRateTimer {
    private final Runnable task;
    private final long period;
    private ScheduledExecutorService executor;
    private boolean isRunning;
    private long remainingTime;
    private long startTime;

    public FixedRateTimer(Runnable task, long delay, long period) {
        this.task = task;
        this.remainingTime = delay;
        this.period = period;
        this.isRunning = false;
    }

    /*public synchronized void start() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
        startTime = System.currentTimeMillis();
        remainingTime = period;
        isRunning = true;
    }*/

    public synchronized void start() {
        if (isRunning) return;
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(task, remainingTime, period, TimeUnit.MILLISECONDS);
        startTime = System.currentTimeMillis() - remainingTime;
        isRunning = true;
    }

    public synchronized void stop() {
        if (!isRunning) return;
        executor.shutdown();
        getRemainingTime();
        isRunning = false;
    }

    public synchronized void reset() {
        if (isRunning) stop();
        remainingTime = period;
        if (isRunning) start();
    }

    public synchronized long getRemainingTime() {
        if (isRunning) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            remainingTime = period - (elapsedTime % period);
        }
        return remainingTime;
    }
}
