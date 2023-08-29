package com.enderliquid.wallpaper.controller;

import com.enderliquid.wallpaper.service.WallpaperManager;
import com.melloware.jintellitype.JIntellitype;
import com.melloware.jintellitype.JIntellitypeException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Objects;

import static com.enderliquid.wallpaper.service.WallpaperManager.*;
import static com.enderliquid.wallpaper.util.Utility.exceptionDetailsOf;

public class Controller extends Application {
    public static final String appName = "壁纸管理器";
    private static final int HIDE_OR_SHOW_MARK = 0;
    private static final int URGENT_PROCESS_MARK = 1;
    public static Controller instance;
    private static Stage primaryStage;
    @FXML
    private Pane pane;
    @FXML
    private Text currentWallpaperText;
    @FXML
    private Button previousButton;
    @FXML
    private ToggleButton switchModeButton;
    @FXML
    private Text currentModeText;
    @FXML
    private Text currentStateText;
    @FXML
    private Button exitButton;
    @FXML
    private ToggleGroup mode;
    @FXML
    private Button nextButton;
    @FXML
    private Button reloadButton;
    @FXML
    private Button openConfigButton;
    @FXML
    private Button openWallpaperDirButton;
    @FXML
    private ToggleGroup state;
    @FXML
    private ToggleButton switchStateButton;

    public static void flush() {
        flushState();
        flushMode();
        flushCurrentWallpaperText();
    }

    public static synchronized void flushCurrentWallpaperText() {
        if (instance == null) return;
        instance.currentWallpaperText.setText(WallpaperManager.getCurrentWallpaperName());
    }

    public static synchronized void flushState() {
        if (instance == null) return;
        instance.currentStateText.setText(WallpaperManager.getState().getName());
        instance.switchStateButton.setSelected(WallpaperManager.getState().isSelected());
    }

    public static void flushMode() {
        if (instance == null) return;
        instance.currentModeText.setText(WallpaperManager.getMode().getName());
        instance.switchModeButton.setSelected(WallpaperManager.getMode().isSelected());
    }

    private static synchronized void hideOrShow() {
        if (primaryStage == null) return;
        if (primaryStage.isShowing()) {
            Platform.runLater(() -> {
                primaryStage.setIconified(false);
                primaryStage.hide();
            });
            globalLogger.info("窗口已隐藏");
        } else {
            Platform.runLater(primaryStage::show);
            globalLogger.info("窗口已显示");
        }
    }

    private static synchronized void urgentProcess() {
        setMode(Mode.MANUAL, false);
        setState(State.IN_CLASS, false);
        globalLogger.info("已进行紧急处理");
    }

    public static synchronized void setOnTop() {
        if (primaryStage == null) return;
        Platform.runLater(() -> {
            primaryStage.show();
            primaryStage.setIconified(false);
        });
        globalLogger.info("窗口已置顶");
    }

    @FXML
    void switchModeEvent(ActionEvent event) {
        for (WallpaperManager.Mode m : WallpaperManager.Mode.values()) {
            if (m.isSelected() == switchModeButton.isSelected()) {
                WallpaperManager.setMode(m, false);
            }
        }
    }

    @FXML
    void switchStateEvent(ActionEvent event) {
        WallpaperManager.setMode(WallpaperManager.Mode.MANUAL, false);
        for (WallpaperManager.State s : WallpaperManager.State.values()) {
            if (s.isSelected() == switchStateButton.isSelected()) {
                WallpaperManager.setState(s, false);
            }
        }
    }

    @FXML
    void previousEvent(ActionEvent event) {
        WallpaperManager.previous(true);
    }

    @FXML
    void nextEvent(ActionEvent event) {
        WallpaperManager.next(true);
    }

    @FXML
    public void exitEvent(ActionEvent actionEvent) {
        WallpaperManager.exit(0);
    }

    @FXML
    void openConfigEvent(ActionEvent event) {
        try {
            Runtime.getRuntime().exec(String.format("notepad \"%s\"", Paths.get("./config/config.json").toAbsolutePath().normalize()));
        } catch (IOException e) {
            globalLogger.warning("执行命令行失败" + exceptionDetailsOf(e));
        }
    }

    @FXML
    void openWallpaperDirEvent(ActionEvent event) {
        try {
            Runtime.getRuntime().exec(String.format("explorer \"%s\"", getGlobalConfig().getWallpaperPath().toAbsolutePath()));
        } catch (IOException e) {
            globalLogger.warning("执行命令行失败" + exceptionDetailsOf(e));
        }
    }

    @FXML
    void reloadEvent(ActionEvent event) {
        WallpaperManager.load();
    }

    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        Parent root = null;
        String MainWindowFxmlPath = "fxml/MainWindow.fxml";
        globalLogger.info("开始加载fxml文件");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(MainWindowFxmlPath));
            root = loader.load();
            instance = loader.getController();
        } catch (RuntimeException e) {
            globalLogger.severe("fxml文件无法被加载" + exceptionDetailsOf(e));
            WallpaperManager.exit(-1);
        }
        globalLogger.info("加载fxml文件成功");
        globalLogger.info("正在初始化窗口与场景");
        Scene scene = new Scene(root);
        primaryStage.setTitle(appName);
        primaryStage.setScene(scene);
        primaryStage.setOnShowing(event -> flush());
        Platform.setImplicitExit(false);
        primaryStage.setResizable(false);
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        primaryStage.setX(dimension.width - instance.pane.getPrefWidth());
        primaryStage.setY(dimension.height - instance.pane.getPrefHeight() - 70);
        globalLogger.info("正在注册并监听快捷键");
        try {
            JIntellitype.getInstance().registerHotKey(HIDE_OR_SHOW_MARK, JIntellitype.MOD_CONTROL + JIntellitype.MOD_ALT, 'D');
            JIntellitype.getInstance().registerHotKey(URGENT_PROCESS_MARK, JIntellitype.MOD_CONTROL + JIntellitype.MOD_ALT, 'E');
            JIntellitype.getInstance().addHotKeyListener((markCode) -> {
                if (!getGlobalConfig().isEnableShortcuts()) return;
                switch (markCode) {
                    case HIDE_OR_SHOW_MARK:
                        hideOrShow();
                        break;
                    case URGENT_PROCESS_MARK:
                        urgentProcess();
                        break;
                }
            });
        } catch (JIntellitypeException e) {
            globalLogger.warning("快捷键无法被注册并监听");
        }
        primaryStage.show();
        globalLogger.info("正在创建系统托盘");
        if (!SystemTray.isSupported()) {
            globalLogger.warning("当前环境不支持系统托盘");
            return;
        }
        SystemTray tray = SystemTray.getSystemTray();
        BufferedImage bufferedImageIcon;
        Image imageIcon;
        InputStream imageInputStream;
        String iconPath = "icon/icon.png";
        try {
            imageInputStream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(iconPath));
        } catch (NullPointerException e) {
            globalLogger.warning("图标文件输入流无法被创建" + exceptionDetailsOf(e));
            return;
        }
        imageIcon = new Image(imageInputStream);
        primaryStage.getIcons().add(imageIcon);
        bufferedImageIcon = SwingFXUtils.fromFXImage(imageIcon, null);
        TrayIcon trayIcon;
        trayIcon = new TrayIcon(bufferedImageIcon, appName);
        trayIcon.setImageAutoSize(true);
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            globalLogger.warning("系统托盘图标无法被加载" + exceptionDetailsOf(e));
            return;
        }
        trayIcon.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 1) {
                    hideOrShow();
                }
                if (e.getButton() == 3 && e.getClickCount() == 2) {
                    WallpaperManager.exit(0);
                }
            }
        });
        globalLogger.info("创建系统托盘成功");
        primaryStage.hide();
    }
}
