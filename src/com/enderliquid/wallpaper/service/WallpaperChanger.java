package com.enderliquid.wallpaper.service;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

import java.io.File;
import java.io.IOException;

import static com.enderliquid.wallpaper.service.WallpaperManager.getGlobalConfig;
import static com.enderliquid.wallpaper.service.WallpaperManager.globalLogger;
import static com.enderliquid.wallpaper.util.Utility.exceptionDetailsOf;
import static com.enderliquid.wallpaper.util.Utility.isImage;

public class WallpaperChanger {
    private static final int SPI_SETDESKWALLPAPER = 0x0014;
    private static final int SPIF_UPDATEINIFILE = 0x0001;
    private static final int SPIF_SENDWININICHANGE = 0x0002;
    private static final Object LOCKER = new Object();
    private static boolean hasFailed = false;
    private static File imageToDisplay = null;

    public static void initialize() {
        globalLogger.info("正在初始化壁纸更换器");
        new Handler().start();
    }

    public static void changeWallpaper(File img) {
        synchronized (LOCKER) {
            imageToDisplay = img;
            LOCKER.notify();
        }
    }

    private interface MyUser32 extends StdCallLibrary {
        MyUser32 INSTANCE = Native.load("user32", MyUser32.class);

        boolean SystemParametersInfoA(int uiAction, int uiParam, String fnm, int fWinIni);
    }

    private static class Handler extends Thread {
        public void run() {
            File latestImage = null;
            while (!Thread.interrupted()) {
                synchronized (LOCKER) {
                    while (latestImage == imageToDisplay) {
                        try {
                            LOCKER.wait();
                        } catch (InterruptedException e) {
                            globalLogger.warning("线程被意外中断");
                        }
                    }
                    latestImage = imageToDisplay;
                }
                boolean succeeded = false;
                try {
                    if (!isImage(latestImage)) {
                        globalLogger.warning("找不到指定的壁纸");
                        continue;
                    }
                    globalLogger.info("尝试切换到壁纸：" + latestImage);
                    try {
                        Runtime.getRuntime().exec(String.format("reg add \"hkcu\\control panel\\desktop\" /v wallpaper /d \"%s\" /f", latestImage.getAbsoluteFile()));
                    } catch (IOException e) {
                        globalLogger.warning("执行命令行失败" + exceptionDetailsOf(e));
                        continue;
                    }
                    try {
                        Thread.sleep(getGlobalConfig().getApplyWallpaperModificationDelayInMillis());
                    } catch (InterruptedException e) {
                        globalLogger.warning("线程被意外中断");
                        continue;
                    }
                    if (!MyUser32.INSTANCE.SystemParametersInfoA(SPI_SETDESKWALLPAPER, 0, null, SPIF_SENDWININICHANGE | SPIF_UPDATEINIFILE)) {
                        globalLogger.warning("更换到指定的壁纸失败");
                        continue;
                    }
                    globalLogger.info("切换壁纸成功");
                    succeeded = true;
                } finally {
                    if (succeeded) hasFailed = false;
                    else {
                        if (hasFailed) {
                            globalLogger.severe("壁纸切换连续失败，程序将停止运行");
                            WallpaperManager.exit(-1);
                        }
                        WallpaperManager.load();
                    }
                }
            }
        }
    }
}
