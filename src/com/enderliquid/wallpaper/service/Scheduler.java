package com.enderliquid.wallpaper.service;

import com.enderliquid.wallpaper.model.TimeOfWeek;
import com.enderliquid.wallpaper.repository.Cache;
import com.enderliquid.wallpaper.repository.Configuration;
import com.enderliquid.wallpaper.service.WallpaperManager.State;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.enderliquid.wallpaper.service.WallpaperManager.globalLogger;

public class Scheduler {
    public final FixedRateTimer wallpaperSwitchTimer;
    public final FixedRateTimer wallpaperInfoRecordTimer;
    public final List<FixedRateTimer> stateSwitchTimers = new LinkedList<>();

    Scheduler() {
        globalLogger.info("开始构造调度器");
        long remainingTime;
        Configuration config = WallpaperManager.getGlobalConfig();
        try {
            remainingTime = Objects.requireNonNull(Cache.inquire()).remainingTime;
        } catch (NullPointerException e) {
            remainingTime = config.getWallpaperDisplayTime() * 1000L;
        }
        wallpaperSwitchTimer = new FixedRateTimer(
                () -> WallpaperManager.next(false),
                remainingTime,
                config.getWallpaperDisplayTime() * 1000L
        );
        wallpaperInfoRecordTimer = new FixedRateTimer(
                () -> WallpaperManager.saveCache(),
                config.getWallpaperInfoRecordInterval() * 1000L,
                config.getWallpaperInfoRecordInterval() * 1000L
        );
        for (TimeOfWeek[] course : config.getResolvedSchedule()) {
            FixedRateTimer timer;
            for (State state : State.values()) {
                timer = new FixedRateTimer(
                        () -> {
                            if (WallpaperManager.getMode() == WallpaperManager.Mode.AUTOMATIC)
                                WallpaperManager.setState(state, false);
                        },
                        course[state.ordinal()].getDelay(LocalDateTime.now()),
                        7 * 24 * 60 * 60 * 1000L
                );
                timer.start();
                stateSwitchTimers.add(timer);
            }
        }
        globalLogger.info("成功构造调度器");
    }

    public void stop() {
        wallpaperSwitchTimer.stop();
        wallpaperInfoRecordTimer.stop();
        for (FixedRateTimer t : stateSwitchTimers) {
            t.stop();
        }
    }
}
