package com.enderliquid.wallpaper.service;

import com.enderliquid.wallpaper.controller.Controller;
import com.enderliquid.wallpaper.repository.Configuration;
import com.enderliquid.wallpaper.util.Utility;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static com.enderliquid.wallpaper.service.WallpaperManager.getGlobalConfig;
import static com.enderliquid.wallpaper.service.WallpaperManager.globalLogger;

public class SingleProcessVerifier {
    private static final String HOST = "localhost";
    private static ServerSocket server;
    private static Thread handleThread;
    private static int port = -1;

    public static synchronized void verify() {
        Configuration config = getGlobalConfig();
        if (config.getPort() != port) {
            port = config.getPort();
            if (handleThread != null) {
                globalLogger.info("正在关闭原有的socket");
                try {
                    server.close();
                    handleThread.interrupt();
                } catch (IOException e) {
                    globalLogger.warning("关闭socket时出现异常" + Utility.exceptionDetailsOf(e));
                }
            }
        }
        globalLogger.info("开始监听端口：" + port);
        try {
            server = new ServerSocket(port);
            globalLogger.info("成功监听端口：" + port);
            handleThread = new Thread(() -> {
                while (!Thread.interrupted()) {
                    try {
                        server.accept();
                        globalLogger.info("成功响应连接请求");
                        Controller.setOnTop();
                    } catch (IOException e) {
                        globalLogger.warning("等待连接时出现异常，可能socket已被关闭" + Utility.exceptionDetailsOf(e));
                    }
                }
            });
            handleThread.start();
        } catch (IOException e) {
            globalLogger.severe("端口已被占用：" + port + Utility.exceptionDetailsOf(e));
            globalLogger.info("正在连接到原有进程的socket");
            try (Socket client = new Socket(HOST, port)) {
                globalLogger.warning("连接成功");
            } catch (IOException ex) {
                globalLogger.warning("连接时出现异常" + Utility.exceptionDetailsOf(ex));
            }
            WallpaperManager.exit(-1);
        }
    }
}