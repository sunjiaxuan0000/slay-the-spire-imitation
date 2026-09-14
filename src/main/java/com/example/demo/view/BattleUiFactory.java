package com.example.demo.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.function.Consumer;

/**
 * 战斗界面静态 UI 构造工具：从 BattleView 抽出的纯 UI 拼装方法，
 * 不依赖战斗状态，便于复用与单独测试。
 */
public final class BattleUiFactory {

    /**
     * 悬停描述回调：BattleView 会把它接到画面上的"描述条"上。
     * 悬停图标时传入描述文字，移开时传空串。这样不依赖 Tooltip 也能看到描述。
     */
    public static Consumer<String> hoverConsumer;

    private BattleUiFactory() {
    }

    /** 给任意节点挂上悬停描述（Tooltip + 描述条 双保险） */
    public static void attachHover(javafx.scene.Node node, String tip) {
        node.setPickOnBounds(true);
        Tooltip t = new Tooltip(tip);
        t.setShowDelay(javafx.util.Duration.millis(150)); // 默认 1 秒太久
        Tooltip.install(node, t);
        node.setOnMouseEntered(e -> {
            if (hoverConsumer != null) hoverConsumer.accept(tip);
        });
        node.setOnMouseExited(e -> {
            if (hoverConsumer != null) hoverConsumer.accept("");
        });
    }

    /** 血条：底槽 + 填充 + 数字/数字文字，包进 wrap（wrap 负责“有格挡=金属框”） */
    public static void hpWrap(StackPane wrap, Region fill, Label text, double width, String fillColor) {
        HBox bar = new HBox();
        bar.setPrefSize(width, 18);
        bar.setMaxSize(width, 18);
        bar.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 9;");
        bar.getChildren().add(fill);
        fill.setStyle("-fx-background-color: " + fillColor + "; -fx-background-radius: 9;");

        text.setTextFill(Color.WHITE);
        text.setFont(Font.font(13));
        text.setStyle("-fx-font-weight: bold;");

        wrap.setPrefSize(width, 18);
        wrap.setMaxSize(width, 18);
        wrap.getChildren().addAll(bar, text); // 条在底、数字在上
        wrap.setStyle("-fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 10;");
    }

    /** 有格挡时：血条包上金属框 */
    public static String frameStyle(boolean metal) {
        return metal
                ? "-fx-border-color: #e2e8f0; -fx-border-width: 2; -fx-border-radius: 10; "
                        + "-fx-effect: dropshadow(gaussian, rgba(226,232,240,0.35), 4, 0, 0, 0);"
                : "-fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 10;";
    }

    /** 格挡盾牌：圆底 + 盾牌图标 + 格挡数字 */
    public static void shield(StackPane s, Label num) {
        s.setPrefSize(32, 32);
        s.setMaxSize(32, 32);
        s.setStyle("-fx-background-color: #0ea5e9; -fx-background-radius: 16; "
                + "-fx-border-color: #bae6fd; -fx-border-width: 1; -fx-border-radius: 16;");
        s.getChildren().add(num);
        s.setVisible(false);
    }

    /** buff/debuff 图标 + 层数 */
    public static HBox statusChip(String glyph, int count, String color, String tip) {
        return statusChip(glyph, String.valueOf(count), color, tip);
    }

    /** 无层数的能力图标（用于常驻能力牌，如残暴） */
    public static HBox statusChip(String glyph, String color, String tip) {
        return statusChip(glyph, null, color, tip);
    }

    private static HBox statusChip(String glyph, String count, String color, String tip) {
        StackPane icon = new StackPane();
        icon.setPrefSize(24, 24);
        icon.setMaxSize(24, 24);
        icon.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12;");
        Label g = new Label(glyph);
        g.setTextFill(Color.WHITE);
        g.setFont(Font.font(12));
        g.setStyle("-fx-font-weight: bold;");
        icon.getChildren().add(g);

        HBox chip = new HBox(3);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPickOnBounds(true); // 整个图标+数字区域都能触发悬停提示
        chip.getChildren().add(icon);
        if (count != null) {
            Label n = new Label(count);
            n.setTextFill(Color.WHITE);
            n.setFont(Font.font(13));
            n.setStyle("-fx-font-weight: bold;");
            chip.getChildren().add(n);
        }
        attachHover(chip, tip); // Tooltip + 屏幕描述条
        return chip;
    }

    /** 菱形状态标（神风猪蓄势专用）：方块旋转 45° 成菱形，内文反向旋转保持正立 */
    public static HBox diamondChip(String glyph, int count, String color, String tip) {
        StackPane icon = new StackPane();
        icon.setPrefSize(24, 24);
        icon.setMaxSize(24, 24);
        icon.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        icon.setRotate(45);
        Label g = new Label(glyph);
        g.setTextFill(Color.WHITE);
        g.setFont(Font.font(12));
        g.setStyle("-fx-font-weight: bold;");
        g.setRotate(-45); // 抵消菱形旋转，文字保持正立
        icon.getChildren().add(g);

        HBox chip = new HBox(3);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPickOnBounds(true);
        chip.getChildren().add(icon);
        Label n = new Label(String.valueOf(count));
        n.setTextFill(Color.WHITE);
        n.setFont(Font.font(13));
        n.setStyle("-fx-font-weight: bold;");
        chip.getChildren().add(n);
        attachHover(chip, tip);
        return chip;
    }

    /** 立绘圆（占位，以后换成 ImageView） */
    public static StackPane portrait(String glyph, String gradient) {
        StackPane p = new StackPane();
        p.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 105;");
        Label g = new Label(glyph);
        g.setTextFill(Color.rgb(255, 255, 255, 0.85));
        g.setFont(Font.font(92));
        p.getChildren().add(g);
        return p;
    }

    /** 牌堆小图标（左下抽牌堆 / 右下弃牌堆），右下角带数量下标。
     *  @param glyph 图标上的字（「抽」/「弃」）
     *  @param color 字形颜色（十六进制，如 "#4a3624"）
     */
    public static StackPane pileIcon(String glyph, String color) {
        StackPane icon = new StackPane();
        icon.setPrefSize(76, 100);
        icon.setMaxSize(76, 100);
        icon.setCursor(Cursor.HAND);
        icon.setStyle("-fx-background-color: linear-gradient(to bottom right, #e5e7eb, #9ca3af); "
                + "-fx-background-radius: 10;");
        Label g = new Label(glyph);
        g.setTextFill(Color.web(color)); // 之前这里写死了深灰，参数一直没生效
        g.setFont(Font.font(26));
        g.setStyle("-fx-font-weight: bold;");
        StackPane.setAlignment(g, Pos.CENTER);
        icon.getChildren().add(g);
        return icon;
    }

    /** 把数量下标放到牌堆图标右下角 */
    public static void attachBadge(StackPane icon, Label badge) {
        badge.setTextFill(Color.WHITE);
        badge.setFont(Font.font(13));
        badge.setStyle("-fx-font-weight: bold; -fx-background-color: #dc2626; "
                + "-fx-background-radius: 9; -fx-padding: 1 6 1 6;");
        icon.getChildren().add(badge);
        StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(badge, new Insets(0, 2, 3, 0));
    }

    /** 从资源路径加载图片（path 相对 /com/example/demo/） */
    public static Image loadImage(String path) {
        var in = BattleUiFactory.class.getResourceAsStream("/com/example/demo/" + path);
        return in == null ? null : new Image(in);
    }

    /** 背景用 cover（等比铺满、居中裁边）铺在战斗层 */
    public static Background makeCoverBackground(Image image) {
        BackgroundImage bi = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1, 1, true, true, false, true) // cover
        );
        return new Background(bi);
    }
}
