package com.example.demo.card;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

/**
 * 多层贴图卡面（由旧版 CardFaceView 重构而来，适配新分包 com.example.demo.card）。
 *
 * 层次（自底向上）：
 *   1) 基础卡面：ability.png(能力) / attack.png(攻击) / skill.png(技能)
 *   2) 稀有度飘带：normal.png(weight=4 白) / rare.png(3 蓝) / gold.png(2 金)，类型文字写在飘带中间
 *   3) 卡名：卡上方
 *   4) 描述：卡下方
 *   5) 能量徽章 power.png：左上角，上面写能量
 *
 * ★ 想调某层贴图位置：改“布局参数”区常量即可（坐标系 = 卡面 150×210）。
 */
public class CardFaceView {

    // ================= 布局参数（卡面 150 × 210 坐标系）=================
    public static final double CARD_W = 150;
    public static final double CARD_H = 210;

    private static final double EMBLEM_CX = CARD_W / 2; // 稀有度飘带中心 x（水平居中）
    private static final double EMBLEM_CY = 104;       // 稀有度飘带中心 y（卡中部）
    private static final double EMBLEM_W = 108;        // 飘带宽
    private static final double EMBLEM_H = 64;         // 飘带高

    private static final double BADGE_X = 6;           // power 徽章左上角 x（卡左上角）
    private static final double BADGE_Y = 4;
    private static final double BADGE_SIZE = 42;       // 徽章边长

    // ---- 卡名 / 描述底板（浅色压底，把文字从花哨的底图里托出来）----
    /**
     * 基础卡面纵向放大到的高度（横向仍是 CARD_W，不做缩放）。
     *
     * 原图 756×1126，实际非透明内容只到 y≈1048（约 93%），底部约 7% 是透明留白。
     * 按 150×210 铺满时粉色底边只到 card y≈194，而描述底板底端在 y=202，
     * 于是底板会落进那块透明留白里 —— 看起来就是"描述跑出了卡面边框"。
     *
     * 纵向放大到 230（≈1.095×）并把内容顶边对齐卡面顶端，粉色底边正好铺到 y=210，
     * 描述底板就被完整包在边框内。
     *
     * 横向**故意不动**：左右两侧的粉色边框在源图里只有约 18px 宽（≈3.6 卡面像素），
     * 一旦等比放大会被裁边切掉，所以这里只拉纵向。
     */
    private static final double BASE_FACE_H = 230;
    private static final double BASE_FACE_Y = -4;      // 顶边略微上提，盖住源图顶部 1~2% 的透明留白
    private static final double NAME_PLATE_X = 50;     // 特意让开左上角的能量徽章
    private static final double NAME_PLATE_Y = 5;
    private static final double NAME_PLATE_W = 94;
    private static final double NAME_PLATE_H = 30;
    private static final double DESC_PLATE_X = 8;
    private static final double DESC_PLATE_Y = 134;
    private static final double DESC_PLATE_W = 134;
    private static final double DESC_PLATE_H = 68;
    private static final double PLATE_ARC = 10;        // 底板圆角
    private static final double DESC_PAD = 6;          // 描述文字内边距

    // ---- 配色：暖米白底板 + 深咖文字，柔和不刺眼，对比也够 ----
    private static final Color PLATE_FILL = Color.rgb(252, 245, 230, 0.95);
    private static final Color PLATE_STROKE = Color.rgb(120, 98, 70, 0.45);
    private static final Color NAME_TEXT = Color.rgb(74, 54, 36);
    private static final Color DESC_TEXT = Color.rgb(88, 72, 56);
    private static final double NAME_FONT = 17;
    private static final double DESC_FONT = 12.5;

    /**
     * 文字的十六进制色，配合 inline {@code -fx-text-fill} 使用。
     *
     * ★ 为什么要用 inline style 而不是只靠 {@code setTextFill()}：
     * JavaFX 里 CSS 的优先级高于代码 setter，modena 样式表对 .label 有
     * {@code -fx-text-fill: -fx-text-background-color}，而 {@code -fx-text-background-color}
     * 是由祖先的 {@code -fx-background} 推导的"背景阶梯色"。
     * 牌堆浏览页 / 牌组页的 ScrollPane 设了 {@code -fx-background: #0f172a}（深色），
     * 于是阶梯色推成白色，把我们 setTextFill 的深咖色整个盖掉 ——
     * 卡面一到那个弹层里就变成白字。
     * inline style 优先级最高，可以钉死颜色，不再受祖先背景影响。
     */
    private static final String NAME_TEXT_CSS = "#4a3624";
    private static final String DESC_TEXT_CSS = "#584838";
    private static final String ON_ART_TEXT_CSS = "#ffffff"; // 飘带/徽章上的字，压在深色图案上
    // ======================================================

    private static Image img(String name) {
        // 用“/com/example/demo/icons/…”绝对路径：本类已移到 card 子包，
        // 相对路径会错误地找 card/icons/…
        var in = CardFaceView.class.getResourceAsStream("/com/example/demo/icons/" + name);
        return in == null ? null : new Image(in);
    }

    private static final Image FACE_ATTACK = img("attack.png");
    private static final Image FACE_SKILL = img("skill.png");
    private static final Image FACE_ABILITY = img("ability.png");
    private static final Image RIBBON_COMMON = img("normal.png"); // 4 白
    private static final Image RIBBON_RARE = img("rare.png");     // 3 蓝
    private static final Image RIBBON_GOLD = img("gold.png");     // 1 金
    private static final Image POWER = img("power.png");

    /** 生成一张分层贴图卡面（150×210） */
    public static Pane build(Card c) {
        return buildAt(c, CARD_W);
    }

    /** 按目标宽度等比生成卡面并外套固定尺寸容器（堆/组/列表浏览用），高度 = 宽 × 1.4 */
    public static StackPane buildAt(Card c, double width) {
        return buildAt(c, width, c.kind.desc);
    }

    /**
     * 按目标宽度等比生成卡面，描述文字使用 {@code descOverride}（战斗中用于显示含力量/易伤加成的实际数值）。
     */
    public static StackPane buildAt(Card c, double width, String descOverride) {
        Pane face = buildRaw(c, descOverride);
        double scale = width / CARD_W;
        face.setScaleX(scale);
        face.setScaleY(scale);
        StackPane wrap = new StackPane(face);
        wrap.setPrefSize(width, width * 1.4);
        wrap.setMaxSize(width, width * 1.4);
        wrap.setAlignment(javafx.geometry.Pos.CENTER);
        return wrap;
    }

    /** 浅色圆角底板：垫在卡名/描述下面，把文字从花哨的底图和飘带里托出来 */
    private static Rectangle plate(double x, double y, double w, double h) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setArcWidth(PLATE_ARC);
        r.setArcHeight(PLATE_ARC);
        r.setFill(PLATE_FILL);
        r.setStroke(PLATE_STROKE);
        r.setStrokeWidth(1.2);
        r.setMouseTransparent(true); // 底板只负责好看，别抢卡片的点击
        return r;
    }

    private static Pane buildRaw(Card c) {
        return buildRaw(c, c.kind.desc);
    }

    private static Pane buildRaw(Card c, String descOverride) {
        Pane root = new Pane();
        root.setPrefSize(CARD_W, CARD_H);
        root.setMaxSize(CARD_W, CARD_H);
        // 基础卡面纵向放大后底部会溢出卡面，用矩形裁剪把可视区限定在 150×210 内
        root.setClip(new Rectangle(0, 0, CARD_W, CARD_H));

        // ---------- 1) 基础卡面（横向铺满 150，纵向放大到 BASE_FACE_H）----------
        Image base = switch (c.kind.type) {
            case ATTACK -> FACE_ATTACK;
            case SKILL  -> FACE_SKILL;
            case POWER  -> FACE_ABILITY;
            case STATUS -> FACE_SKILL; // 状态牌暂无专用面，先借技能面
        };
        ImageView baseView = new ImageView(base);
        double baseW = BASE_FACE_H * (base.getWidth() / base.getHeight()); // 按源图等比，不拉伸
        baseView.setFitWidth(baseW);
        baseView.setFitHeight(BASE_FACE_H);
        baseView.setLayoutX((CARD_W - baseW) / 2); // 居中：只裁掉源图左右那圈透明留白
        baseView.setLayoutY(BASE_FACE_Y);
        root.getChildren().add(baseView);

        // ---------- 2) 稀有度飘带（中间）+ 类型文字 ----------
        Image ribbon = switch (c.kind.weight) {
            case 4 -> RIBBON_COMMON;
            case 3 -> RIBBON_RARE;
            case 1 -> RIBBON_GOLD;
            default -> RIBBON_COMMON;
        };
        ImageView ribbonView = new ImageView(ribbon);
        ribbonView.setFitWidth(EMBLEM_W);
        ribbonView.setFitHeight(EMBLEM_H);
        ribbonView.setLayoutX(EMBLEM_CX - EMBLEM_W / 2);
        ribbonView.setLayoutY(EMBLEM_CY - EMBLEM_H / 2);
        root.getChildren().add(ribbonView);

        String typeText = switch (c.kind.type) {
            case ATTACK -> "攻击";
            case SKILL  -> "技能";
            case POWER  -> "能力";
            case STATUS -> "状态";
        };
        Label typeLabel = new Label(typeText);
        typeLabel.setTextFill(Color.WHITE);
        typeLabel.setFont(Font.font(16));
        // 颜色走 inline style：见 NAME_TEXT_CSS 上面的说明（CSS 会盖过 setTextFill）
        typeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ON_ART_TEXT_CSS + ";");
        typeLabel.setLayoutX(EMBLEM_CX - 20);
        typeLabel.setLayoutY(EMBLEM_CY - 20);
        typeLabel.setPrefWidth(40);
        typeLabel.setAlignment(javafx.geometry.Pos.CENTER);
        root.getChildren().add(typeLabel);

        // ---------- 3) 卡名（上方）：浅色底板 + 深咖粗体 ----------
        root.getChildren().add(plate(NAME_PLATE_X, NAME_PLATE_Y, NAME_PLATE_W, NAME_PLATE_H));

        Label name = new Label(c.kind.label);
        name.setTextFill(NAME_TEXT);
        name.setFont(Font.font(NAME_FONT));
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: " + NAME_TEXT_CSS + ";");
        name.setLayoutX(NAME_PLATE_X);
        name.setLayoutY(NAME_PLATE_Y);
        name.setPrefWidth(NAME_PLATE_W);
        name.setPrefHeight(NAME_PLATE_H);
        name.setAlignment(Pos.CENTER);
        root.getChildren().add(name);

        // ---------- 4) 描述（下方）：浅色底板 + 深灰褐正文，整块垂直居中 ----------
        root.getChildren().add(plate(DESC_PLATE_X, DESC_PLATE_Y, DESC_PLATE_W, DESC_PLATE_H));

        Label desc = new Label(descOverride);
        desc.setTextFill(DESC_TEXT);
        desc.setFont(Font.font(DESC_FONT));
        desc.setStyle("-fx-text-fill: " + DESC_TEXT_CSS + ";");
        desc.setLayoutX(DESC_PLATE_X);
        desc.setLayoutY(DESC_PLATE_Y);
        desc.setPrefWidth(DESC_PLATE_W);
        desc.setPrefHeight(DESC_PLATE_H);
        desc.setWrapText(true);
        desc.setAlignment(Pos.CENTER);
        desc.setPadding(new Insets(DESC_PAD));
        root.getChildren().add(desc);

        // ---------- 5) power 徽章（左上角）+ 能量数字 ----------
        ImageView badge = new ImageView(POWER);
        badge.setFitWidth(BADGE_SIZE);
        badge.setFitHeight(BADGE_SIZE);
        badge.setLayoutX(BADGE_X);
        badge.setLayoutY(BADGE_Y);
        root.getChildren().add(badge);

        Label cost = new Label(c.cost >= 0 ? String.valueOf(c.cost) : "X");
        cost.setTextFill(Color.WHITE);
        cost.setFont(Font.font(20));
        cost.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ON_ART_TEXT_CSS + ";");
        cost.setEffect(new DropShadow(2, Color.BLACK));
        cost.setLayoutX(BADGE_X);
        cost.setLayoutY(BADGE_Y + (BADGE_SIZE - 24) / 2);
        cost.setPrefWidth(BADGE_SIZE);
        cost.setAlignment(javafx.geometry.Pos.CENTER);
        root.getChildren().add(cost);

        return root;
    }
}
