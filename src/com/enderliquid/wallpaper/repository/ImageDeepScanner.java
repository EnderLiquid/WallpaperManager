package com.enderliquid.wallpaper.repository;

import com.enderliquid.wallpaper.service.WallpaperManager;

import java.io.File;
import java.util.ArrayList;

import static com.enderliquid.wallpaper.util.Utility.isImage;

public class ImageDeepScanner {
    private static ArrayList<File> images;
    private static int scanned = 0;

    public static synchronized ArrayList<File> deepScan(File topDir) {
        scanned = 0;
        images = new ArrayList<>();
        scan(topDir);
        return images;
    }

    private static void scan(File dir) {
        if (scanned > WallpaperManager.getGlobalConfig().getMaxScanningFileCount()) return;
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        scanned += files.length;
        for (File f : files) {
            if (isImage(f)) images.add(f);
            else if (f.isDirectory()) scan(f);
        }
    }
}
