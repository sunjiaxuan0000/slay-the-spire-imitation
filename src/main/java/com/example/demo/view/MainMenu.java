package com.example.demo.view;

import javafx.animation.ScaleTransition;
import javafx.event.EventTarget;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面：背景 start_menu.png，上面叠三张按钮图：
 *   START.png（开始游戏）—— 位置是你用方向键对齐后固化下来的 (320, 497)
 *   set.png （设置）      —— 在“开始游戏”正下方
 *   exit.png（退出）      —— 在“设置”正下方
 *
 * 所有按钮都用「背景图像素坐标」定位：位置和大小都会乘上背景的 cover 缩放系数，
 * 所以窗口怎么拉伸，按钮都和背景图保持同比例、不错位。
 *
 * 想微调 set/exit 的位置：改下面的 SET_CY / EXIT_CY（或运行后按方向键调 START，
 * 点击界面时控制台会打印你点的“背景图像素坐标”，照着填即可）。
 */
public class MainMenu extends Pane {

    // ===== “开始游戏”按钮位置（背景图像素）：你用方向键对齐后的中心 = (320, 497) =====
    private static final double START_CX = 320; // 按钮中心 X
    private static final double START_CY = 497; // 按钮中心 Y
    // ==================================================================================

    /** 开始按钮显示宽度（背景图像素）；取/退按钮的宽度见下面各自的常量。 */
    private static final double BUTTON_W_IMG = 520;

    // ===== “设置”“退出”按钮位置（背景图像素）：都在开始游戏正下方，X 与之一致 =====
    private static final double SET_CX = 320;
    private static final double SET_CY = 620;   // 抬高后：占 567~672
    private static final double SET_W_IMG = 300;

    private static final double EXIT_CX = 320;
    private static final double EXIT_CY = 770;  // 抬高后：占 705~835
    private static final double EXIT_W_IMG = 300;
    // ================================================================================

    // ===== 「放弃上局游戏」按钮（只在有存档时出现）：摆在「开始游戏」右边 =====
    // 开始游戏占 x=60~580，所以这个按钮从 630 起，两者之间留 50px 空隙。
    private static final double ABANDON_CX = 800;  // 按钮中心 X（背景图像素）
    private static final double ABANDON_CY = 497;  // 与「开始游戏」同一水平线
    private static final double ABANDON_W_IMG = 340;
    private static final double ABANDON_H_IMG = 96;
    private static final double ABANDON_FONT_IMG = 40; // 字号也按背景缩放，跟着窗口一起变大变小
    // ================================================================================

    /** 一个图片按钮：图片里的一张图 + 它在背景图上的中心/宽度 + 点击动作。 */
    private static class ImageButton {
        final String name;
        final ImageView view;
        final double cx;
        final double cy;
        final double w;
        final Runnable action;
        final ScaleTransition hover;
        double offX = 0; // 方向键微调量（背景图像素）
        double offY = 0;

        ImageButton(String name, ImageView view, double cx, double cy, double w, Runnable action) {
            this.name = name;
            this.view = view;
            this.cx = cx;
            this.cy = cy;
            this.w = w;
            this.action = action;
            this.hover = new ScaleTransition(Duration.millis(120), view);
        }

        /** 当前中心（背景图像素，含微调） */
        double centerX() { return cx + offX; }

        double centerY() { return cy + offY; }
    }

    private final Image bgImage;
    private final List<ImageButton> buttons = new ArrayList<>();
    private final List<Rectangle> debugRects = new ArrayList<>(); // 青色框：显示各按钮当前范围（F3）
    private final Button abandonBtn; // 「放弃上局游戏」；没有存档时为 null
    private ImageButton hovered;
    private int selected = 0; // 方向键当前在调哪个按钮（Tab 切换）

    /** 没有存档时的老写法：只有开始 / 设置 / 退出三个按钮。 */
    public MainMenu(Runnable onStartGame, Runnable onSettings, Runnable onExit) {
        this(onStartGame, onSettings, onExit, null);
    }

    /**
     * @param onAbandon 「放弃上局游戏」的点击动作；<b>传 null = 当前没有存档，不显示这个按钮</b>
     */
    public MainMenu(Runnable onStartGame, Runnable onSettings, Runnable onExit, Runnable onAbandon) {
        bgImage = new Image(MainMenu.class.getResourceAsStream("/com/example/demo/start_menu.png"));
        setBackground(buildBackground(bgImage));

        addButton("开始", "/com/example/demo/START.png", START_CX, START_CY, BUTTON_W_IMG, onStartGame);
        addButton("设置", "/com/example/demo/icons/set.png", SET_CX, SET_CY, SET_W_IMG, onSettings);
        addButton("退出", "/com/example/demo/icons/exit.png", EXIT_CX, EXIT_CY, EXIT_W_IMG, onExit);

        // 有存档：在「开始游戏」右边再放一个「放弃上局游戏」。
        // 它是个纯文字按钮（没有美术素材），位置和大小同样按背景图坐标换算，见 layoutAbandonButton。
        if (onAbandon != null) {
            abandonBtn = new Button("放弃上局游戏");
            // 样式全部 inline 钉死：项目里的 .button 默认样式会盖掉代码里设的颜色
            abandonBtn.setStyle(
                    "-fx-background-color: rgba(127, 29, 29, 0.88);"
                            + "-fx-text-fill: #fee2e2;"
                            + "-fx-border-color: #f87171;"
                            + "-fx-border-width: 2;"
                            + "-fx-background-radius: 8;"
                            + "-fx-border-radius: 8;");
            abandonBtn.setCursor(Cursor.HAND);
            abandonBtn.setOnAction(e -> onAbandon.run());
            getChildren().add(abandonBtn);
        } else {
            abandonBtn = null;
        }

        // 调试用的青色外框（默认隐藏，F3 打开），每个按钮一个，画在最上层方便对齐
        for (int i = 0; i < buttons.size(); i++) {
            Rectangle r = new Rectangle();
            r.setFill(null);
            r.setStroke(javafx.scene.paint.Color.rgb(34, 211, 238, 0.9));
            r.setStrokeWidth(2);
            r.setVisible(false);
            getChildren().add(r);
            debugRects.add(r);
        }

        // 窗口尺寸一变，就重新计算按钮图的位置和大小
        widthProperty().addListener(o -> layoutButtons());
        heightProperty().addListener(o -> layoutButtons());

        // 点击：命中哪个按钮就执行哪个；没命中就打印图片坐标（用来校准位置）
        setOnMouseClicked(e -> {
            ImageButton hit = buttonAt(e.getX(), e.getY());
            if (hit != null) {
                hit.action.run();
                return;
            }
            Point2D p = toImageCoords(e.getX(), e.getY());
            System.out.println("点击了图片坐标: (" + (int) p.getX() + ", " + (int) p.getY() + ")");
        });

        // 鼠标移到按钮上：显示“小手”，并触发放大动画
        setOnMouseMoved(e -> {
            ImageButton over = buttonAt(e.getX(), e.getY());
            setCursor(over != null ? Cursor.HAND : Cursor.DEFAULT);
            setHovered(over);
        });
        setOnMouseExited(e -> setHovered(null)); // 鼠标离开界面时恢复原样
    }

    /** 加一个图片按钮（图片按原比例显示，宽度 = w × 背景缩放系数）。 */
    private void addButton(String name, String resource, double cx, double cy, double w, Runnable action) {
        ImageView iv = new ImageView(new Image(MainMenu.class.getResourceAsStream(resource)));
        iv.setPreserveRatio(true);    // 保持原比例，绝不压扁
        iv.setMouseTransparent(true); // 鼠标事件穿透，交给本面板统一处理
        getChildren().add(iv);
        buttons.add(new ImageButton(name, iv, cx, cy, w, action));
    }

    /** 鼠标当前落在哪个按钮上（没命中返回 null）。 */
    private ImageButton buttonAt(double winX, double winY) {
        for (ImageButton b : buttons) {
            Rectangle2D r = buttonRect(b);
            if (r != null && r.contains(winX, winY)) return b;
        }
        return null;
    }

    /** 悬停时按钮放大、离开时缩回原样（只对状态变化的那一个按钮做动画）。 */
    private void setHovered(ImageButton target) {
        if (hovered == target) return;
        if (hovered != null) {
            hovered.hover.stop();
            hovered.hover.setToX(1.0);
            hovered.hover.setToY(1.0);
            hovered.hover.playFromStart();
        }
        hovered = target;
        if (hovered != null) {
            hovered.hover.stop();
            hovered.hover.setToX(1.12);
            hovered.hover.setToY(1.12);
            hovered.hover.playFromStart();
        }
    }

    /**
     * 把所有按钮摆到窗口里：中心 = 背景图上的坐标(+微调) × 缩放系数，
     * 宽度 = 背景图上的固定宽度 × 缩放系数（随背景一起拉伸，不错位、不变形）。
     */
    private void layoutButtons() {
        for (int i = 0; i < buttons.size(); i++) {
            ImageButton b = buttons.get(i);
            Rectangle2D r = buttonRect(b);
            if (r == null) continue;
            b.view.setLayoutX(r.getMinX());
            b.view.setLayoutY(r.getMinY());
            b.view.setFitWidth(r.getWidth());
            b.view.setFitHeight(r.getHeight());

            // 调试框跟着按钮走
            Rectangle box = debugRects.get(i);
            box.setX(r.getMinX());
            box.setY(r.getMinY());
            box.setWidth(r.getWidth());
            box.setHeight(r.getHeight());
        }

        layoutAbandonButton();
    }

    /**
     * 摆放「放弃上局游戏」：位置 / 大小 / 字号同样按背景图的 cover 缩放系数换算，
     * 所以窗口怎么拉伸，它和「开始游戏」的相对位置都不变。
     *
     * <p><b>夹回可见范围是必要的：</b>窗口比背景图更「高瘦」时，背景会被左右裁掉一截，
     * 按背景坐标算出来的位置可能落到屏幕外 —— 那样按钮就点不到了。</p>
     */
    private void layoutAbandonButton() {
        if (abandonBtn == null) return;
        double paneW = getWidth();
        double paneH = getHeight();
        if (paneW <= 0 || paneH <= 0) return;

        double scale = Math.max(paneW / bgImage.getWidth(), paneH / bgImage.getHeight());
        double offsetX = (paneW - bgImage.getWidth() * scale) / 2;
        double offsetY = (paneH - bgImage.getHeight() * scale) / 2;

        double w = ABANDON_W_IMG * scale;
        double h = ABANDON_H_IMG * scale;
        abandonBtn.setPrefSize(w, h);
        abandonBtn.setMinSize(w, h);
        abandonBtn.setMaxSize(w, h);
        abandonBtn.setFont(Font.font(ABANDON_FONT_IMG * scale));

        double x = ABANDON_CX * scale + offsetX - w / 2;
        double y = ABANDON_CY * scale + offsetY - h / 2;
        x = Math.max(8, Math.min(x, paneW - w - 8)); // 别跑出屏幕
        y = Math.max(8, Math.min(y, paneH - h - 8));
        abandonBtn.setLayoutX(x);
        abandonBtn.setLayoutY(y);
    }

    /** 一次鼠标事件的落点是不是在 node 上（含它的子节点，比如按钮里的文字）。 */
    private static boolean isInside(EventTarget target, Node node) {
        if (node == null || !(target instanceof Node start)) return false;
        for (Node cur = start; cur != null; cur = cur.getParent()) {
            if (cur == node) return true;
        }
        return false;
    }

    /** 算出某个按钮当前的窗口矩形（用来摆放、判断点击和悬停）。 */
    private Rectangle2D buttonRect(ImageButton b) {
        double paneW = getWidth();
        double paneH = getHeight();
        if (paneW <= 0 || paneH <= 0) return null;

        // 背景图 cover 铺满窗口的换算系数（和 buildBackground 一致）
        double scale = Math.max(paneW / bgImage.getWidth(), paneH / bgImage.getHeight());
        double offsetX = (paneW - bgImage.getWidth() * scale) / 2;
        double offsetY = (paneH - bgImage.getHeight() * scale) / 2;

        double centerW = b.centerX() * scale + offsetX;
        double centerH = b.centerY() * scale + offsetY;

        double w = b.w * scale;
        double imgW = b.view.getImage().getWidth();
        double imgH = b.view.getImage().getHeight();
        double h = w * imgH / imgW; // 高度按原比例，不变形

        return new Rectangle2D(centerW - w / 2, centerH - h / 2, w, h);
    }

    // ================= 对齐调试工具 =================

    /**
     * 方向键微调：把「当前选中的按钮」中心往 dx/dy 方向挪（单位：背景图像素）。
     * 按 Tab 可以在 开始 / 设置 / 退出 之间切换选中目标。
     */
    public void nudge(double dx, double dy) {
        ImageButton b = buttons.get(selected);
        b.offX += dx;
        b.offY += dy;
        layoutButtons();
        printCenter();
    }

    /** Tab：切换方向键调节哪一个按钮（开始 → 设置 → 退出 → 开始） */
    public void selectNext() {
        selected = (selected + 1) % buttons.size();
        System.out.println("方向键现在调节：" + buttons.get(selected).name + " 按钮");
        printCenter();
    }

    /** F3：显示/隐藏青色调试框（三个按钮各一个）。 */
    public void toggleDebug() {
        boolean show = !debugRects.get(0).isVisible();
        for (Rectangle r : debugRects) r.setVisible(show);
    }

    /**
     * 打印当前选中按钮的中心在背景图上的坐标。
     * （把结果发我 / 记下来，填进 MainMenu 上面的常量即可固化。）
     */
    private void printCenter() {
        ImageButton b = buttons.get(selected);
        System.out.printf("%s 按钮中心(背景图像素) = (%d, %d)  宽度=%d%n",
                b.name, (int) b.centerX(), (int) b.centerY(), (int) b.w);
    }

    /** 把窗口坐标换算回图片坐标（cover 裁剪的反推，校准按钮位置时用）。 */
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
