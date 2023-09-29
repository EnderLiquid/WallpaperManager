package com.enderliquid.wallpaper.repository;

import com.enderliquid.wallpaper.model.WallpaperInfo;
import com.enderliquid.wallpaper.util.Utility;

import java.io.*;

import static com.enderliquid.wallpaper.service.WallpaperManager.globalLogger;

public class Cache {

    private static File tempFile;
    private static WallpaperInfo latestInfo;

    public static void initialize() {
        globalLogger.info("开始初始化缓存器");
        try {
            tempFile = new File("./cache/");
            tempFile.mkdirs();
            tempFile = new File(tempFile, "record.bin");
            if (tempFile.createNewFile()) globalLogger.info("已生成缓存文件");
            globalLogger.info("缓存器初始化完成");

        } catch (IOException e) {
            globalLogger.warning("无法生成缓存文件" + Utility.exceptionDetailsOf(e));
        }
    }

    public static synchronized void save(WallpaperInfo info) {
        globalLogger.info("正在记录壁纸信息");
        latestInfo = info;
        if (tempFile == null) {
            globalLogger.warning("缓存器未进行初始化，无法进行信息记录");
            return;
        }
        try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(tempFile))) {
            outputStream.writeUTF(info.wallpaper);
            outputStream.writeLong(info.remainingTime);
        } catch (IOException e) {
            globalLogger.warning("无法正常写入缓存文件" + Utility.exceptionDetailsOf(e));
        }
    }

    public static synchronized WallpaperInfo inquire() {
        globalLogger.info("正在查询壁纸信息");
        if (latestInfo != null) return latestInfo;
        if (tempFile == null) {
            globalLogger.warning("缓存器未进行初始化，无法进行信息查询");
            return null;
        }
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(tempFile))) {
            latestInfo = new WallpaperInfo(inputStream.readUTF(), inputStream.readLong());
            return latestInfo;
        } catch (IOException e) {
            globalLogger.warning("无法读取缓存文件，缓存文件可能从未被写入");
            return null;
        }
    }
}
