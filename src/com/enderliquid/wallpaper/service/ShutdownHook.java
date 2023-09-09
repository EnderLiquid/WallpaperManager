package com.enderliquid.wallpaper.service;

import com.enderliquid.wallpaper.repository.Cache;

import static com.enderliquid.wallpaper.service.WallpaperManager.globalLogger;

public class ShutdownHook extends Thread {
    private static boolean shuttingDown = false;

    public static synchronized void process() {
        if (shuttingDown) {
            globalLogger.info("关闭挂钩已被调用，无需重新调用");
            return;
        }
        shuttingDown = true;
        try {
            Cache.record(WallpaperManager.getWallpaperInfo());
        } catch (NullPointerException e) {
            globalLogger.warning("调度器未被初始化");
        }
    }

    public void run() {
        process();
    }
}
