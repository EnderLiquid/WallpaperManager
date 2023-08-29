package com.enderliquid.wallpaper.model;

public class WallpaperInfo {
    public final String wallpaper;
    public final long remainingTime;

    public WallpaperInfo(String wallpaper, long remainingTime) {
        this.wallpaper = wallpaper;
        this.remainingTime = remainingTime;
    }
}
