package com.example.demo.view;

import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;
import com.example.demo.character.Player;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 「从牌组里选一张牌」的整页窗口 —— <b>删牌 / 强化共用</b>。
 *
 * <p>版面与 {@code HelloApplication.deckPage}（牌组页）完全一致：深色面板 {@code #1e293b} +
 * 24px 白色粗体标题 + 灰色副标题 + 900×360 卡面滚动区
 * （{@link CardFaceView#buildAt} 分层贴图卡面，宽度 120，按牌 id 排序）+ 关闭按钮。
 * 与牌组页唯一的区别：这里的卡面<b>可以点</b>。
 *
 * <p>目前的使用者：
 * <ul>
 *   <li>起点房间的「破镜」遗物 —— {@link RoomView#removeCardFromDeck()}</li>
 *   <li>Boss 遗物的「空鸟笼」—— {@code BattleView.showRemovePicker()}</li>
 *   <li>篝火节点的「强化卡牌」—— {@link RestView}</li>
 * </ul>
 *
 * <p>用法：{@code parent.getChildren().add(picker)}（父容器须为 {@link Pane}）+ {@link #show()}。
 * 组件会把自己铺满父容器，选牌或取消后<b>自动把自己摘掉</b>。
 */
public class DeckPickOverlay extends StackPane {

    /** 面板宽度：与牌组页 deckPage 保持一致 */
    private static final double PANEL_W = 900;
    /** 卡面滚动区高度：与牌组页 deckPage 保持一致 */
    private static final double PANEL_H = 360;
    /** 卡面宽度：与牌组页 deckPage 保持一致 */
    private static final double CARD_W = 120;

    /** 默认副标题（删牌用） */
    private static final String DEFAULT_SUB =
            "点击卡牌将其从牌组移除 · 按牌 id 排列 · 点遮罩或「取消」跳过";

    private static final String CARD_BTN_STYLE =
            "-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;";
    /** 悬停时给卡面套一圈金色描边 */
    private static final String CARD_BTN_HOVER =
            "-fx-background-color: rgba(251, 191, 36, 0.14); -fx-background-radius: 10; "
            + "-fx-border-color: #fbbf24; -fx-border-width: 3; -fx-border-radius: 10; "
            + "-fx-padding: 0; -fx-cursor: hand;";
    /**
     * 不可选的卡（已强化 / 状态牌）：压暗且不响应点击。
     * <p>用 inline {@code -fx-opacity} 钉死 —— 只靠 CSS 的话没有效果，
     * 而 {@code setDisable(true)} 会连 Tooltip 一起废掉（禁用的节点收不到鼠标事件）。
     */
    private static final String CARD_BTN_LOCKED =
            "-fx-background-color: transparent; -fx-padding: 0; -fx-opacity: 0.38;";

    private final Player player;
    private final String titleText;
    private final String subText;
    /** null 表示用玩家当前牌组 */
    private final List<Card> cards;
    private final Predicate<Card> selectable;
    private final Consumer<Card> onPick;
    /** 「取消 / 点遮罩」时的回调，可为 null（战斗里传「跳过剩余删牌」的续接逻辑） */
    private final Runnable onCancel;
    /** 悬停提示，可为 null */
    private final Function<Card, String> tooltipFor;

    private final Label title = new Label();
    private final FlowPane cardGrid = new FlowPane(8, 8);
    private final ScrollPane scroll = new ScrollPane(cardGrid);

    /** 防止「点牌 / 点遮罩 / 点取消」重复触发回调 */
    private boolean resolved = false;

    /**
     * 删牌（起点房间「破镜」用）。
     *
     * @param onPick 选中某张牌时的回调；调用方负责真正把它从 {@code player.deck} 里删掉
     */
    public DeckPickOverlay(Player player, String titleText, Consumer<Card> onPick) {
        this(player, titleText, onPick, null);
    }

    /**
     * 删牌（Boss 遗物「空鸟笼」用）。
     *
     * @param onCancel 「取消」时的回调；战斗里必须传「跳过剩余删牌、继续流程」，
     *                 否则玩家会卡在战斗界面出不来
     */
    public DeckPickOverlay(Player player, String titleText, Consumer<Card> onPick, Runnable onCancel) {
        this(player, titleText, DEFAULT_SUB, null, c -> true, onPick, onCancel, null);
    }

    /**
     * 通用版（篝火「强化卡牌」用）。
     *
     * @param subText    副标题
     * @param cards      要展示的牌，{@code null} 表示玩家当前牌组
     * @param selectable 哪些牌可以点；不满足的会压暗并忽略点击
     * @param tooltipFor 每张牌的悬停提示，可为 {@code null}
     */
    public DeckPickOverlay(Player player, String titleText, String subText, List<Card> cards,
                           Predicate<Card> selectable, Consumer<Card> onPick,
                           Runnable onCancel, Function<Card, String> tooltipFor) {
        this.player = player;
        this.titleText = titleText;
        this.subText = subText;
        this.cards = cards;
        this.selectable = selectable == null ? c -> true : selectable;
        this.onPick = onPick;
        this.onCancel = onCancel;
        this.tooltipFor = tooltipFor;
        buildChrome();
    }

    // ================= 版面（照搬牌组页） =================

    private void buildChrome() {
        Pane dim = new Pane();
        dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.72);");
        dim.setOnMouseClicked(e -> cancel()); // 点遮罩 = 取消

        cardGrid.setPrefWrapLength(PANEL_W);

        scroll.setPrefSize(PANEL_W, PANEL_H);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: #0f172a; -fx-background-color: #0f172a;");

        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(24));
        title.setStyle("-fx-font-weight: bold;");

        Label sub = new Label(subText);
        sub.setTextFill(Color.rgb(148, 163, 184));
        sub.setFont(Font.font(13));

        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 16; -fx-padding: 18;");
        panel.getChildren().addAll(title, sub, scroll, closeButton());

        setAlignment(panel, Pos.CENTER);
        getChildren().addAll(dim, panel);
        setVisible(false);
    }

    /** 关闭按钮：与牌组页的「关闭」同款，只是文案改成「取消」 */
    private Button closeButton() {
        Button close = new Button("取消");
        close.setFont(Font.font(14));
        close.setPrefSize(110, 34);
        close.setStyle("-fx-background-color: #475569; -fx-text-fill: white; "
                + "-fx-background-radius: 9; -fx-cursor: hand;");
        close.setOnAction(e -> cancel());
        return close;
    }

    // ================= 展示 =================

    /** 铺满父容器并刷新卡面（每次展示都按当前牌组重建，牌组变了也不会残留旧卡） */
    public void show() {
        resolved = false;

        List<Card> list = new ArrayList<>(cards != null ? cards : player.deck);
        list.sort(Comparator.comparingInt(c -> c.id));

        title.setText(titleText + " · 共 " + list.size() + " 张");

        cardGrid.getChildren().clear();
        for (Card c : list) {
            cardGrid.getChildren().add(cardButton(c));
        }

        setVisible(true);
    }

    /** 一张卡面：图形与奖励弹层一致，外面套一层透明按钮承接点击与悬停高亮 */
    private Button cardButton(Card c) {
        Button b = new Button();
        b.setGraphic(CardFaceView.buildAt(c, CARD_W)); // 分层贴图卡面

        if (tooltipFor != null) {
            String tip = tooltipFor.apply(c);
            if (tip != null) Tooltip.install(b, new Tooltip(tip));
        }

        if (!selectable.test(c)) {
            // 不可选：压暗、不响应点击（也不给悬停高亮）
            b.setStyle(CARD_BTN_LOCKED);
            b.setOnAction(e -> { });
            return b;
        }

        b.setStyle(CARD_BTN_STYLE);
        b.setOnMouseEntered(e -> b.setStyle(CARD_BTN_HOVER));
        b.setOnMouseExited(e -> b.setStyle(CARD_BTN_STYLE));
        b.setOnAction(e -> {
            if (resolved) return;
            resolved = true;
            onPick.accept(c);
            dismiss();
        });
        return b;
    }

    // ================= 收尾 =================

    /** 取消：不做任何选择（战斗里由调用方决定是否续接后续流程） */
    private void cancel() {
        if (resolved) return;
        resolved = true;
        dismiss();
        if (onCancel != null) onCancel.run();
    }

    /** 隐藏并把自己从父容器摘掉（父容器不是 Pane 时只隐藏，不影响功能） */
    private void dismiss() {
        setVisible(false);
        if (getParent() instanceof Pane p) {
            p.getChildren().remove(this);
        }
    }
}
