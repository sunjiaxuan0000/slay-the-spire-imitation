package com.example.demo.operator;

import com.example.demo.battle.BattleView;
import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;
import com.example.demo.character.Player;
import com.example.demo.character.Relic;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;
import java.util.function.Consumer;

/**
 * 开发者模式面板（operator 包：所有“开发者模式”专属的界面逻辑都在这里）。
 *
 * 三个页签，每个页签内部都是「整块可滚动」的：
 *   牌组 —— 上半部分是当前牌组（点卡片右上角「×」移除），下半部分点一下就加一张
 *   手牌 —— 只有战斗中才有：同样能加能删（上限 10 张）
 *   遗物 —— 列出全部遗物，点一下就是“获得 / 移除”的开关
 *
 * 面板只改数据（player.deck / player.relics / battle.handCards()），
 * 改完调用 onRefresh 让 HUD 与战斗界面重画一次。
 */
public class DevPanel extends VBox {

    private static final double FACE_W = 96;   // 面板里卡面显示宽度
    private static final int HAND_LIMIT = 10;  // 和战斗里的手牌上限保持一致
    private static final double PANEL_W = 940;

    private final Player player;
    private final BattleView battle; // 非战斗场景时为 null
    private final Runnable onRefresh;
    private final Runnable onClose;  // 关面板（由外层清空整页窗口实现）

    private final VBox deckBox = new VBox(10);
    private final VBox handBox = new VBox(10);   // 仅战斗
    private final VBox relicBox = new VBox(10);
    private final TabPane tabs = new TabPane();

    public DevPanel(Player player, BattleView battle, Runnable onRefresh, Runnable onClose) {
        this.player = player;
        this.battle = battle;
        this.onRefresh = onRefresh;
        this.onClose = onClose;

        setAlignment(Pos.CENTER);
        setSpacing(10);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setStyle("-fx-background-color: rgba(30, 41, 59, 0.98); "
                + "-fx-background-radius: 16; -fx-border-color: #f59e0b; "
                + "-fx-border-radius: 16; -fx-border-width: 2;");
        setPadding(new Insets(14, 18, 12, 18));

        Label title = new Label("开发者模式");
        title.setTextFill(Color.rgb(253, 224, 71));
        title.setFont(Font.font(22));
        title.setStyle("-fx-font-weight: bold;");

        Label sub = new Label(battle == null
                ? "地图场景 · 可以直接点任意节点进入"
                : "战斗中 · 打开面板时战斗已暂停 · 改动立刻生效");
        sub.setTextFill(Color.rgb(148, 163, 184));
        sub.setFont(Font.font(12));

        VBox head = new VBox(2, title, sub);
        head.setAlignment(Pos.CENTER);

        // ---- 页签：一次只显示一块，免得面板比窗口还高 ----
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefWidth(PANEL_W);

        ScrollPane deckScroll = scroll(deckBox);
        ScrollPane relicScroll = scroll(relicBox);

        Tab deckTab = new Tab("牌组", deckScroll);
        Tab relicTab = new Tab("遗物", relicScroll);
        tabs.getTabs().add(deckTab);
        if (battle != null) {
            tabs.getTabs().add(new Tab("手牌", scroll(handBox)));
        }
        tabs.getTabs().add(relicTab);

        // 面板尺寸跟着窗口走：窗口再小也不会把内容/关闭按钮挤到屏幕外
        sceneProperty().addListener((ObservableValue<? extends javafx.scene.Scene> o,
                                     javafx.scene.Scene oldScene, javafx.scene.Scene newScene) -> {
            if (newScene == null || tabs.prefHeightProperty().isBound()) return;
            tabs.prefHeightProperty().bind(newScene.heightProperty().multiply(0.68));
            tabs.prefWidthProperty().bind(newScene.widthProperty().subtract(120));
            tabs.setMinSize(0, 0); // 面板本身也别被内容顶大
            for (Tab t : tabs.getTabs()) {
                if (t.getContent() instanceof ScrollPane sp) {
                    sp.prefWidthProperty().bind(newScene.widthProperty().subtract(160));
                }
            }
        });

        getChildren().addAll(head, tabs, closeButton());

        refresh();
    }

    // ================= 刷新（内容变了就重填这三块） =================

    /** 重填牌组 / 手牌 / 遗物三块内容 */
    public void refresh() {
        fillDeck();
        if (battle != null) fillHand();
        fillRelics();
    }

    private void fillDeck() {
        FlowPane own = new FlowPane(6, 6);
        for (Card c : List.copyOf(player.deck)) {
            own.getChildren().add(removableCard(c, "牌组", () -> {
                player.deck.remove(c);
                if (battle != null) battle.removeCardFromBattle(c); // 战斗里也清掉
                afterChange();
            }));
        }
        if (player.deck.isEmpty()) own.getChildren().add(hint("牌组是空的"));

        // 当前牌组：有多高就占多高（整块内容可滚动，不再被压成一条缝）
        own.setMinHeight(FACE_W * 1.6);

        deckBox.getChildren().setAll(
                sectionLabel("当前牌组 · 共 " + player.deck.size() + " 张（点卡片右上角「×」移除）"),
                own,

                sectionLabel("加入牌组 · 点一下加一张（共 " + Card.Kind.values().length + " 种牌）"),
                allCards(this::addToDeck));
    }

    private void fillHand() {
        List<Card> hand = battle.handCards();

        FlowPane own = new FlowPane(6, 6);
        for (Card c : List.copyOf(hand)) {
            own.getChildren().add(removableCard(c, "手牌", () -> {
                hand.remove(c);
                afterChange();
            }));
        }
        if (hand.isEmpty()) own.getChildren().add(hint("手牌是空的"));
        own.setMinHeight(FACE_W * 1.6);

        handBox.getChildren().setAll(
                sectionLabel("当前手牌 · 共 " + hand.size() + " 张（上限 " + HAND_LIMIT + " 张）"),
                own,

                sectionLabel("加入手牌 · 点一下加一张"),
                allCards(this::addToHand));
    }

    private void fillRelics() {
        FlowPane row = new FlowPane(8, 8);
        for (Relic r : Relic.pool()) {
            row.getChildren().add(relicChip(r, hasRelic(r.name)));
        }

        relicBox.getChildren().setAll(
                sectionLabel("遗物 · 持有 " + player.relics.size() + " 个（点一下获得 / 移除）"),
                row,
                hint("提示：遗物效果在战斗开始 / 回合开始时结算，改完下一场战斗完全生效"));
    }

    private boolean hasRelic(String name) {
        return player.relics.stream().anyMatch(h -> h.name.equals(name));
    }

    // ================= 改数据的入口（顺手把界面刷新了） =================

    private void addToDeck(Card c) {
        player.deck.add(c);
        if (battle != null) battle.addToDrawPile(c); // 战斗中：本场也能抽到
        afterChange();
    }

    private void addToHand(Card c) {
        if (battle == null) return;
        List<Card> hand = battle.handCards();
        if (hand.size() >= HAND_LIMIT) return; // 和抽牌一样，不超过上限
        hand.add(c);
        afterChange();
    }

    /** 改完数据后统一收尾：重画面板 + 通知外层刷新 HUD / 战斗界面 */
    private void afterChange() {
        refresh();
        onRefresh.run();
    }

    // ================= 卡面小构件 =================

    /** 卡面 + 右上角「×」 */
    private static StackPane removableCard(Card c, String where, Runnable onRemove) {
        StackPane face = CardFaceView.buildAt(c, FACE_W);

        StackPane wrap = new StackPane(face);
        wrap.setPadding(new Insets(0, 4, 6, 0));

        Button del = new Button("×");
        del.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; "
                + "-fx-background-radius: 10; -fx-font-size: 13; -fx-font-weight: bold; "
                + "-fx-padding: 1 7 1 7; -fx-cursor: hand;");
        del.setTooltip(new Tooltip("从" + where + "移除：" + c.kind.label));

        wrap.getChildren().add(del);
        StackPane.setAlignment(del, Pos.TOP_RIGHT);
        del.setOnAction(e -> onRemove.run());
        return wrap;
    }

    /** “全部卡牌”里的一张可点小卡面 */
    private static StackPane addableCard(Card c, Runnable onAdd) {
        StackPane face = CardFaceView.buildAt(c, FACE_W);
        face.setCursor(javafx.scene.Cursor.HAND);
        Tooltip.install(face, new Tooltip("点击加入：" + c.kind.label + "\n" + c.kind.desc));
        face.setOnMouseClicked(e -> onAdd.run());
        return face;
    }

    /** 全部卡牌种类：点一张加一张（牌组 / 手牌两处共用） */
    private FlowPane allCards(Consumer<Card> onAdd) {
        FlowPane row = new FlowPane(6, 6);
        for (Card.Kind kind : Card.Kind.values()) {
            row.getChildren().add(addableCard(Card.of(kind), () -> onAdd.accept(Card.of(kind))));
        }
        return row;
    }

    private StackPane relicChip(Relic r, boolean has) {
        Label glyph = new Label(r.name.substring(0, 1));
        glyph.setTextFill(Color.WHITE);
        glyph.setFont(Font.font(18));
        glyph.setStyle("-fx-font-weight: bold;");

        Label name = new Label(r.name + (has ? " · 已持有" : " · 未持有"));
        name.setTextFill(has ? Color.rgb(134, 239, 172) : Color.rgb(203, 213, 225));
        name.setFont(Font.font(13));

        HBox inner = new HBox(8, glyph, name);
        inner.setAlignment(Pos.CENTER_LEFT);

        StackPane chip = new StackPane(inner);
        chip.setPadding(new Insets(6, 14, 6, 10));
        chip.setCursor(javafx.scene.Cursor.HAND);
        chip.setStyle("-fx-background-color: " + (has ? "#15803d" : "#334155")
                + "; -fx-background-radius: 10;");
        Tooltip.install(chip, new Tooltip(r.name + "\n" + r.desc + "\n\n点击" + (has ? "移除" : "获得")));

        chip.setOnMouseClicked(e -> {
            if (has) {
                player.relics.removeIf(h -> h.name.equals(r.name));
            } else {
                player.addRelic(new Relic(r.name, r.desc));
            }
            afterChange();
        });
        return chip;
    }

    // ================= 杂项 =================

    /**
     * 每个页签的整块内容都放进一个 ScrollPane：
     *   - fitToWidth=true → 卡片按视口宽度自动换行，不会横向溢出
     *   - minSize(0,0)    → 允许被窗口压小（不压小的话内容会把面板顶出屏幕，
     *                       那正是之前“滑不动 / 够不到”的根源），压小后滚动条接管
     *   - 滚轮 / 按住拖动 / 拖滚动条 三种方式都能滚
     */
    private static ScrollPane scroll(VBox content) {
        content.setPadding(new Insets(12, 14, 14, 12));
        content.setFillWidth(true);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);      // 关键：让 FlowPane 跟着视口宽度换行
        sp.setPannable(true);        // 允许按住拖动滚动
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setPrefWidth(PANEL_W);
        sp.setMinSize(0, 0);         // 关键：允许被压缩，避免把面板撑出窗口
        sp.setStyle("-fx-background: #0f172a; -fx-background-color: #0f172a;");
        return sp;
    }

    private Button closeButton() {
        Button close = new Button("关闭面板（Esc）");
        close.setFont(Font.font(15));
        close.setPrefSize(200, 38);
        close.setStyle("-fx-background-color: #475569; -fx-text-fill: white; "
                + "-fx-background-radius: 10; -fx-cursor: hand;");
        close.setOnAction(e -> onClose.run());
        return close;
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.WHITE);
        l.setFont(Font.font(15));
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private static Label hint(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.rgb(148, 163, 184));
        l.setFont(Font.font(12));
        return l;
    }
}
