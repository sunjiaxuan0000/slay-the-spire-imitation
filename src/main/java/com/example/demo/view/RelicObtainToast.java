package com.example.demo.view;

import com.example.demo.character.Relic;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * 遗物获取界面：屏幕中央浮出一张卡片显示遗物信息，卡片下方是「拾取 / 丢弃」两个按钮。
 * 玩家做出选择后卡片缩小并飞向屏幕左上角，然后消失。
 *
 * <p><b>⚠ 遗物只在点了「拾取」之后才真正生效。</b> 所以调用方必须先「只挑不拿」
 * （{@link com.example.demo.character.RelicFun#pickEliteRelic} 等），
 * 再在本类的回调里用 {@link com.example.demo.character.RelicFun#grantRelic} 入账
 * —— 回调参数为 true 才入账。否则「丢弃」就成了先加后减：遗物栏、最大生命值、
 * 甚至请假条的计次都会被污染，减不干净。</p>
 *
 * <p>卡片是普通节点，不是 {@code Alert} —— 从动画回调里调起来也不会抛
 * {@code showAndWait is not allowed during animation or layout processing}。</p>
 */
public final class RelicObtainToast {
    private RelicObtainToast() {}

    /** 卡片宽度 */
    private static final double CARD_W = 320;
    /** 卡片高度 */
    private static final double CARD_H = 400;
    /** 遗物图标尺寸 */
    private static final double ICON_SIZE = 96;
    /** 按钮尺寸 */
    private static final double BTN_W = 118;
    private static final double BTN_H = 40;
    /** 飞行动画时间（毫秒） */
    private static final double FLY_MS = 600;

    // 样式一律走 inline —— 主题 CSS 里的 .button 会盖掉 setStyle 之外的设置
    private static final String TAKE_STYLE =
            "-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 15px; "
                    + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;";
    private static final String TAKE_HOVER =
            "-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-size: 15px; "
                    + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;";
    private static final String DROP_STYLE =
            "-fx-background-color: #475569; -fx-text-fill: #e2e8f0; -fx-font-size: 15px; "
                    + "-fx-background-radius: 10; -fx-cursor: hand;";
    private static final String DROP_HOVER =
            "-fx-background-color: #64748b; -fx-text-fill: #f1f5f9; -fx-font-size: 15px; "
                    + "-fx-background-radius: 10; -fx-cursor: hand;";

    /**
     * 显示遗物获取界面，让玩家选「拾取 / 丢弃」。
     *
     * @param scene     当前场景；卡片挂到 {@code scene.getRoot()} 顶层，
     *                  并压一层半透明遮罩挡住底下的点击
     * @param relic     要展示的遗物
     * @param onDecided 回调，参数 true = 拾取，false = 丢弃；可传 null
     */
    public static void showChoice(Scene scene, Relic relic, Consumer<Boolean> onDecided) {
        if (scene == null || relic == null || !(scene.getRoot() instanceof Pane root)) {
            // 兜底：没有能挂载的场景就直接当成「拾取」，免得玩家白丢一件遗物
            safeRun(onDecided, true);
            return;
        }

        // 遮罩：铺满整页挡住底下的点击。
        // 没有它的话，玩家能在做选择之前点地图节点跑掉，这件遗物就永远悬着了。
        // （Pane 作为 StackPane 的子节点会被拉到满尺寸，正好当全屏遮罩用。）
        Pane scrim = new Pane();
        scrim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.55);");
        scrim.setOnMousePressed(e -> e.consume());
        scrim.setOnMouseClicked(e -> e.consume());
        scrim.setOpacity(0); // 透明不影响拦截点击，所以第一帧起就挡住了

        // 按钮先建出来，动作等 decide 定义好再接上
        Button takeBtn = optionButton("拾取", TAKE_STYLE, TAKE_HOVER);
        Button dropBtn = optionButton("丢弃", DROP_STYLE, DROP_HOVER);

        // 构建卡片（卡片必须能接鼠标，所以这里不能 setMouseTransparent）
        StackPane card = buildCard(relic, takeBtn, dropBtn);
        card.setManaged(false);

        // 初始状态：透明 + 缩小
        card.setOpacity(0);
        card.setScaleX(0.3);
        card.setScaleY(0.3);

        // 居中定位：卡片是 unmanaged 的，位置全靠 relocate
        double sceneW = scene.getWidth();
        double sceneH = scene.getHeight();
        Point2D local = root.sceneToLocal(new Point2D(sceneW / 2, sceneH / 2));
        card.resize(CARD_W, CARD_H);
        card.relocate(local.getX() - CARD_W / 2, local.getY() - CARD_H / 2);

        root.getChildren().addAll(scrim, card);

        // 1) 淡入 + 放大到正常尺寸（遮罩一起淡入）
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), card);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), card);
        scaleIn.setFromX(0.3);
        scaleIn.setFromY(0.3);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition scrimIn = new FadeTransition(Duration.millis(400), scrim);
        scrimIn.setFromValue(0);
        scrimIn.setToValue(1);

        new ParallelTransition(fadeIn, scaleIn, scrimIn).play();

        // 2) 做出选择后：缩小 + 飞向左上角 + 淡出（遮罩同时淡出）
        ScaleTransition shrink = new ScaleTransition(Duration.millis(FLY_MS), card);
        shrink.setFromX(1);
        shrink.setFromY(1);
        shrink.setToX(0.15);
        shrink.setToY(0.15);
        shrink.setInterpolator(Interpolator.EASE_IN);

        // 飞向场景左上角
        Point2D targetLocal = root.sceneToLocal(new Point2D(60, 60));
        double currentX = local.getX() - CARD_W / 2;
        double currentY = local.getY() - CARD_H / 2;
        double deltaX = targetLocal.getX() - CARD_W * 0.15 / 2 - currentX;
        double deltaY = targetLocal.getY() - CARD_H * 0.15 / 2 - currentY;

        TranslateTransition fly = new TranslateTransition(Duration.millis(FLY_MS), card);
        fly.setFromX(0);
        fly.setFromY(0);
        fly.setToX(deltaX);
        fly.setToY(deltaY);
        fly.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(FLY_MS), card);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        FadeTransition scrimOut = new FadeTransition(Duration.millis(FLY_MS), scrim);
        scrimOut.setFromValue(1);
        scrimOut.setToValue(0);

        ParallelTransition flyAway = new ParallelTransition(shrink, fly, fadeOut, scrimOut);

        // 防连点：只认第一次选择，且演出播完才回调（此时才真正入账）
        final boolean[] decided = {false};
        Consumer<Boolean> decide = taken -> {
            if (decided[0]) return;
            decided[0] = true;
            takeBtn.setDisable(true);
            dropBtn.setDisable(true);
            flyAway.setOnFinished(e -> {
                root.getChildren().removeAll(scrim, card);
                safeRun(onDecided, taken);
            });
            flyAway.play();
        };
        takeBtn.setOnAction(e -> decide.accept(true));
        dropBtn.setOnAction(e -> decide.accept(false));
    }

    /** 构建遗物展示卡片（含「拾取 / 丢弃」按钮） */
    private static StackPane buildCard(Relic relic, Button takeBtn, Button dropBtn) {
        // 遗物图标
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(ICON_SIZE, ICON_SIZE);
        iconBox.setMaxSize(ICON_SIZE, ICON_SIZE);

        Image img = relic.loadImage();
        if (img != null) {
            iconBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 18;");
            ImageView iv = new ImageView(img);
            iv.setFitWidth(ICON_SIZE);
            iv.setFitHeight(ICON_SIZE);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            iconBox.getChildren().add(iv);
        } else {
            iconBox.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 18;");
            Label g = new Label(relic.name.substring(0, 1));
            g.setTextFill(Color.WHITE);
            g.setFont(Font.font(44));
            g.setStyle("-fx-font-weight: bold;");
            iconBox.getChildren().add(g);
        }

        // 遗物名称
        Label nameLabel = new Label(relic.name);
        nameLabel.setTextFill(Color.rgb(251, 191, 36));
        nameLabel.setFont(Font.font(22));
        nameLabel.setStyle("-fx-font-weight: bold;");

        // 遗物描述
        Label descLabel = new Label(relic.desc);
        descLabel.setTextFill(Color.rgb(203, 213, 225));
        descLabel.setFont(Font.font(14));
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(CARD_W - 56);
        descLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 提示文字：说清这一步要做选择，而且丢掉就没了
        Label hint = new Label("发现遗物 · 拾取后才会生效");
        hint.setTextFill(Color.rgb(148, 163, 184));
        hint.setFont(Font.font(12));

        HBox buttons = new HBox(14, takeBtn, dropBtn);
        buttons.setAlignment(Pos.CENTER);
        // ⚠ StackPane 会把可伸缩的子节点拉满卡片，HBox 拉满后自身 CENTER 对齐
        //   会把按钮摆到卡片正中间。钉死 max 尺寸，按钮才老实待在 VBox 的流里。
        buttons.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(hint, iconBox, nameLabel, descLabel, buttons);
        content.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane card = new StackPane(content);
        card.setPrefSize(CARD_W, CARD_H);
        card.setMaxSize(CARD_W, CARD_H);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 20; "
                + "-fx-border-color: #f59e0b; -fx-border-radius: 20; -fx-border-width: 2;");
        card.setEffect(new javafx.scene.effect.DropShadow(20, Color.rgb(245, 158, 11, 0.4)));

        return card;
    }

    /** 建一个带 hover 高亮的按钮 */
    private static Button optionButton(String text, String normal, String hover) {
        Button b = new Button(text);
        b.setPrefSize(BTN_W, BTN_H);
        b.setMinSize(BTN_W, BTN_H);
        b.setMaxSize(BTN_W, BTN_H);
        b.setStyle(normal);
        b.setOnMouseEntered(e -> {
            if (!b.isDisabled()) b.setStyle(hover);
        });
        b.setOnMouseExited(e -> {
            if (!b.isDisabled()) b.setStyle(normal);
        });
        return b;
    }

    /** 回调一律丢到下一帧执行，避免跑在动画调用栈里 */
    private static void safeRun(Consumer<Boolean> cb, boolean taken) {
        if (cb != null) Platform.runLater(() -> cb.accept(taken));
    }
}
