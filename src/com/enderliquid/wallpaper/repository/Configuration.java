package com.enderliquid.wallpaper.repository;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.enderliquid.wallpaper.model.TimeOfWeek;
import com.enderliquid.wallpaper.service.WallpaperManager;
import com.enderliquid.wallpaper.util.Utility;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Scanner;

import static com.enderliquid.wallpaper.service.WallpaperManager.globalLogger;

public class Configuration {
    public static final int defaultMaxScanningFileCount = 1000000;//WallpaperChanger,wallpapers相关
    public static final String[] defaultFileExtensions = new String[]{"jpg", "png", "bmp"};//WallpaperChanger,wallpapers相关
    public static final int defaultWallpaperDisplayTime = 120;//Scheduler相关
    public static final Path defaultWallpaperPath = Paths.get("./wallpaper/");//wallpapers相关
    public static final Path defaultNormalWallpaperPath = Paths.get("./normal.png");//normalWallpaper相关
    public static final int defaultApplyWallpaperModificationDelayInMillis = 300;//WallpaperChanger相关
    public static final int defaultWallpaperInfoRecordInterval = 20;//Scheduler相关
    public static final int defaultSyncWithScheduleInterval = 30;//Scheduler相关
    public static final int defaultPort = 25564;
    public static final boolean defaultEnableShortcuts = true;
    public static final int[][] defaultSchedule = new int[][]{
            {10710, 10750},
            {10800, 10840},
    };
    private int maxScanningFileCount = defaultMaxScanningFileCount;
    private String[] fileExtensions = defaultFileExtensions;
    private int wallpaperDisplayTime = defaultWallpaperDisplayTime;
    private Path wallpaperPath = defaultWallpaperPath;
    private Path normalWallpaperPath = defaultNormalWallpaperPath;
    private int applyWallpaperModificationDelayInMillis = defaultApplyWallpaperModificationDelayInMillis;
    private int wallpaperInfoRecordInterval = defaultWallpaperInfoRecordInterval;
    private int syncWithScheduleInterval = defaultSyncWithScheduleInterval;
    private boolean enableShortcuts = defaultEnableShortcuts;
    private int port = defaultPort;
    private int[][] schedule = defaultSchedule;
    @JSONField(serialize = false, deserialize = false)
    private TimeOfWeek[][] scheduleInObjects;
    @JSONField(serialize = false, deserialize = false)
    private volatile boolean editable = true;

    @JSONField(serialize = false, deserialize = false)
    private static Configuration getDefaultInstance() {
        Configuration result = new Configuration();
        try {
            result.scheduleInObjects = scheduleToScheduleInObjects(defaultSchedule);
        } catch (RuntimeException e) {
            globalLogger.severe("默认日程表存在问题");
            WallpaperManager.exit(-1);
        }
        return result;
    }

    private static TimeOfWeek[][] scheduleToScheduleInObjects(int[][] schedule) {
        TimeOfWeek[][] scheduleInObjects = new TimeOfWeek[schedule.length][2];
        try {
            if (schedule.length == 0) {
                globalLogger.warning("日程表为空");
                throw new NullPointerException();
            }
            globalLogger.info("正在解析日程表");
            for (int i = 0; i < schedule.length; i++) {
                for (int j = 0; j <= 1; j++) {
                    scheduleInObjects[i][j] = new TimeOfWeek(schedule[i][j]);
                }
            }
            return scheduleInObjects;
        } catch (RuntimeException e) {
            globalLogger.warning("无法解析日程表" + Utility.exceptionDetailsOf(e));
            throw e;
        }
    }

    @JSONField(serialize = false, deserialize = false)
    public static Configuration getInstance() {
        globalLogger.info("开始读取配置");
        try {
            File configFile = new File("./config/");
            configFile.mkdirs();
            configFile = new File(configFile, "config.json");
            if (configFile.createNewFile()) {
                globalLogger.info("未检测到配置文件，将使用默认配置，并生成默认配置文件");
                try (FileWriter defaultConfigWriter = new FileWriter(configFile)) {
                    Configuration defaultInstance = getDefaultInstance();
                    defaultConfigWriter.write(JSON.toJSONString(defaultInstance, SerializerFeature.PrettyFormat));
                    return defaultInstance;
                } catch (IOException e) {
                    globalLogger.warning("无法生成默认配置文件" + Utility.exceptionDetailsOf(e));
                    return getDefaultInstance();
                }
            }
            FileReader configReader = new FileReader(configFile);
            Scanner configScanner = new Scanner(configReader);
            StringBuilder contentBuilder = new StringBuilder();
            while (configScanner.hasNextLine()) {
                contentBuilder.append(configScanner.nextLine()).append(System.lineSeparator());
            }
            configScanner.close();
            String jsonContent = contentBuilder.toString();
            globalLogger.info("成功读取配置文件内容");
            Configuration instance = Objects.requireNonNull(JSON.parseObject(jsonContent, Configuration.class));
            try {
                instance.scheduleInObjects = scheduleToScheduleInObjects(instance.schedule);
            } catch (RuntimeException e) {
                globalLogger.warning("将使用默认日程配置");
                return getDefaultInstance();
            }
            return instance;
        } catch (IOException | NullPointerException | JSONException e) {
            globalLogger.warning("无法读取配置文件或配置文件内容有误，将使用默认配置" + Utility.exceptionDetailsOf(e));
            return getDefaultInstance();
        }
    }

    public int getMaxScanningFileCount() {
        return maxScanningFileCount;
    }

    public void setMaxScanningFileCount(int maxScanningFileCount) {
        if (!editable) throw new UnsupportedOperationException();
        this.maxScanningFileCount = maxScanningFileCount;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(String[] fileExtensions) {
        if (!editable) throw new UnsupportedOperationException();
        this.fileExtensions = fileExtensions;
    }

    public int getWallpaperDisplayTime() {
        return wallpaperDisplayTime;
    }

    public void setWallpaperDisplayTime(int wallpaperDisplayTime) {
        if (!editable) throw new UnsupportedOperationException();
        this.wallpaperDisplayTime = wallpaperDisplayTime;
    }

    public Path getWallpaperPath() {
        return wallpaperPath;
    }

    public void setWallpaperPath(Path wallpaperPath) {
        if (!editable) throw new UnsupportedOperationException();
        this.wallpaperPath = wallpaperPath;
    }

    public int getApplyWallpaperModificationDelayInMillis() {
        return applyWallpaperModificationDelayInMillis;
    }

    public void setApplyWallpaperModificationDelayInMillis(int applyWallpaperModificationDelayInMillis) {
        if (!editable) throw new UnsupportedOperationException();
        this.applyWallpaperModificationDelayInMillis = applyWallpaperModificationDelayInMillis;
    }

    public Path getNormalWallpaperPath() {
        return normalWallpaperPath;
    }

    public void setNormalWallpaperPath(Path normalWallpaperPath) {
        if (!editable) throw new UnsupportedOperationException();
        this.normalWallpaperPath = normalWallpaperPath;
    }

    public int getSyncWithScheduleInterval() {
        return syncWithScheduleInterval;
    }

    public void setSyncWithScheduleInterval(int syncWithScheduleInterval) {
        if (!editable) throw new UnsupportedOperationException();
        this.syncWithScheduleInterval = syncWithScheduleInterval;
    }

    public int getWallpaperInfoRecordInterval() {
        return wallpaperInfoRecordInterval;
    }

    public void setWallpaperInfoRecordInterval(int wallpaperInfoRecordInterval) {
        if (!editable) throw new UnsupportedOperationException();
        this.wallpaperInfoRecordInterval = wallpaperInfoRecordInterval;
    }

    public int[][] getSchedule() {
        return schedule;
    }

    public void setSchedule(int[][] schedule) {
        if (!editable) throw new UnsupportedOperationException();
        this.schedule = schedule;
    }

    public boolean isEnableShortcuts() {
        return enableShortcuts;
    }

    public void setEnableShortcuts(boolean enableShortcuts) {
        if (!editable) throw new UnsupportedOperationException();
        this.enableShortcuts = enableShortcuts;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        if (!editable) throw new UnsupportedOperationException();
        this.port = port;
    }

    public TimeOfWeek[][] getScheduleInObjects() {
        return scheduleInObjects;
    }

    public void freeze() {
        editable = false;
    }
}
