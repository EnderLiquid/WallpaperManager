package com.enderliquid.wallpaper.model;

import java.time.LocalDateTime;

public class TimeOfWeek {
    public final int dayOfWeek;
    public final int hour;
    public final int minute;

    public TimeOfWeek(int time) {
        this.dayOfWeek = time / 10000;
        time = time % 10000;
        this.hour = time / 100;
        time = time % 100;
        this.minute = time;
        if (dayOfWeek < 1 || dayOfWeek > 7 || hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new RuntimeException("Illegal Date Format");
        }
    }

    public long getDelay(LocalDateTime now) {
        int delay = 0;
        int currentDayOfWeek = now.getDayOfWeek().getValue();
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();
        int currentSecond = now.getSecond();
        delay += (dayOfWeek - currentDayOfWeek) * 24 * 60 * 60;
        delay += (hour - currentHour) * 60 * 60;
        delay += (minute - currentMinute) * 60;
        delay -= currentSecond;
        if (delay < 0) delay += 7 * 24 * 60 * 60;
        return delay * 1000L;
    }
}
