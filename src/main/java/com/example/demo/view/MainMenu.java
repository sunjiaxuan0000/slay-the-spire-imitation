package com.example.demo.view;

import javafx.animation.ScaleTransition;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面：背景 start_menu.png，上面用 START.png 作为“开始游戏”按钮图。
 * 按钮保持原比例、以较大尺寸显示在“你找出的开始游戏位置”的中心，
 * 鼠标悬停会放大，点击开始游戏。
 */
public class MainMenu extends Pane {

    // ===== “开始游戏”按钮位置（背景图像素）：你用方向键对齐后的中心 = (320, 497) =====
    private static final double START_CX = 320; // 按钮中心 X
    private static final double START_CY = 497; // 按钮中心 Y
    // ==================================================================================

    /**
     * 按钮显示宽度（背景图像素）。它会乘上“背景缩放系数”，
     * 所以窗口怎么拉，按钮都和背景图保持同比例（位置、大小一起动）。
     * 想再放大就把这个数调大。
     */
    private static final double BUTTON_W_IMG = 520;

    /** 一个可点击区域：图片上的矩形 (x, y, w, h)，命中后执行 action。 */
    private static class Hotspot {
        double x, y, w, h;
        Runnable action;

        Hotspot(double x, double y, double w, double h, Runnable action) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.action = action;
        }

        boolean contains(double px, double py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    private final Image bgImage;
    private final ImageView startButton;
    private final List<Hotspot> hotspots = new ArrayList<>();
    private final ScaleTransition hoverAnim; // 悬停放大动画
    private boolean overStart = false;       // 当前鼠标是否在开始按钮上

    // ===== 对齐调试工具 =====
    private double offX = 0;                 // 按钮中心的额外偏移（背景图像素）
    private double offY = 0;
    private final javafx.scene.shape.Rectangle debugRect = new javafx.scene.shape.Rectangle(); // 青色框显示按钮当前范围

    public MainMenu(Runnable onStartGame, Runnable onExit) {
        bgImage = new Image(MainMenu.class.getResourceAsStream("/com/example/demo/start_menu.png"));
        setBackground(buildBackground(bgImage));

        // START.png：开始游戏按钮图（只负责显示，点击由下面的逻辑统一处理）
        startButton = new ImageView(new Image(MainMenu.class.getResourceAsStream("/com/example/demo/START.png")));
        startButton.setPreserveRatio(true);    // 保持原比例，绝不压扁
        startButton.setMouseTransparent(true); // 鼠标事件穿透，交给本面板统一处理
        getChildren().add(startButton);

        // 调试用的青色外框（默认隐藏，F3 打开），画在最上层方便看对齐
        debugRect.setFill(null);
        debugRect.setStroke(javafx.scene.paint.Color.rgb(34, 211, 238, 0.9));
        debugRect.setStrokeWidth(2);
        debugRect.setVisible(false);
        getChildren().add(debugRect);

        // 悬停放大动画（120 毫秒，放大 15%）
        hoverAnim = new ScaleTransition(Duration.millis(120), startButton);

        // 窗口尺寸一变，就重新计算按钮图的位置和大小
        widthProperty().addListener(o -> layoutStartButton());
        heightProperty().addListener(o -> layoutStartButton());

        // ============ 其它按钮热区（图片像素坐标）============
        addHotspot(0, 0, 0, 0, onExit);   // TODO: “退出”按钮位置找到后照填
        // ===================================================

        // 点击：先判断是否点在开始按钮上，再查其它热区
        setOnMouseClicked(e -> {
            Rectangle2D r = buttonRect();
            if (r != null && r.contains(e.getX(), e.getY())) {
                onStartGame.run();
                return;
            }
            // 打印坐标 + 检查其它图片热区（用来校准“退出”按钮位置）
            Point2D p = toImageCoords(e.getX(), e.getY());
            System.out.println("点击了图片坐标: (" + (int) p.getX() + ", " + (int) p.getY() + ")");
            for (Hotspot hs : hotspots) {
                if (hs.contains(p.getX(), p.getY())) {
                    hs.action.run();
                    return;
                }
            }
        });

        // 鼠标移到按钮/热区上：显示“小手”，并触发放大动画
        setOnMouseMoved(e -> {
            Rectangle2D r = buttonRect();
            boolean over = (r != null && r.contains(e.getX(), e.getY()));
            if (!over) {
                Point2D p = toImageCoords(e.getX(), e.getY());
                for (Hotspot hs : hotspots) {
                    if (hs.contains(p.getX(), p.getY())) { over = true; break; }
                }
            }
            setCursor(over ? Cursor.HAND : Cursor.DEFAULT);
            setHoverState(over);
        });
        setOnMouseExited(e -> setHoverState(false)); // 鼠标离开界面时恢复原样
    }

    /** 添加一个热区：矩形用图片像素坐标 (x, y, w, h)。 */
    private void addHotspot(double x, double y, double w, double h, Runnable action) {
        hotspots.add(new Hotspot(x, y, w, h, action));
    }

    /** 悬停时按钮放大、离开时缩回原样（只在状态变化时触发动画）。 */
    private void setHoverState(boolean over) {
        if (over == overStart) return;
        overStart = over;
        hoverAnim.stop();
        hoverAnim.setToX(over ? 1.15 : 1.0);
        hoverAnim.setToY(over ? 1.15 : 1.0);
        hoverAnim.playFromStart();
    }

    /**
     * 把 START.png 放到窗口里：中心 = 你测出的开始按钮位置的中心(+微调)，
     * 宽度 = 背景图上的固定宽度 × 缩放系数（随背景一起拉伸，不错位、不变形）。
     */
    private void layoutStartButton() {
        Rectangle2D r = buttonRect();
        if (r == null) return;
        startButton.setLayoutX(r.getMinX());
        startButton.setLayoutY(r.getMinY());
        startButton.setFitWidth(r.getWidth());
        startButton.setFitHeight(r.getHeight());

        // 调试框跟着按钮走
        debugRect.setX(r.getMinX());
        debugRect.setY(r.getMinY());
        debugRect.setWidth(r.getWidth());
        debugRect.setHeight(r.getHeight());
    }

    /** 算出按钮当前的窗口矩形（用来摆放、判断点击和悬停）。 */
    private Rectangle2D buttonRect() {
        double paneW = getWidth();
        double paneH = getHeight();
        if (paneW <= 0 || paneH <= 0) return null;

        // 背景图 cover 铺满窗口的换算系数（和 buildBackground 一致）
        double scale = Math.max(paneW / bgImage.getWidth(), paneH / bgImage.getHeight());
        double offsetX = (paneW - bgImage.getWidth() * scale) / 2;
        double offsetY = (paneH - bgImage.getHeight() * scale) / 2;

        // “开始游戏”位置的中心点（可被方向键微调）→ 窗口坐标
        double centerImgX = START_CX + offX;
        double centerImgY = START_CY + offY;
        double centerW = centerImgX * scale + offsetX;
        double centerH = centerImgY * scale + offsetY;

        // 宽度 = 背景图像素宽 × 缩放系数（背景放大多少，按钮就放大多少）
        double w = BUTTON_W_IMG * scale;
        double imgW = startButton.getImage().getWidth();
        double imgH = startButton.getImage().getHeight();
        double h = w * imgH / imgW; // 高度按 START.png 原比例，不变形

        return new Rectangle2D(centerW - w / 2, centerH - h / 2, w, h);
    }

    // ================= 对齐调试工具 =================

    /** 方向键微调：把按钮中心往 dx/dy 方向挪（单位：背景图像素）。 */
    public void nudge(double dx, double dy) {
        offX += dx;
        offY += dy;
        layoutStartButton();
        printCenter();
    }

    /** F3：显示/隐藏青色调试框。 */
    public void toggleDebug() {
        debugRect.setVisible(!debugRect.isVisible());
    }

    /** 打印当前按钮中心在背景图上的坐标（把结果发我或记下来固化进常量）。 */
    private void printCenter() {
        double cx = START_CX + offX;
        double cy = START_CY + offY;
        System.out.printf("按钮中心(背景图像素) = (%d, %d)%n", (int) cx, (int) cy);
    }

    /** 把窗口坐标换算回图片坐标（cover 裁剪的反推，校准其它按钮时用）。 */
    private Point2D toImageCoords(double winX, double winY) {
        double regionW = getWidth();
        double regionH = getHeight();
        double imgW = bgImage.getWidth();
        double imgH = bgImage.getHeight();

        double scale = Math.max(regionW / imgW, regionH / imgH);
        double offsetX = (regionW - imgW * scale) / 2;
        double offsetY = (regionH - imgH * scale) / 2;

        return new Point2D((winX - offsetX) / scale, (winY - offsetY) / scale);
    }

    /** 加载 start_menu.png 作为背景，等比放大铺满（cover）。 */
    private static Background buildBackground(Image image) {
        BackgroundImage bgImage = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1, 1, true, true, false, true)
        );
        return new Background(bgImage);
    }
}
