package com.example.demo.view;

import com.example.demo.card.Card;
import com.example.demo.character.Player;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * 篝火（休息）节点页面。版式仿 {@link RoomView}：背景铺满 + 中间两个选项 +
 * 选完出现「离开篝火」。
 *
 * <p>两个选项<b>二选一</b>（和起点房间「三选一遗物」同理，选完另一个压暗）：
 * <ul>
 *   <li><b>休息</b> —— 恢复 30% 最大生命值，并置 {@code player.restedAtCampfire}
 *       （古茶具套装的「篝火休息后下一场战斗 +2 能量」靠它判断）</li>
 *   <li><b>强化卡牌</b> —— 打开 {@link DeckPickOverlay} 选一张牌升级
 *       （{@link Card#upgrade()}，已强化的牌与状态牌会压暗不可选）</li>
 * </ul>
 */
public class RestView extends StackPane {

    /** 背景图：src/main/resources/com/example/demo/rest.png */
    private static final String BG = "/com/example/demo/rest.png";

    /** 休息恢复的最大生命值比例 */
    private static final double HEAL_RATIO = 0.30;

    private static final double OPTION_W = 348;
    private static final double OPTION_H = 128;

    /** 底部提示条的宽度（与 RoomView 的 CONTENT_W 一致） */
    private static final double CONTENT_W = 880;

    /**
     * 两个选项距页面底部的距离。
     * <p>art 的主体（猪 + 篝火）正好落在画面正中间，选项放正中间会把它整个盖住，
     * 所以压到「底部提示条上方」。想让选项更靠上就调小这个值。
     */
    private static final double OPTIONS_BOTTOM_MARGIN = 92;

    private static final String OPTION_STYLE =
            "-fx-background-color: rgba(15, 23, 42, 0.86); -fx-background-radius: 14; "
            + "-fx-border-color: rgba(148, 163, 184, 0.45); -fx-border-width: 2; -fx-border-radius: 14; "
            + "-fx-padding: 14 20 14 20; -fx-cursor: hand;";
    private static final String OPTION_STYLE_HOVER =
            "-fx-background-color: rgba(30, 41, 59, 0.94); -fx-background-radius: 14; "
            + "-fx-border-color: rgba(251, 191, 36, 0.9); -fx-border-width: 2; -fx-border-radius: 14; "
            + "-fx-padding: 14 20 14 20; -fx-cursor: hand;";
    /** 选中的那个 */
    private static final String OPTION_STYLE_CHOSEN =
            "-fx-background-color: rgba(69, 26, 3, 0.92); -fx-background-radius: 14; "
            + "-fx-border-color: #fbbf24; -fx-border-width: 3; -fx-border-radius: 14; "
            + "-fx-padding: 14 20 14 20;";
    /** 没选的那个（以及不可用的选项）：压暗、不响应点击 */
    private static final String OPTION_STYLE_LOCKED =
            "-fx-background-color: rgba(15, 23, 42, 0.86); -fx-background-radius: 14; "
            + "-fx-border-color: rgba(148, 163, 184, 0.3); -fx-border-width: 2; -fx-border-radius: 14; "
            + "-fx-padding: 14 20 14 20; -fx-opacity: 0.4;";

    private final Player player;
    private final Runnable onLeave;

    private boolean chosen = false;
    private final List<Button> optionButtons = new ArrayList<>();

    private final Label hpLabel = new Label();
    private final Label hint = new Label("休息一下，或者强化一张牌，然后离开");
    private final Button leaveBtn = new Button("离开篝火");

    private Button restBtn;
    private Button upgradeBtn;

    public RestView(Player player, Runnable onLeave) {
        this.player = player;
        this.onLeave = onLeave;

        // ---- 背景：rest.png 等比铺满 ----
        Image bg = loadImage(BG);
        if (bg != null) {
            setBackground(cover(bg));
        } else {
            setStyle("-fx-background-color: linear-gradient(to bottom, #1c1917, #292524);");
        }

        // ---- 顶部：标题 + 当前生命 ----
        Label title = new Label("篝 火");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(30));
        title.setStyle("-fx-font-weight: bold;");

        hpLabel.setTextFill(Color.rgb(226, 232, 240));
        hpLabel.setFont(Font.font(15));
        refreshHpLabel();

        VBox header = new VBox(4, title, hpLabel);
        header.setAlignment(Pos.CENTER);
        // ⚠ StackPane 会把可伸缩的子节点拉满整页，而 VBox 内部又是 CENTER 对齐 ——
        // 于是内容会跑到正中间、TOP_CENTER / BOTTOM_CENTER 形同虚设。
        // 必须把 max 尺寸钉成 USE_PREF_SIZE，对齐约束才真正生效。
        header.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(header, Pos.TOP_CENTER);
        StackPane.setMargin(header, new Insets(30, 0, 0, 0));
        getChildren().add(header);

        // ---- 中间偏下：两个选项（二选一）----
        restBtn = buildOption("休息", healText(), this::doRest, true);
        upgradeBtn = buildOption("强化卡牌", upgradeText(), this::doUpgrade, hasUpgradable());

        HBox options = new HBox(26, restBtn, upgradeBtn);
        options.setAlignment(Pos.CENTER);
        options.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        // 放在底部提示条上方：这样猪和篝火（画面主体，正好在正中间）不会被挡住
        StackPane.setAlignment(options, Pos.BOTTOM_CENTER);
        StackPane.setMargin(options, new Insets(0, 20, OPTIONS_BOTTOM_MARGIN, 20));
        getChildren().add(options);

        // ---- 底部：提示 + 离开 ----
        hint.setTextFill(Color.rgb(226, 232, 240, 0.8));
        hint.setFont(Font.font(14));

        leaveBtn.setFont(Font.font(17));
        leaveBtn.setPrefSize(170, 46);
        leaveBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; "
                + "-fx-background-radius: 12; -fx-cursor: hand;");
        leaveBtn.setVisible(false);
        leaveBtn.setOnAction(e -> onLeave.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(16, hint, spacer, leaveBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setMaxWidth(CONTENT_W);
        footer.setMinWidth(CONTENT_W);
        footer.setMaxHeight(Region.USE_PREF_SIZE); // 同上：不钉住的话会被拉满整页、跑到正中间
        StackPane.setAlignment(footer, Pos.BOTTOM_CENTER);
        StackPane.setMargin(footer, new Insets(0, 20, 26, 20));
        getChildren().add(footer);

        // 左上角提示（Esc = 放弃本局回主菜单，与起点房间一致）
        Label esc = new Label("Esc 放弃本局回主菜单");
        esc.setTextFill(Color.rgb(203, 213, 225, 0.7));
        esc.setFont(Font.font(13));
        StackPane.setAlignment(esc, Pos.TOP_LEFT);
        StackPane.setMargin(esc, new Insets(14, 0, 0, 18));
        getChildren().add(esc);
    }

    // ================= 选项 =================

    /** 一个选项格子：标题 + 效果说明 */
    private Button buildOption(String name, String effect, Runnable action, boolean enabled) {
        Label n = new Label(name);
        n.setTextFill(Color.rgb(253, 224, 71));
        n.setFont(Font.font(20));
        n.setStyle("-fx-font-weight: bold;");

        Label e = new Label(effect);
        e.setTextFill(Color.rgb(226, 232, 240));
        e.setFont(Font.font(13.5));
        e.setWrapText(true);
        e.setMaxWidth(OPTION_W - 44);
        e.setAlignment(Pos.CENTER);

        VBox text = new VBox(6, n, e);
        text.setAlignment(Pos.CENTER);

        Button btn = new Button();
        btn.setGraphic(text);
        btn.setPrefSize(OPTION_W, OPTION_H);
        btn.setMinSize(OPTION_W, OPTION_H);
        btn.setMaxSize(OPTION_W, OPTION_H);
        btn.setCursor(Cursor.HAND);

        if (!enabled) {
            btn.setStyle(OPTION_STYLE_LOCKED);
            btn.setCursor(Cursor.DEFAULT);
            btn.setOnAction(ev -> { });
            optionButtons.add(btn);
            return btn;
        }

        btn.setStyle(OPTION_STYLE);
        btn.setOnMouseEntered(ev -> { if (!chosen) btn.setStyle(OPTION_STYLE_HOVER); });
        btn.setOnMouseExited(ev -> { if (!chosen) btn.setStyle(OPTION_STYLE); });
        btn.setOnAction(ev -> action.run());
        optionButtons.add(btn);
        return btn;
    }

    /** 休息：恢复 30% 最大生命值 */
    private void doRest() {
        if (chosen) return;
        chosen = true;

        int before = player.hp();
        player.heal(healAmount());
        player.restedAtCampfire = true; // 古茶具套装：篝火休息后下一场战斗 +2 能量

        refreshHpLabel();
        lockOptions(restBtn);
        hint.setText("你靠在火边睡了一会，生命 " + before + " → " + player.hp() + " —— 可以离开了");
        leaveBtn.setVisible(true);
    }

    /** 强化卡牌：打开选牌页，选中的那张升级（数值/文案换成升级版，牌组里的位置不变） */
    private void doUpgrade() {
        if (chosen || !hasUpgradable()) return;

        DeckPickOverlay picker = new DeckPickOverlay(
                player,
                "强化卡牌 · 选择一张牌升级",
                "点击卡牌将其强化 · 按牌 id 排列 · 点遮罩或「取消」返回",
                null,                 // 用玩家当前牌组
                Card::canUpgrade,     // 已强化的牌与状态牌压暗不可选
                c -> {
                    Card up = c.upgrade();
                    int idx = player.deck.indexOf(c);
                    if (idx >= 0) player.deck.set(idx, up);
                    chosen = true;
                    lockOptions(upgradeBtn);
                    hint.setText("已强化：" + up.name() + " —— 可以离开了");
                    leaveBtn.setVisible(true);
                },
                null,                 // 取消 = 什么都没做，留在篝火页
                c -> {
                    if (!c.canUpgrade()) return "这张牌不能再强化";
                    Card up = c.upgrade();
                    return "升级后：" + up.name() + " —— " + up.desc();
                });
        getChildren().add(picker); // RestView 是 StackPane：铺在最上层盖住整页
        picker.show();
    }

    /** 选完之后：把选中的那个高亮、另一个压暗（都不可再点） */
    private void lockOptions(Button chosenBtn) {
        for (Button b : optionButtons) {
            b.setStyle(b == chosenBtn ? OPTION_STYLE_CHOSEN : OPTION_STYLE_LOCKED);
        }
    }

    // ================= 文案 / 数据 =================

    /** 一次休息恢复多少：最大生命值的 30%（至少 1 点） */
    private int healAmount() {
        return Math.max(1, (int) Math.round(player.maxHp * HEAL_RATIO));
    }

    private String healText() {
        if (player.hp() >= player.maxHp) return "生命已满，休息不会恢复";
        return "恢复 " + healAmount() + " 点生命（最大生命的 30%）";
    }

    private String upgradeText() {
        long n = player.deck.stream().filter(Card::canUpgrade).count();
        if (n == 0) return "没有可强化的牌";
        // 手动断行，免得自动换行把「张）」单独挤到第三行
        return "选择一张牌强化，数值 / 费用提升\n可强化 " + n + " 张";
    }

    private boolean hasUpgradable() {
        return player.deck.stream().anyMatch(Card::canUpgrade);
    }

    private void refreshHpLabel() {
        hpLabel.setText("当前生命 " + player.hp() + " / " + player.maxHp);
    }

    // ================= 工具 =================

    private static Image loadImage(String path) {
        var in = RestView.class.getResourceAsStream(path);
        return in == null ? null : new Image(in);
    }

    /** 背景图等比铺满（cover）：按窗口比例放大，多余部分居中裁掉 */
    private static Background cover(Image image) {
        return new Background(new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1, 1, true, true, false, true)));
    }
}
