package com.enderliquid.wallpaper.service;

import com.enderliquid.wallpaper.repository.WallpaperInfoRecorder;

import static com.enderliquid.wallpaper.service.WallpaperManager.globalLogger;

public class ShutdownHook extends Thread {
    private static boolean shuttingDown = false;

    public static synchronized void processing() {
        if (shuttingDown) {
            globalLogger.info("关闭挂钩已被调用，无需重新调用");
            return;
        }
        shuttingDown = true;
        try {
            WallpaperInfoRecorder.record(WallpaperManager.getWallpaperInfo());
        } catch (NullPointerException e) {
            globalLogger.warning("调度器未被初始化");
        }
    }

    public void run() {
        processing();
    }
}