package com.example.demo.view;

import com.example.demo.character.Relic;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * 遗物获取提示卡片：屏幕中央浮出一张卡片显示遗物信息，
 * 约 3 秒后缩小并飞向屏幕左上角，然后消失。
 *
 * <p>设计思路：与 {@link CardFlyFx} 类似，ghost 卡片挂在场景根上，
 * 不影响任何数据，只做视觉演出。演出结束（或跳过）后通过 onDone 回调
 * 通知调用方继续后续流程。</p>
 */
public final class RelicObtainToast {
    private RelicObtainToast() {}

    /** 卡片宽度 */
    private static final double CARD_W = 280;
    /** 卡片高度 */
    private static final double CARD_H = 340;
    /** 遗物图标尺寸 */
    private static final double ICON_SIZE = 96;
    /** 展示停留时间（毫秒） */
    private static final double SHOW_MS = 3000;
    /** 飞行动画时间（毫秒） */
    private static final double FLY_MS = 600;

    /**
     * 显示遗物获取提示卡片。
     *
     * @param scene  当前场景（ghost 会挂到 scene.getRoot() 顶层）
     * @param relic  要展示的遗物
     * @param onDone 演出结束后的回调，可传 null
     */
    public static void show(Scene scene, Relic relic, Runnable onDone) {
        if (scene == null || scene.getRoot() == null) {
            safeRun(onDone);
            return;
        }
        if (!(scene.getRoot() instanceof Pane root)) {
            safeRun(onDone);
            return;
        }

        // 构建卡片
        StackPane card = buildCard(relic);
        card.setMouseTransparent(true);
        card.setManaged(false);

        // 初始状态：透明 + 缩小
        card.setOpacity(0);
        card.setScaleX(0.3);
        card.setScaleY(0.3);

        // 计算卡片在场景中心的位置
        double sceneW = scene.getWidth();
        double sceneH = scene.getHeight();
        Point2D center = new Point2D(sceneW / 2, sceneH / 2);
        Point2D local = root.sceneToLocal(center);
        card.resize(CARD_W, CARD_H);
        card.relocate(local.getX() - CARD_W / 2, local.getY() - CARD_H / 2);

        root.getChildren().add(card);

        // 1) 淡入 + 放大到正常尺寸
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), card);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), card);
        scaleIn.setFromX(0.3);
        scaleIn.setFromY(0.3);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition appear = new ParallelTransition(fadeIn, scaleIn);

        // 2) 停留展示
        PauseTransition hold = new PauseTransition(Duration.millis(SHOW_MS));

        // 3) 缩小 + 飞向左上角 + 淡出
        ScaleTransition shrink = new ScaleTransition(Duration.millis(FLY_MS), card);
        shrink.setFromX(1);
        shrink.setFromY(1);
        shrink.setToX(0.15);
        shrink.setToY(0.15);
        shrink.setInterpolator(Interpolator.EASE_IN);

        // 飞向场景左上角
        Point2D target = new Point2D(60, 60);
        Point2D targetLocal = root.sceneToLocal(target);
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

        ParallelTransition flyAway = new ParallelTransition(shrink, fly, fadeOut);
        flyAway.setOnFinished(e -> {
            root.getChildren().remove(card);
            safeRun(onDone);
        });

        // 按顺序播放：出现 → 停留 → 飞走
        SequentialTransition seq = new SequentialTransition(appear, hold, flyAway);
        seq.play();
    }

    /** 构建遗物展示卡片 */
    private static StackPane buildCard(Relic relic) {
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
        descLabel.setMaxWidth(CARD_W - 40);
        descLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 提示文字
        Label hint = new Label("获得遗物");
        hint.setTextFill(Color.rgb(148, 163, 184));
        hint.setFont(Font.font(12));

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(hint, iconBox, nameLabel, descLabel);

        StackPane card = new StackPane(content);
        card.setPrefSize(CARD_W, CARD_H);
        card.setMaxSize(CARD_W, CARD_H);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 20; "
                + "-fx-border-color: #f59e0b; -fx-border-radius: 20; -fx-border-width: 2;");
        card.setEffect(new javafx.scene.effect.DropShadow(20, Color.rgb(245, 158, 11, 0.4)));

        return card;
    }

    /** 回调一律丢到下一帧执行，避免跑在动画调用栈里 */
    private static void safeRun(Runnable r) {
        if (r != null) Platform.runLater(r);
    }
}
