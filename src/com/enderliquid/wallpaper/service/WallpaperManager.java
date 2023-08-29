package com.enderliquid.wallpaper.service;

import com.enderliquid.wallpaper.controller.Controller;
import com.enderliquid.wallpaper.model.TimeOfWeek;
import com.enderliquid.wallpaper.model.WallpaperInfo;
import com.enderliquid.wallpaper.repository.Configuration;
import com.enderliquid.wallpaper.repository.ImageDeepScanner;
import com.enderliquid.wallpaper.repository.WallpaperInfoRecorder;
import com.enderliquid.wallpaper.util.Utility;
import javafx.application.Application;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static com.enderliquid.wallpaper.util.Utility.exceptionDetailsOf;
import static com.enderliquid.wallpaper.util.Utility.isImage;

public class WallpaperManager {
    public static final Logger globalLogger = Logger.getLogger("com.enderliquid.wallpaper");
    private static final Object GLOBAL_CONFIG_LOCKER = new Object();
    private static final Object WALLPAPER_LOCKER = new Object();
    private static final Object MODE_LOCKER = new Object();
    private static final Object SYNC_LOCKER = new Object();
    private static final Object SCHEDULE_LOCKER = new Object();
    private static Configuration globalConfig;
    private static Scheduler scheduler;
    private static ArrayList<File> wallpapers;
    private static File normalWallpaper;
    private static State state = State.IN_CLASS;
    private static Mode mode = Mode.AUTOMATIC;
    private static int index = 0;

    public static void main(String[] args) {
        loadLogger();
        if (!Utility.isWindows()) {
            globalLogger.severe("当前环境不是Windows系统，程序将停止运行");
            exit(-1);
        }
        globalLogger.info("开始进行初始化");
        WallpaperInfoRecorder.initialize();
        WallpaperChanger.initialize();
        load();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        globalLogger.info("初始化成功，开始启动图形操作界面");
        Application.launch(Controller.class, args);
    }

    public synchronized static void load() {
        loadGlobalConfig();
        SingleProcessVerifier.verify();
        loadWallpapers();
        loadScheduler();
        setState(state, true);
        setMode(mode, true);
        Runtime.getRuntime().gc();
    }

    public static void loadLogger() {
        Handler handler;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
            File logFile = new File("./logs/");
            logFile.mkdirs();
            logFile = new File(logFile, LocalDateTime.now().format(formatter) + ".txt");
            if (!logFile.createNewFile()) {
                globalLogger.severe("已存在同名日志文件");
                WallpaperManager.exit(-1);
            }
            handler = new FileHandler(logFile.toString());
            for (Handler h : globalLogger.getHandlers()) {
                h.close();
            }
            Objects.requireNonNull(handler).setFormatter(new SimpleFormatter());
            globalLogger.addHandler(handler);
            globalLogger.info("日志文件创建成功");
        } catch (IOException e) {
            globalLogger.warning("无法创建日志文件" + exceptionDetailsOf(e));
        }
    }

    public static void loadGlobalConfig() {
        synchronized (GLOBAL_CONFIG_LOCKER) {
            globalConfig = Configuration.getInstance();
            globalConfig.freeze();
            globalLogger.info("成功获取配置实例");
        }
    }

    public static Configuration getGlobalConfig() {
        synchronized (GLOBAL_CONFIG_LOCKER) {
            return globalConfig;
        }
    }

    public static void loadWallpapers() {
        synchronized (WALLPAPER_LOCKER) {
            globalLogger.info("开始加载壁纸");
            normalWallpaper = globalConfig.getNormalWallpaperPath().toFile();
            if (!isImage(normalWallpaper)) {
                globalLogger.severe("所提供的一般壁纸无效");
                exit(-1);
            }
            wallpapers = ImageDeepScanner.deepScan(globalConfig.getWallpaperPath().toFile());
            if (wallpapers == null || wallpapers.isEmpty()) {
                globalLogger.severe("所提供的壁纸路径无效或无壁纸");
                exit(-1);
            }
            Collections.sort(wallpapers);
            globalLogger.info("成功加载壁纸");
            WallpaperInfo info = WallpaperInfoRecorder.inquire();
            if (info != null) {
                index = Collections.binarySearch(wallpapers, new File(info.wallpaper));
                if (index < 0) {
                    index = -index - 1;
                    if (index >= wallpapers.size()) {
                        index = 0;
                    }
                    WallpaperInfoRecorder.record(new WallpaperInfo(wallpapers.get(index).toString(), getGlobalConfig().getWallpaperDisplayTime() * 1000L));
                }
            } else {
                index = 0;
                WallpaperInfoRecorder.record(new WallpaperInfo(wallpapers.get(0).toString(), getGlobalConfig().getWallpaperDisplayTime() * 1000L));
            }
            Controller.flushCurrentWallpaperText();
        }
    }

    public static void loadScheduler() {
        synchronized (SCHEDULE_LOCKER) {
            if (scheduler != null) {
                scheduler.wallpaperSwitchTimer.stop();
                scheduler.wallpaperInfoRecordTimer.stop();
                scheduler.syncWithScheduleTimer.stop();
                for (FixedRateTimer t : scheduler.stateSwitchTimers) {
                    t.stop();
                }
            }
            scheduler = new Scheduler();
            for (FixedRateTimer t : scheduler.stateSwitchTimers) {
                t.start();
            }
        }
    }

    public static void syncWithSchedule() {
        synchronized (SYNC_LOCKER) {
            globalLogger.info("正在与日程表同步");
            State latestUpdate = State.AFTER_CLASS;
            long latestUpdateDelay = -1;
            LocalDateTime now = LocalDateTime.now();
            for (TimeOfWeek[] clazz : getGlobalConfig().getScheduleInObjects()) {
                for (int i = 0; i <= 1; i++) {
                    long delay = clazz[i].getDelay(now);
                    if (delay > latestUpdateDelay) {
                        switch (i) {
                            case 0:
                                latestUpdate = State.IN_CLASS;
                                break;
                            case 1:
                                latestUpdate = State.AFTER_CLASS;
                                break;
                        }
                        latestUpdateDelay = delay;
                    }
                }
            }
            setState(latestUpdate, false);
        }
    }

    public static void next(boolean updateScheduler) {
        synchronized (WALLPAPER_LOCKER) {
            if (state == State.IN_CLASS || wallpapers.size() == 1) return;
            index++;
            if (index == wallpapers.size()) index = 0;
            WallpaperChanger.changeWallpaper(wallpapers.get(index));
            WallpaperInfoRecorder.record(getWallpaperInfo());
            Controller.flushCurrentWallpaperText();
            if (updateScheduler) {
                synchronized (SCHEDULE_LOCKER) {
                    scheduler.wallpaperSwitchTimer.reset();
                }
            }
        }
    }

    public static void previous(boolean updateScheduler) {
        synchronized (WALLPAPER_LOCKER) {
            if (state == State.IN_CLASS || wallpapers.size() == 1) return;
            index--;
            if (index == -1) index = wallpapers.size() - 1;
            WallpaperChanger.changeWallpaper(wallpapers.get(index));
            WallpaperInfoRecorder.record(getWallpaperInfo());
            Controller.flushCurrentWallpaperText();
            if (updateScheduler) {
                synchronized (SCHEDULE_LOCKER) {
                    scheduler.wallpaperSwitchTimer.reset();
                }
            }
        }
    }

    public static String getCurrentWallpaperName() {
        synchronized (WALLPAPER_LOCKER) {
            if (state == State.IN_CLASS) return normalWallpaper.getName();
            return wallpapers.get(index).getName();
        }
    }

    public static WallpaperInfo getWallpaperInfo() {
        synchronized (WALLPAPER_LOCKER) {
            return new WallpaperInfo(wallpapers.get(index).toString(), scheduler.wallpaperSwitchTimer.getRemainingTime());
        }
    }

    public static State getState() {
        synchronized (WALLPAPER_LOCKER) {
            return state;
        }
    }

    public static void setState(State s, boolean updateScheduler) {
        synchronized (WALLPAPER_LOCKER) {
            if (!updateScheduler && state == s) return;
            state = s;
            switch (state) {
                case IN_CLASS:
                    WallpaperInfoRecorder.record(getWallpaperInfo());
                    WallpaperChanger.changeWallpaper(normalWallpaper);
                    synchronized (SCHEDULE_LOCKER) {
                        scheduler.wallpaperSwitchTimer.stop();
                        scheduler.wallpaperInfoRecordTimer.stop();
                    }
                    break;
                case AFTER_CLASS:
                    WallpaperChanger.changeWallpaper(wallpapers.get(index));
                    synchronized (SCHEDULE_LOCKER) {
                        if (getMode() == Mode.AUTOMATIC) scheduler.wallpaperSwitchTimer.start();
                        scheduler.wallpaperInfoRecordTimer.start();
                    }
                    break;
            }
            Controller.flushCurrentWallpaperText();
            Controller.flushState();
        }
    }

    public static Mode getMode() {
        synchronized (MODE_LOCKER) {
            return mode;
        }
    }

    public static void setMode(Mode m, boolean updateScheduler) {
        synchronized (MODE_LOCKER) {
            if (!updateScheduler && mode == m) return;
            mode = m;
            switch (mode) {
                case AUTOMATIC:
                    synchronized (SCHEDULE_LOCKER) {
                        if (getState() == State.AFTER_CLASS) scheduler.wallpaperSwitchTimer.start();
                        syncWithSchedule();
                        scheduler.syncWithScheduleTimer.reset();
                        scheduler.syncWithScheduleTimer.start();
                    }
                    break;
                case MANUAL:
                    synchronized (SCHEDULE_LOCKER) {
                        scheduler.wallpaperSwitchTimer.stop();
                        scheduler.syncWithScheduleTimer.stop();
                    }
                    break;
            }
            Controller.flushMode();
        }
    }

    public static synchronized void exit(int status) {
        globalLogger.info("预料内的停止操作");
        ShutdownHook.processing();
        System.exit(status);
    }

    public enum State {
        IN_CLASS("上课", true),
        AFTER_CLASS("课间", false);
        private final String name;
        private final boolean selected;

        State(String name, boolean selected) {
            this.name = name;
            this.selected = selected;
        }

        public String getName() {
            return name;
        }

        public boolean isSelected() {
            return selected;
        }
    }

    public enum Mode {
        AUTOMATIC("自动", true),
        MANUAL("手动", false);
        private final String name;
        private final boolean selected;

        Mode(String name, boolean selected) {
            this.name = name;
            this.selected = selected;
        }

        public String getName() {
            return name;
        }

        public boolean isSelected() {
            return selected;
        }
    }
}
