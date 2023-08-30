package com.enderliquid.wallpaper.service;

import com.enderliquid.wallpaper.model.TimeOfWeek;
import com.enderliquid.wallpaper.repository.Configuration;
import com.enderliquid.wallpaper.repository.WallpaperInfoHandler;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.enderliquid.wallpaper.service.WallpaperManager.globalLogger;

public class Scheduler {
    public final FixedRateTimer wallpaperSwitchTimer;
    public final FixedRateTimer wallpaperInfoRecordTimer;
    public final List<FixedRateTimer> stateSwitchTimers = new LinkedList<>();

    public Scheduler() {
        globalLogger.info("开始构造调度器");
        long remainingTime;
        Configuration config = WallpaperManager.getGlobalConfig();
        try {
            remainingTime = Objects.requireNonNull(WallpaperInfoHandler.inquire()).remainingTime;
        } catch (NullPointerException e) {
            remainingTime = config.getWallpaperDisplayTime() * 1000L;
        }
        wallpaperSwitchTimer = new FixedRateTimer(
                () -> WallpaperManager.next(false),
                remainingTime,
                config.getWallpaperDisplayTime() * 1000L
        );
        wallpaperInfoRecordTimer = new FixedRateTimer(
                () -> WallpaperInfoHandler.record(WallpaperManager.getWallpaperInfo()),
                config.getWallpaperInfoRecordInterval() * 1000L,
                config.getWallpaperInfoRecordInterval() * 1000L
        );
        for (TimeOfWeek[] clazz : config.getScheduleInObjects()) {
            stateSwitchTimers.add(new FixedRateTimer(
                    () -> {
                        if (WallpaperManager.getMode() == WallpaperManager.Mode.AUTOMATIC)
                            WallpaperManager.setState(WallpaperManager.State.IN_CLASS, false);
                    },
                    clazz[0].getDelay(LocalDateTime.now()),
                    7 * 24 * 60 * 60 * 1000L
            ));
            stateSwitchTimers.add(new FixedRateTimer(
                    () -> {
                        if (WallpaperManager.getMode() == WallpaperManager.Mode.AUTOMATIC)
                            WallpaperManager.setState(WallpaperManager.State.AFTER_CLASS, false);
                    },
                    clazz[1].getDelay(LocalDateTime.now()),
                    7 * 24 * 60 * 60 * 1000L
            ));
        }
        globalLogger.info("成功构造调度器");
    }
}
