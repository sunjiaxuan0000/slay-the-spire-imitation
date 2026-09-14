package com.example.demo.view;

import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 通用「获得卡牌 → 飞入牌组」演出。
 *
 * 设计跟战斗里的抽牌飞入一致：数据由调用方照常即时结算（先 player.deck.add），
 * 这里只复制一张只读 ghost 卡面挂到场景根上，从 source 飞到 deckTarget，
 * 落位后缩进牌组并消失；同时让牌组图标轻轻“跳”一下。演出不会影响任何数据，
 * 只要坐标取不到（过早重画 / 节点还没挂上场景 / 场景根不是 Pane）就跳过演出直接回调。
 *
 * ★ onDone 一律延迟到下一帧执行（见 {@link #safeRun}），因此回调里可以安全地
 *   弹模态框、切换场景，不用担心踩到 JavaFX 的动画/布局限制。
 */
public final class CardFlyFx {
    private CardFlyFx() {}

    /** 飞入牌组的单张卡面宽度（与牌组页一致）。 */
    private static final double FLY_FACE_W = 120;
    /** 飞行时长（毫秒）。 */
    private static final double FLY_MS = 420;

    /**
     * 让获得的卡从 source 飞进 deckTarget（牌组图标）。
     *
     * @param scene       当前场景（ghost 会挂到 scene.getRoot() 顶层）
     * @param source      起点节点（比如被点的奖励卡按钮），为 null 时直接结束
     * @param deckTarget  终点节点（牌组图标），为 null 时直接结束
     * @param card        要飞的那张牌
     * @param onDone      演出结束（或跳过）后的回调，可传 null
     */
    public static void flyIntoDeck(Scene scene, Node source, Node deckTarget,
                                   Card card, Runnable onDone) {
        if (scene == null || scene.getRoot() == null || deckTarget == null) {
            safeRun(onDone);
            return;
        }
        // ghost 要挂在场景根上才能盖住一切；本作场景根都是 StackPane，
        // 万一以后换成别的容器（不是 Pane），宁可不演也不能抛 ClassCastException。
        if (!(scene.getRoot() instanceof Pane root)) {
            safeRun(onDone);
            return;
        }
        Point2D from = centerInScene(source);
        Point2D to = centerInScene(deckTarget);
        if (from == null || to == null) {
            safeRun(onDone);
            return;
        }
        // 统一换算到场景根局部坐标（ghost 要挂到根上）
        Point2D fromLocal = root.sceneToLocal(from);
        Point2D toLocal = root.sceneToLocal(to);

        double w = FLY_FACE_W, h = w * 1.4;
        StackPane ghost = CardFaceView.buildAt(card, w);
        ghost.setMouseTransparent(true);
        ghost.setManaged(false);
        ghost.resize(w, h);
        ghost.relocate(fromLocal.getX() - w / 2, fromLocal.getY() - h / 2);
        root.getChildren().add(ghost);

        TranslateTransition move = new TranslateTransition(Duration.millis(FLY_MS), ghost);
        move.setToX(toLocal.getX() - fromLocal.getX());
        move.setToY(toLocal.getY() - fromLocal.getY());
        move.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition shrink = new ScaleTransition(Duration.millis(FLY_MS), ghost);
        shrink.setToX(0.2);
        shrink.setToY(0.2);

        FadeTransition fade = new FadeTransition(Duration.millis(FLY_MS), ghost);
        fade.setFromValue(1);
        fade.setToValue(0);

        ParallelTransition fly = new ParallelTransition(move, shrink, fade);
        fly.setOnFinished(e -> {
            root.getChildren().remove(ghost);
            popDeck(deckTarget);
            safeRun(onDone);
        });
        fly.play();
    }

    /** 牌组图标轻“跳”一下，强化“收到牌了”的反馈。 */
    private static void popDeck(Node deckTarget) {
        ScaleTransition pop = new ScaleTransition(Duration.millis(180), deckTarget);
        pop.setFromX(1.25);
        pop.setFromY(1.25);
        pop.setToX(1);
        pop.setToY(1);
        pop.setInterpolator(Interpolator.EASE_OUT);
        pop.play();
    }

    /** 节点中心 → 场景坐标；节点未挂上场景时返回 null。 */
    private static Point2D centerInScene(Node n) {
        if (n == null || n.getScene() == null) return null;
        Bounds b = n.localToScene(n.getLayoutBounds());
        if (b == null) return null;
        return new Point2D(b.getMinX() + b.getWidth() / 2, b.getMinY() + b.getHeight() / 2);
    }

    /**
     * 回调一律丢到下一帧执行，保证它**不会**跑在动画/布局的调用栈里。
     *
     * 原因：JavaFX 的 Stage.showAndWait() 明确禁止在动画或布局处理过程中调用，
     * 否则抛 IllegalStateException（"showAndWait is not allowed during animation
     * or layout processing"）。而这个工具类的 onDone 常常要弹弹窗、切场景
     * （比如精英战胜利后要先弹「精英战利品」再回地图），所以这里统一延迟一帧，
     * 让调用方不必关心自己身处哪个回调栈。
     */
    private static void safeRun(Runnable r) {
        if (r != null) Platform.runLater(r);
    }
}
