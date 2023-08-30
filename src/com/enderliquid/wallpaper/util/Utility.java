package com.enderliquid.wallpaper.util;

import com.enderliquid.wallpaper.service.WallpaperManager;

import java.io.File;

import static java.lang.System.lineSeparator;

public class Utility {
    public static boolean isImage(File f) {
        if (f == null || !f.isFile()) return false;
        String name = f.getName();
        for (String e : WallpaperManager.getGlobalConfig().getFileExtensions()) {
            if (name.endsWith("." + e)) return true;
        }
        return false;
    }

    public static String exceptionDetailsOf(Exception e) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(lineSeparator())
                .append(e.getClass())
                .append(':')
                .append(lineSeparator())
                .append(e.getMessage())
                .append(lineSeparator());
        for (StackTraceElement t : e.getStackTrace()) {
            stringBuilder.append(t).append(lineSeparator());
        }
        return stringBuilder.toString();
    }

    public static boolean isWindows() {
        return System.getProperties().getProperty("os.name").toUpperCase().contains("WINDOWS");
    }
}
