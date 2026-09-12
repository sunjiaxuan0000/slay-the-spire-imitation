package com.example.demo.character;

import com.example.demo.sound.SoundFx;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * 角色选择界面（模仿《杀戮尖塔》）。
 * 屏幕下方有一张小角色卡，点击选中后：
 * 背景变成该角色的“立绘”（大图），右下角出现开始箭头。
 * 点右下角箭头 = 开始游戏（onStartRun）。
 */
public class CharacterSelect extends StackPane {

    /** 左侧介绍面板的宽度（像素）——想调面板宽窄就改这个数 */
    private static final double INFO_W = 440;

    /** 介绍面板的内边距 */
    private static final double INFO_PAD = 24;

    /** 介绍面板的边框宽度（左右各一条，算文字换行宽度时要扣掉） */
    private static final double INFO_BORDER = 2;

    /** 介绍面板出现/淡入的时长（和立绘淡入保持一致） */
    private static final double INFO_FADE_MS = 400;

    /** 介绍面板从左边滑入的起始偏移 */
    private static final double INFO_SLIDE_FROM = -26;

    /** 初始遗物那一行里图标的边长 */
    private static final double RELIC_ICON_SIZE = 36;

    /** 初始遗物行里 图标 ↔ 名字 ↔ 描述 的间距 */
    private static final double RELIC_ROW_GAP = 10;

    // 下方小角色卡的样式：没选中/已选中
    private static final String CARD_STYLE = "-fx-background-color: rgba(30, 41, 59, 0.95); "
            + "-fx-background-radius: 14; -fx-border-color: #475569; -fx-border-width: 2; "
            + "-fx-border-radius: 14; -fx-padding: 8 14 8 14;";
    private static final String CARD_STYLE_SELECTED = "-fx-background-color: rgba(30, 41, 59, 0.95); "
            + "-fx-background-radius: 14; -fx-border-color: #fbbf24; -fx-border-width: 3; "
            + "-fx-border-radius: 14; -fx-padding: 8 14 8 14;";

    private static final String ARROW_STYLE = "-fx-background-color: #16a34a; -fx-background-radius: 34;";
    private static final String ARROW_STYLE_HOVER = "-fx-background-color: #22c55e; -fx-background-radius: 34;";

    private final StackPane artBack;  // 角色立绘“背景”（选中后出现）
    private final VBox card;          // 屏幕下方的小角色卡
    private final StackPane arrow;    // 右下角开始箭头
    private final Label prompt;       // 未选择时的提示文字
    private final VBox infoPanel;     // 左侧角色介绍面板（选中后出现）

    private boolean selected = false;

    public CharacterSelect(Runnable onStartRun) {
        // ---- 第 0 层：普通深色背景 ----
        StackPane bg = new StackPane();
        bg.setStyle("-fx-background-color: #0f1424;");
        getChildren().add(bg);

        // ---- 第 1 层：点击头像后的“立绘背景”（ironclad_art.png，cover 铺满）----
        artBack = new StackPane();
        artBack.setBackground(bgCover("ironclad_art.png"));
        artBack.setMouseTransparent(true); // 只做背景展示，不拦截鼠标
        artBack.setVisible(false);
        getChildren().add(artBack);

        // ---- 中央提示：还没选角色时 ----
        prompt = new Label("点击下方角色卡选择角色");
        prompt.setTextFill(Color.rgb(203, 213, 225));
        prompt.setFont(Font.font(22));
        StackPane.setAlignment(prompt, Pos.CENTER);
        StackPane.setMargin(prompt, new Insets(0, 0, 90, 0)); // 稍微上移，别挡住下方卡片
        getChildren().add(prompt);

        // ---- 第 3 层：左侧角色介绍面板（选中后淡入；先摆好位置，不占鼠标） ----
        infoPanel = buildInfoPanel();
        infoPanel.setVisible(false);
        infoPanel.setOpacity(0); // 先透明，避免淡入前闪一帧
        StackPane.setAlignment(infoPanel, Pos.CENTER_LEFT);
        StackPane.setMargin(infoPanel, new Insets(0, 0, 0, 46));
        getChildren().add(infoPanel);

        // ---- 第 2 层：屏幕下方的小角色卡 ----
        card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setCursor(Cursor.HAND);
        card.setStyle(CARD_STYLE);

        // 卡内头像（ironclad_head.png）
        StackPane mini = new StackPane();
        mini.setPrefSize(110, 90);
        mini.setMaxSize(110, 90);
        mini.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 10;");
        ImageView head = new ImageView(new Image(CharacterSelect.class.getResourceAsStream("/com/example/demo/ironclad_head.png")));
        head.setPreserveRatio(true);
        head.setFitWidth(100);
        head.setFitHeight(80);
        mini.getChildren().add(head);

        Label cardName = new Label(Player.CHARACTER_NAME);
        cardName.setTextFill(Color.WHITE);
        cardName.setFont(Font.font(20));
        cardName.setStyle("-fx-font-weight: bold;");

        card.getChildren().addAll(mini, cardName);
        StackPane.setAlignment(card, Pos.BOTTOM_CENTER);
        StackPane.setMargin(card, new Insets(0, 0, 18, 0));
        getChildren().add(card);

        // ---- 右下角的开始箭头（选中后才出现） ----
        arrow = new StackPane();
        arrow.setPrefSize(70, 70);
        arrow.setMaxSize(70, 70);
        arrow.setStyle(ARROW_STYLE);
        arrow.setCursor(Cursor.HAND);
        Label arrowText = new Label("▶");
        arrowText.setTextFill(Color.WHITE);
        arrowText.setFont(Font.font(28));
        arrow.setTranslateX(1);
        arrow.getChildren().add(arrowText);
        arrow.setVisible(false);
        arrow.setOnMouseClicked(e -> onStartRun.run());
        arrow.setOnMouseEntered(e -> arrow.setStyle(ARROW_STYLE_HOVER));
        arrow.setOnMouseExited(e -> arrow.setStyle(ARROW_STYLE));
        StackPane.setAlignment(arrow, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(arrow, new Insets(0, 28, 22, 0));
        getChildren().add(arrow);

        // ---- 左上角返回提示 ----
        Label backHint = new Label("Esc 返回主菜单");
        backHint.setTextFill(Color.rgb(100, 116, 139));
        backHint.setFont(Font.font(13));
        StackPane.setAlignment(backHint, Pos.TOP_LEFT);
        StackPane.setMargin(backHint, new Insets(14, 0, 0, 18));
        getChildren().add(backHint);

        // ---- 点击角色卡 → 选中 ----
        card.setOnMouseClicked(e -> select());
        card.setOnMouseEntered(e -> card.setCursor(Cursor.HAND));
    }

    /** 选中角色：背景换成角色立绘、卡片加金框、右下角出现开始箭头。 */
    private void select() {
        if (selected) return;
        selected = true;

        SoundFx.play("ironclad_attack");

        // 背景淡入“立绘”
        artBack.setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.millis(400), artBack);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();

        card.setStyle(CARD_STYLE_SELECTED); // 卡片金框
        prompt.setVisible(false);           // 隐藏中央提示
        arrow.setVisible(true);             // 出现开始箭头
        showInfoPanel();                    // 左侧介绍面板：淡入 + 从左边滑进来

        // 卡片轻轻弹一下
        ScaleTransition pop = new ScaleTransition(Duration.millis(160), card);
        pop.setFromX(0.94);
        pop.setFromY(0.94);
        pop.setToX(1.0);
        pop.setToY(1.0);
        pop.play();
    }

    /**
     * 左侧角色介绍面板：角色名 + 生命 + 简介 + 初始遗物。
     *
     * <p>⚠ 面板必须 {@code setMaxSize(INFO_W, USE_PREF_SIZE)}：它是 StackPane 的孩子，
     * StackPane 会把可伸缩的孩子拉满整页，而 VBox 的 maxHeight 默认是无限 ——
     * 不钉死的话面板会变成一整块深色盖住立绘。</p>
     *
     * <p>文字全部取自 {@link Player}，面板里不写死任何数值/文案，
     * 以后改角色只需改 Player 那几个常量。</p>
     */
    private static VBox buildInfoPanel() {
        Label name = new Label(Player.CHARACTER_NAME);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(30));
        name.setStyle("-fx-font-weight: bold;");

        // 「生命 80/80」：数字来自 Player.BASE_MAX_HP，别在这里写死
        Label hp = new Label("生命 " + Player.BASE_MAX_HP + "/" + Player.BASE_MAX_HP);
        hp.setTextFill(Color.rgb(248, 113, 113)); // 和血条同色系
        hp.setFont(Font.font(17));

        // 名字/生命 与 简介 之间的分隔线：金色渐隐到透明
        Region sep = new Region();
        sep.setPrefHeight(2);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: linear-gradient(to right, #fbbf24, transparent);");

        Label desc = new Label(Player.CHARACTER_DESC);
        desc.setTextFill(Color.rgb(226, 232, 240));
        desc.setFont(Font.font(16));
        desc.setWrapText(true);
        // 换行宽度 = 面板宽 − 左右内边距 − 左右边框
        desc.setMaxWidth(INFO_W - INFO_PAD * 2 - INFO_BORDER * 2);

        // ---- 简介下面：初始遗物（图标 + 名字 + 描述） ----
        // 图标走 Relic.buildIconGraphic —— 那是遗物图标唯一的渲染入口
        Relic starter = Player.STARTER_RELIC;

        Label relicName = new Label(starter.name);
        relicName.setTextFill(Color.rgb(251, 191, 36)); // 金色，和面板边框呼应
        relicName.setFont(Font.font(16));
        relicName.setStyle("-fx-font-weight: bold;");

        Label relicDesc = new Label(starter.desc);
        relicDesc.setTextFill(Color.rgb(203, 213, 225));
        relicDesc.setFont(Font.font(14));
        // 剩下的宽度全给描述：面板宽 − 内边距 − 边框 − 图标 − 两处间距
        relicDesc.setMaxWidth(INFO_W - INFO_PAD * 2 - INFO_BORDER * 2
                - RELIC_ICON_SIZE - RELIC_ROW_GAP * 2);

        HBox relicRow = new HBox(RELIC_ROW_GAP,
                starter.buildIconGraphic(RELIC_ICON_SIZE), relicName, relicDesc);
        relicRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(12, name, hp, sep, desc, relicRow);
        box.setPadding(new Insets(INFO_PAD));
        box.setPrefWidth(INFO_W);
        box.setMaxSize(INFO_W, Region.USE_PREF_SIZE);
        box.setStyle("-fx-background-color: rgba(10, 15, 28, 0.78); "
                + "-fx-background-radius: 16; "
                + "-fx-border-color: rgba(251, 191, 36, 0.55); "
                + "-fx-border-width: " + (int) INFO_BORDER + "; -fx-border-radius: 16;");
        box.setMouseTransparent(true); // 纯展示，不拦鼠标
        return box;
    }

    /** 介绍面板出场：淡入 + 从左边滑进来（和立绘淡入同时开始） */
    private void showInfoPanel() {
        infoPanel.setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.millis(INFO_FADE_MS), infoPanel);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(INFO_FADE_MS), infoPanel);
        slide.setFromX(INFO_SLIDE_FROM);
        slide.setToX(0);
        new ParallelTransition(fade, slide).play();
    }

    /** 读取资源图并做成 cover（等比放大铺满、超出的裁掉）背景 */
    private static Background bgCover(String name) {
        Image image = new Image(CharacterSelect.class.getResourceAsStream("/com/example/demo/" + name));
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
