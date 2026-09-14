package com.example.demo.view;

import com.example.demo.card.Card;
import com.example.demo.card.CardRewardPool;
import com.example.demo.character.Player;
import com.example.demo.character.Relic;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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

import java.util.List;
import java.util.Random;

/**
 * “起点房间”：进入地图冒险前和 NPC 对话、选一个初始遗物。
 *
 * 版面（从上到下）：
 *   …上半屏留白，露出背景图 nieo.png…
 *   对话框   ← 随机台词，点一下换一句
 *   遗物选项 1
 *   遗物选项 2   ← 三个选项在屏幕下半部分，一行一个，格子写“图标 + 名字 + 描述”
 *   遗物选项 3
 *   提示 + “离开房间”按钮（选中遗物后才出现）
 *
 * 背景用 nieo.png 等比铺满（cover），所以窗口怎么拉都不会变形。
 */
public class RoomView extends StackPane {

    /** 背景图路径（放 resources/com/example/demo/ 下） */
    private static final String BG = "/com/example/demo/nieo.png";

    /**
     * 下半部分内容块的宽度 —— <b>想改「选项宽度」就是改这个</b>。
     *
     * <p>遗物格子、对话框、底部条三者共用这一个宽度（它们本来就该左右对齐），
     * 所以改它会一起变宽 / 变窄。</p>
     *
     * <p>⚠ 不要去调 {@code desc.setMaxWidth(...)}：描述能占多宽完全由
     * 「行宽 − 图标 − 名字 − 两个间距」决定，那个上限只是照这个式子算出来的。
     * 单独把它改大 / 改小都会被行宽压回去，界面上看不出任何变化。</p>
     *
     * <p>描述实际能用的宽度 = <b>CONTENT_W − 262</b>（行宽减 40+150+32+40）。
     * 目前最长的描述是「混沌」的 626px，所以 CONTENT_W 至少要 902 才不被省略号截断；
     * 现在取 920，留 32px 余量。</p>
     */
    private static final double CONTENT_W = 920;

    /** 遗物行里元素之间的间距（图标↔名字、名字↔描述） */
    private static final double RELIC_ROW_GAP = 16;

    /** 遗物行里「名字」列的最小宽度 —— 让几个选项的名字对齐成一列 */
    private static final double RELIC_NAME_W = 150;

    /**
     * 遗物格子里左边那个图标的边长。
     * 格子高 66、上下 padding 各 12 → 内容区 42，取 40 刚好不撑破行高。
     */
    private static final double RELIC_ICON_SIZE = 40;

    /** 混沌一次给几次卡牌奖励 */
    private static final int CHAOS_CARD_REWARDS = 3;

    private static final String CARD_STYLE = "-fx-background-color: rgba(15, 23, 42, 0.82); "
            + "-fx-background-radius: 12; -fx-border-color: rgba(148, 163, 184, 0.45); "
            + "-fx-border-width: 2; -fx-border-radius: 12; -fx-padding: 12 18 12 18;";
    private static final String CARD_STYLE_HOVER = "-fx-background-color: rgba(30, 41, 59, 0.92); "
            + "-fx-background-radius: 12; -fx-border-color: rgba(226, 232, 240, 0.75); "
            + "-fx-border-width: 2; -fx-border-radius: 12; -fx-padding: 12 18 12 18;";
    private static final String CARD_STYLE_CHOSEN = "-fx-background-color: rgba(69, 26, 3, 0.92); "
            + "-fx-background-radius: 12; -fx-border-color: #fbbf24; "
            + "-fx-border-width: 3; -fx-border-radius: 12; -fx-padding: 12 18 12 18;";

    private final Player player;
    private final List<Relic> options;
    private final List<String> dialogues; // 随机台词池
    private final String npcName;
    private final Runnable onLeave;
    private final Random rnd = new Random();

    private boolean chosen = false;
    private int dialogIndex = -1;
    private final List<Button> optionButtons = new java.util.ArrayList<>();

    /** 混沌的三连卡牌奖励弹层（只建一次反复 show）；没选混沌时一直是 null */
    private RewardOverlay chaosOverlay;
    /** 混沌还剩几次卡牌奖励没给（0 = 已给完） */
    private int chaosRewardsLeft = 0;

    private final Label dialogText = new Label();
    private final Button leaveBtn = new Button("离开房间");
    private final Label hint = new Label("选择一个初始遗物后即可离开");

    /** 兼容旧写法：只给一句台词 */
    public RoomView(Player player, List<Relic> options, String npcName,
                    String npcDialog, Runnable onLeave) {
        this(player, options, npcName, List.of(npcDialog), onLeave);
    }

    public RoomView(Player player, List<Relic> options, String npcName,
                    List<String> dialogues, Runnable onLeave) {
        this.player = player;
        this.options = options;
        this.npcName = npcName;
        this.dialogues = (dialogues == null || dialogues.isEmpty())
                ? List.of("……") : dialogues;
        this.onLeave = onLeave;

        // ---- 背景：nieo.png 等比铺满 ----
        Image bg = loadImage(BG);
        if (bg != null) {
            setBackground(cover(bg));
        } else {
            setStyle("-fx-background-color: linear-gradient(to bottom, #131a22, #1c2430);");
        }

        // ---- 下半部分：对话框 → 三个遗物格子 → 提示/离开 ----
        VBox lower = new VBox(11);
        lower.setAlignment(Pos.CENTER);
        lower.setMaxWidth(CONTENT_W);

        lower.getChildren().add(buildDialogBox());
        for (Relic r : options) {
            lower.getChildren().add(buildRelicRow(r));
        }
        lower.getChildren().add(buildFooter());

        StackPane.setAlignment(lower, Pos.BOTTOM_CENTER);
        StackPane.setMargin(lower, new Insets(0, 20, -400, 20));
        getChildren().add(lower);

        showRandomDialog(); // 开局随机一句

        // 左上角提示（Esc = 放弃本局回主菜单）
        Label esc = new Label("Esc 放弃本局回主菜单");
        esc.setTextFill(Color.rgb(203, 213, 225, 0.7));
        esc.setFont(Font.font(13));
        StackPane.setAlignment(esc, Pos.TOP_LEFT);
        StackPane.setMargin(esc, new Insets(14, 0, 0, 18));
        getChildren().add(esc);
    }

    // ================= 对话框 =================

    /** 对话框：选项上方那条，点一下随机换一句台词 */
    private StackPane buildDialogBox() {
        Label nameTag = new Label(npcName);
        nameTag.setTextFill(Color.rgb(253, 224, 71));
        nameTag.setFont(Font.font(16));
        nameTag.setStyle("-fx-font-weight: bold;");

        dialogText.setTextFill(Color.rgb(241, 245, 249));
        dialogText.setFont(Font.font(16));
        dialogText.setWrapText(true);
        dialogText.setMaxWidth(CONTENT_W - 70);

        VBox box = new VBox(6, nameTag, dialogText);
        box.setAlignment(Pos.CENTER_LEFT);

        StackPane bubble = new StackPane(box);
        bubble.setPadding(new Insets(14, 20, 14, 20));
        bubble.setMaxWidth(CONTENT_W);
        bubble.setMinHeight(104);
        bubble.setStyle("-fx-background-color: rgba(15, 23, 42, 0.86); "
                + "-fx-background-radius: 14; -fx-border-color: rgba(148, 163, 184, 0.5); "
                + "-fx-border-width: 1; -fx-border-radius: 14;");
        bubble.setCursor(Cursor.HAND);
        Tooltip.install(bubble, new Tooltip("点一下换一句台词"));
        bubble.setOnMouseClicked(e -> showRandomDialog());
        StackPane.setAlignment(box, Pos.CENTER_LEFT);
        return bubble;
    }

    /** 从台词池里随机挑一句（尽量不和上一句重复） */
    private void showRandomDialog() {
        if (dialogues.size() == 1) {
            dialogIndex = 0;
        } else {
            int next;
            do {
                next = rnd.nextInt(dialogues.size());
            } while (next == dialogIndex);
            dialogIndex = next;
        }
        dialogText.setText("“" + dialogues.get(dialogIndex) + "”");
    }

    // ================= 遗物选项 =================

    /** 一个遗物格子：一行搞定「图标 + 名字 + 描述」 */
    private Button buildRelicRow(Relic r) {
        // 行宽 = 按钮内容区 = CONTENT_W − 左右 padding(18×2) − 边框(2×2) = CONTENT_W − 40。
        // 所以「选项宽度」的唯一开关是 CONTENT_W，下面那些尺寸都只是按它算出来的。
        double rowW = CONTENT_W - 40;

        // 图标放在名字左边。buildIconGraphic 是遗物图标的唯一渲染入口
        // （只画图标，不带点击 / Tooltip —— 点击归整行的按钮）。
        // 没配贴图或贴图加载失败时，它会退回「名字首字」的紫色方块。
        StackPane icon = r.buildIconGraphic(RELIC_ICON_SIZE);

        Label name = new Label(r.name);
        name.setTextFill(Color.rgb(253, 224, 71));
        name.setFont(Font.font(19));
        name.setStyle("-fx-font-weight: bold;");
        name.setMinWidth(RELIC_NAME_W);

        Label desc = new Label(r.desc);
        desc.setTextFill(Color.rgb(226, 232, 240));
        desc.setFont(Font.font(15));
        desc.setWrapText(false); // 只占一行
        // 描述能占多宽 = 行宽 − 图标 − 名字 − 两个间距（当前 840−40−150−32 = 618）。
        // 按同一个式子算出来当上限，改 CONTENT_W 时会自动跟着走；
        // 单独调这个数是没用的 —— 行宽不给那么多，改大改小都会被压回去。
        desc.setMaxWidth(rowW - RELIC_ICON_SIZE - RELIC_NAME_W - RELIC_ROW_GAP * 2);

        HBox row = new HBox(RELIC_ROW_GAP, icon, name, desc);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefWidth(rowW);

        Button btn = new Button();
        btn.setGraphic(row);
        btn.setStyle(CARD_STYLE);
        btn.setCursor(Cursor.HAND);
        btn.setMaxWidth(CONTENT_W);
        btn.setMinWidth(CONTENT_W);
        btn.setPrefHeight(66);
        btn.setOnMouseEntered(e -> {
            if (!chosen) btn.setStyle(CARD_STYLE_HOVER);
        });
        btn.setOnMouseExited(e -> {
            if (!chosen) btn.setStyle(CARD_STYLE);
        });
        btn.setOnAction(e -> choose(r, btn));
        optionButtons.add(btn);
        return btn;
    }

    /** 选中一个遗物：加金框、清掉其它选项的高亮、出现离开按钮 */
    private void choose(Relic r, Button btn) {
        if (chosen) return;
        chosen = true;

        // 先把所有格子恢复普通样式，再高亮选中的那个
        for (Button b : optionButtons) b.setStyle(CARD_STYLE);
        btn.setStyle(CARD_STYLE_CHOSEN);
        btn.setDisable(false); // 保持可点（只是不再响应，choose 里有 chosen 守卫）

        player.addRelic(r); // 遗物记进玩家状态（含破镜/混沌等即时效果，由 RelicFun 统一处理）
        // 破镜：需要 UI 交互，单独处理
        if (r.name.equals("破镜")) {
            removeCardFromDeck();
        }
        // 青铜怀表：需要 UI 交互（升级一张牌），单独处理
        if (r.name.equals("青铜怀表")) {
            upgradeCardFromDeck();
        }
        // 混沌：需要 UI 交互（三次卡牌奖励），单独处理。
        // 地图变异那半边（player.chaos）已经在 RelicFun.onRelicObtained 里置好了。
        if (r.name.equals("混沌")) {
            giveChaosCardRewards();
        }
        
        hint.setText("已获得：" + r.name + " —— 可以离开了");
        hint.setTextFill(Color.rgb(251, 191, 36));
        leaveBtn.setVisible(true);
    }

    /**
     * 破镜效果：让玩家选择一张卡牌删除。
     *
     * <p>版面走 {@link DeckPickOverlay}，与牌组页（{@code HelloApplication.deckPage}）同款 ——
     * 深色面板 + 24px 标题 + 卡面滚动区，区别只是这里的卡面可以点。
     */
    private void removeCardFromDeck() {
        if (player.deck.isEmpty()) return;

        DeckPickOverlay picker = new DeckPickOverlay(
                player,
                "破镜 · 选择一张牌移除",
                c -> {
                    player.deck.remove(c);
                    hint.setText("已移除：" + c.name() + " —— 可以离开了");
                });
        getChildren().add(picker); // RoomView 是 StackPane：铺在最上层盖住整个房间
        picker.show();
    }

    /**
     * 青铜怀表效果：让玩家选择一张牌强化。
     *
     * <p>和篝火「强化卡牌」（{@code RestView.doUpgrade}）走<b>同一套</b>
     * {@link DeckPickOverlay} + {@link Card#canUpgrade()} + {@link Card#upgrade()}：
     * 已强化的牌和状态牌压暗不可选，悬停显示升级后的名字与描述。</p>
     *
     * <p>「取消」= 跳过强化（和破镜取消跳过删牌一样），遗物照样已经拿到手。</p>
     */
    private void upgradeCardFromDeck() {
        if (player.deck.stream().noneMatch(Card::canUpgrade)) return; // 没有可强化的牌就静默跳过

        DeckPickOverlay picker = new DeckPickOverlay(
                player,
                "青铜怀表 · 选择一张牌强化",
                "点击卡牌将其强化 · 按牌 id 排列 · 点遮罩或「取消」跳过",
                null,                 // 用玩家当前牌组
                Card::canUpgrade,     // 已强化的牌与状态牌压暗不可选
                c -> {
                    Card up = c.upgrade();
                    int idx = player.deck.indexOf(c);
                    if (idx >= 0) player.deck.set(idx, up);
                    hint.setText("已强化：" + up.name() + " —— 可以离开了");
                },
                null,                 // 取消 = 跳过强化，仍然可以离开房间
                c -> {
                    if (!c.canUpgrade()) return "这张牌不能再强化";
                    Card up = c.upgrade();
                    return "升级后：" + up.name() + " —— " + up.desc();
                });
        getChildren().add(picker); // RoomView 是 StackPane：铺在最上层盖住整个房间
        picker.show();
    }

    // ================= 混沌：三次卡牌奖励 =================

    /**
     * 混沌：连给三次卡牌奖励。
     *
     * <p>和战斗胜利的奖励<b>完全同一套</b> —— 同一个 {@link RewardOverlay} 三选一界面、
     * 同一个 {@link CardRewardPool#rewardPool()} 卡池与抽取规则（普通池概率 + 怜悯偏移），
     * 只是标题换成「混沌 · 卡牌奖励（n/3）」，不写「战斗胜利」。点「跳过」也和胜利时
     * 一样只算放弃这一次，三次机会照给。</p>
     *
     * <p>弹层只建一个反复用：{@link RewardOverlay#show} 每次都会重置内部的选择守卫，
     * 所以连着 show 三次是安全的；给完最后一次才 {@code hide()}。</p>
     */
    private void giveChaosCardRewards() {
        chaosRewardsLeft = CHAOS_CARD_REWARDS;
        if (chaosOverlay == null) {
            chaosOverlay = new RewardOverlay(this::onChaosRewardDecided);
        }
        if (!getChildren().contains(chaosOverlay)) {
            getChildren().add(chaosOverlay); // RoomView 是 StackPane：铺在最上层盖住房间
        }
        showNextChaosReward();
    }

    /** 这一次奖励处理完了（选牌或跳过）：还有就弹下一次，没了就把弹层收起来 */
    private void onChaosRewardDecided() {
        chaosRewardsLeft--;
        if (chaosRewardsLeft <= 0) {
            chaosOverlay.hide();
            return;
        }
        showNextChaosReward();
    }

    /** 弹第 {@code CHAOS_CARD_REWARDS - chaosRewardsLeft + 1} 次三选一 */
    private void showNextChaosReward() {
        int index = CHAOS_CARD_REWARDS - chaosRewardsLeft + 1; // 3 → 1
        chaosOverlay.setHeader(
                "混沌 · 卡牌奖励（" + index + "/" + CHAOS_CARD_REWARDS + "）",
                "选择一张牌加入牌组");
        List<Card> offers = CardRewardPool.draw(CardRewardPool.rewardPool(), 3, false);
        chaosOverlay.show(offers, (c, node) -> {
            player.deck.add(c);      // 数据即时结算；牌组页会跟着变
            onChaosRewardDecided();
        });
    }

    // ================= 底部 =================

    private HBox buildFooter() {
        hint.setTextFill(Color.rgb(226, 232, 240, 0.75));
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
        return footer;
    }

    // ================= 工具 =================

    private static Image loadImage(String path) {
        var in = RoomView.class.getResourceAsStream(path);
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
