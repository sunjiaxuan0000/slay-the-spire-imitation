package com.example.demo.card;

import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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

    private static final double NAME_Y = 16;           // 卡名中心 y（卡上方）
    private static final double EMBLEM_CX = CARD_W / 2; // 稀有度飘带中心 x（水平居中）
    private static final double EMBLEM_CY = 104;       // 稀有度飘带中心 y（卡中部）
    private static final double EMBLEM_W = 108;        // 飘带宽
    private static final double EMBLEM_H = 64;         // 飘带高
    private static final double DESC_TOP = 136;        // 描述文字区顶部 y（卡下方）
    private static final double DESC_W = 126;          // 描述最大宽度

    private static final double BADGE_X = 6;           // power 徽章左上角 x（卡左上角）
    private static final double BADGE_Y = 4;
    private static final double BADGE_SIZE = 42;       // 徽章边长
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
        Pane face = buildRaw(c);
        double scale = width / CARD_W;
        face.setScaleX(scale);
        face.setScaleY(scale);
        StackPane wrap = new StackPane(face);
        wrap.setPrefSize(width, width * 1.4);
        wrap.setMaxSize(width, width * 1.4);
        wrap.setAlignment(javafx.geometry.Pos.CENTER);
        return wrap;
    }

    private static Pane buildRaw(Card c) {
        Pane root = new Pane();
        root.setPrefSize(CARD_W, CARD_H);
        root.setMaxSize(CARD_W, CARD_H);

        // ---------- 1) 基础卡面（铺满全卡）----------
        Image base = switch (c.kind.type) {
            case ATTACK -> FACE_ATTACK;
            case SKILL  -> FACE_SKILL;
            case POWER  -> FACE_ABILITY;
            case STATUS -> FACE_SKILL; // 状态牌暂无专用面，先借技能面
        };
        ImageView baseView = new ImageView(base);
        baseView.setFitWidth(CARD_W);
        baseView.setFitHeight(CARD_H);
        baseView.setLayoutX(0);
        baseView.setLayoutY(0);
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
        typeLabel.setStyle("-fx-font-weight: bold;");
        typeLabel.setLayoutX(EMBLEM_CX - 20);
        typeLabel.setLayoutY(EMBLEM_CY - 20);
        typeLabel.setPrefWidth(40);
        typeLabel.setAlignment(javafx.geometry.Pos.CENTER);
        root.getChildren().add(typeLabel);

        // ---------- 3) 卡名（上方） ----------
        Label name = new Label(c.kind.label);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(17));
        name.setStyle("-fx-font-weight: bold;");
        name.setEffect(new DropShadow(3, Color.BLACK));
        name.setLayoutX(8);
        name.setLayoutY(NAME_Y - 12);
        name.setPrefWidth(CARD_W - 16);
        name.setAlignment(javafx.geometry.Pos.CENTER);
        root.getChildren().add(name);

        // ---------- 4) 描述（下方） ----------
        Label desc = new Label(c.kind.desc);
        desc.setTextFill(Color.rgb(245, 245, 245));
        desc.setFont(Font.font(12));
        desc.setEffect(new DropShadow(2, Color.BLACK));
        desc.setLayoutX((CARD_W - DESC_W) / 2);
        desc.setLayoutY(DESC_TOP);
        desc.setPrefWidth(DESC_W);
        desc.setWrapText(true);
        desc.setAlignment(javafx.geometry.Pos.TOP_CENTER);
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
        cost.setStyle("-fx-font-weight: bold;");
        cost.setEffect(new DropShadow(2, Color.BLACK));
        cost.setLayoutX(BADGE_X);
        cost.setLayoutY(BADGE_Y + (BADGE_SIZE - 24) / 2);
        cost.setPrefWidth(BADGE_SIZE);
        cost.setAlignment(javafx.geometry.Pos.CENTER);
        root.getChildren().add(cost);

        return root;
    }
}
