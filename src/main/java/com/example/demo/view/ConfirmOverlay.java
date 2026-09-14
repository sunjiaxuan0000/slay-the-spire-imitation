package com.example.demo.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * 场景内确认弹窗：铺满父容器的一层半透明遮罩 + 居中的一张卡片。
 *
 * <p><b>为什么不用 {@code Alert}：</b>本项目统一用「普通节点弹层」代替
 * {@code Alert.showAndWait()} —— 后者在动画 / 布局回调里调用会抛
 * {@code IllegalStateException}，而且原生窗口和游戏画风也不是一套。
 * 这里做成普通 {@link StackPane} 子节点，随便什么时候弹都行。</p>
 *
 * <p>用法（父容器必须是 {@link Pane}，本弹窗自己算尺寸）：</p>
 * <pre>{@code
 * new ConfirmOverlay("标题", "正文", "确定", "取消",
 *         () -> { ...点确定... }, () -> { ...点取消... }).show(parentPane);
 * }</pre>
 *
 * <p>遮罩会<b>吃掉点击</b>（{@link MouseEvent#consume()}），所以点空白处既不会误触
 * 下面的按钮，也不会被当成“校准用的点击”打印坐标；必须点两个按钮之一才会关闭。</p>
 */
public class ConfirmOverlay extends StackPane {

    private static final double CARD_W = 540;
    private static final double CARD_PAD = 28;
    private static final double BTN_W = 130;
    private static final double BTN_H = 42;

    private final Runnable onOk;
    private final Runnable onCancel;

    private Pane parent;
    private boolean resolved = false;
    private final VBox card;

    public ConfirmOverlay(String title, String message, String okText, String cancelText,
                          Runnable onOk, Runnable onCancel) {
        this.onOk = onOk;
        this.onCancel = onCancel;

        setStyle("-fx-background-color: rgba(2, 6, 23, 0.62);");
        setAlignment(Pos.CENTER);
        // 吃掉点击：别让点空白穿透到下面的按钮上
        setOnMouseClicked(MouseEvent::consume);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font(24));
        titleLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold;");

        Label msg = new Label(message);
        msg.setFont(Font.font(15));
        msg.setWrapText(true);
        msg.setLineSpacing(4);
        msg.setMaxWidth(CARD_W - CARD_PAD * 2);
        msg.setStyle("-fx-text-fill: #cbd5e1;");

        HBox buttons = new HBox(14, makeCancel(cancelText), makeOk(okText));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        card = new VBox(18, titleLabel, msg, buttons);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(CARD_PAD));
        card.setStyle("-fx-background-color: #1e293b;"
                + "-fx-border-color: #f87171; -fx-border-width: 2;"
                + "-fx-background-radius: 12; -fx-border-radius: 12;");
        // ⚠ 必须钉死尺寸：StackPane 会把子节点拉满，VBox 的 maxHeight 又默认无限，
        //    不钉的话卡片会被拉成整屏，内部居中反而把内容甩到正中间。
        card.setMaxSize(CARD_W, Region.USE_PREF_SIZE);

        getChildren().add(card);
    }

    private Button makeOk(String text) {
        Button b = new Button(text);
        b.setPrefSize(BTN_W, BTN_H);
        b.setFont(Font.font(15));
        b.setCursor(Cursor.HAND);
        b.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: #fee2e2;"
                + "-fx-background-radius: 8;");
        b.setOnAction(e -> finish(onOk));
        return b;
    }

    private Button makeCancel(String text) {
        Button b = new Button(text);
        b.setPrefSize(BTN_W, BTN_H);
        b.setFont(Font.font(15));
        b.setCursor(Cursor.HAND);
        b.setStyle("-fx-background-color: #475569; -fx-text-fill: #e2e8f0;"
                + "-fx-background-radius: 8;");
        b.setOnAction(e -> finish(onCancel));
        return b;
    }

    /** 关掉弹窗并跑回调；{@code resolved} 保证一次点击只结算一次。 */
    private void finish(Runnable callback) {
        if (resolved) return;
        resolved = true;
        dismiss();
        if (callback != null) callback.run();
    }

    /**
     * 挂到 {@code parent} 上并铺满它。
     *
     * <p>父容器是 {@link Pane}（不做自动布局），所以尺寸得自己跟着父容器改 ——
     * 这里监听父容器的宽高然后 {@code resizeRelocate}。</p>
     */
    public void show(Pane parent) {
        this.parent = parent;
        parent.getChildren().add(this);
        fit();
        parent.widthProperty().addListener(o -> fit());
        parent.heightProperty().addListener(o -> fit());
    }

    /** 把自己从父容器上摘掉（外部想提前关掉也能调）。 */
    public void dismiss() {
        if (parent != null) {
            parent.getChildren().remove(this);
        }
        parent = null;
    }

    private void fit() {
        if (parent == null) return;
        resizeRelocate(0, 0, parent.getWidth(), parent.getHeight());
    }

    /** 这个弹窗当前是不是正挂在某个容器上。 */
    public boolean isShowing() {
        return parent != null && !resolved;
    }

    /** 居中的那张卡片（调试 / 截图 / 量尺寸用）。 */
    public VBox card() {
        return card;
    }
}
