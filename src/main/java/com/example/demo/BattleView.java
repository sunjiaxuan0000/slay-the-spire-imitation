package com.example.demo;

import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * 回合制战斗界面。
 *
 * 布局：角色在左、怪物在右（立绘下方是血量条和 buff/debuff）；
 * 卡牌横排在屏幕下方；左下角是抽牌堆小图标、右下角是弃牌堆小图标
 * （图标带数字下标 = 堆内牌数，点开可看堆里的牌，按牌 id 排序）。
 *
 * 规则：角色先手 → 回合开始抽 5 张 → 打牌(立刻进弃牌堆)或结束回合(手牌全进弃牌堆)
 *       → 怪物按意图行动 → 抽牌堆空了自动洗弃牌堆。能量每回合 3 点。
 */
public class BattleView extends StackPane {

    private final Player player;
    private final RunHud hud;
    private final Enemy enemy;
    private final Consumer<Boolean> onFinish;

    private final List<Card> draw = new ArrayList<>();
    private final List<Card> discard = new ArrayList<>();
    private final List<Card> hand = new ArrayList<>();
    private final Random rnd = new Random();

    private int energy = 3;
    private int playerBlock = 0;
    private int weakTurns = 0;
    private int enemyVulnerable = 0; // 敌人易伤层数：承受伤害 +50%，每回合 -1，多层不叠加伤害加成
    private int enemyWeakTurns = 0; // 敌人虚弱层数：造成伤害 ×0.75，每回合 -1
    private int playerStrength = 0; // 玩家力量层数：每层为造成的所有伤害 +1，不随回合减少
    private boolean playerTurn = true;
    private boolean battleOver = false;
    private boolean drawLocked = false; // 本回合抽牌被锁定（战斗专注效果）
    private boolean paused = false;            // 切去只读地图时暂停，防止后台偷偷行动
    private boolean pendingTurnStart = false;  // 暂停期间错过的“下一回合开始”

    // ================= UI =================
    // 角色(左)
    private final Label pName = new Label(Player.CHARACTER_NAME);
    private final Label pHpText = new Label();          // 血条上的数字数字
    private final Region pHpFill = new Region();
    private final StackPane pHpWrap = new StackPane();  // 包住血条的容器（有格挡=金属框）
    private final StackPane pShield = new StackPane();  // 格挡盾牌
    private final Label pShieldNum = new Label();
    private final FlowPane pChips = new FlowPane(4, 4); // buff/debuff 图标行
    // 怪物(右)
    private final Label eName = new Label();
    private final StackPane eIntentIcon = new StackPane(); // 意图图标
    private final Label eIntentNum = new Label();          // 攻击意图的数字
    private final Label eHpText = new Label();
    private final Region eHpFill = new Region();
    private final StackPane eHpWrap = new StackPane();
    private final StackPane eShield = new StackPane();
    private final Label eShieldNum = new Label();
    private final FlowPane eChips = new FlowPane(4, 4);
    // 中下
    private int turn = 0;                        // 当前第几回合
    private final Label turnLabel = new Label(); // 中间：第 n 回合
    private final Label energyLabel = new Label(); // 左下抽牌堆上方
    private final Label pilesInfo = new Label();
    private final HBox handBox = new HBox(10);
    private final Button endTurnBtn = new Button("结束回合");
    private Label emptyHandLabel;
    // 牌堆图标 + 计数下标
    private final Label drawBadge = new Label();
    private final Label discardBadge = new Label();
    private final StackPane drawIcon = pileIcon("抽", "#78350f");
    private final StackPane discardIcon = pileIcon("弃", "#1e293b");
    // 牌堆详情弹层（StackPane 才能让遮罩铺满、内容居中）
    private final StackPane overlay = new StackPane();
    private final Label overlayTitle = new Label();
    private final HBox overlayCards = new HBox(8);
    // 胜利奖励弹层（三选一加牌）
    private final StackPane rewardOverlay = new StackPane();
    private final HBox rewardBox = new HBox(16);
    private boolean rewardChosen = false;
    // 死亡演出
    private StackPane playerPortrait;                 // 角色立绘（倒地动画）
    private final Pane deadDim = new Pane();          // 背景变暗层（在内容下层，角色/怪物保持高亮）
    private final StackPane deathOverlay = new StackPane(); // 死亡提示页
    private boolean diedShown = false;

    public BattleView(Player player, RunHud hud, Enemy enemy, Consumer<Boolean> onFinish) {
        this.player = player;
        this.hud = hud;
        this.enemy = enemy;
        this.onFinish = onFinish;

        // 整层背景（以后换战斗背景图）
        setStyle("-fx-background-color: linear-gradient(to bottom, #191511, #23201c);");

        // 背景变暗层：默认隐藏，死亡时点亮（它在内容层下面 → 只有背景暗、角色怪物亮）
        deadDim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.5);");
        deadDim.setVisible(false);
        getChildren().add(deadDim);

        BorderPane main = new BorderPane();
        main.setStyle("-fx-background-color: transparent;");

        // ---- 中间三大块：角色(左) | 回合信息(中) | 怪物(右) ----
        HBox fighters = new HBox(10);
        fighters.setAlignment(Pos.CENTER);

        VBox left = buildLeftPanel();   // 角色
        VBox middle = buildMiddlePanel(); // 能量 / 结束回合
        VBox right = buildRightPanel();  // 怪物
        fighters.getChildren().addAll(left, middle, right);
        main.setCenter(fighters);

        // ---- 底部：手牌区 ----
        main.setBottom(buildHandPanel());
        main.setPadding(new Insets(8, 10, 8, 10));

        // 主布局（本类就是 StackPane，子元素叠放、可拉伸的自动铺满）
        getChildren().add(main);

        // ---- 牌堆图标：左下抽牌堆 / 右下弃牌堆 ----
        drawIcon.setOnMouseClicked(e -> openPile("抽牌堆", draw));
        discardIcon.setOnMouseClicked(e -> openPile("弃牌堆", discard));
        StackPane.setAlignment(drawIcon, Pos.BOTTOM_LEFT);
        StackPane.setMargin(drawIcon, new Insets(0, 0, 16, 22));
        StackPane.setAlignment(discardIcon, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(discardIcon, new Insets(0, 22, 16, 0));
        getChildren().addAll(drawIcon, discardIcon);

        // 数量下标挂到图标右下角
        attachBadge(drawIcon, drawBadge);
        attachBadge(discardIcon, discardBadge);
        drawBadge.setVisible(false);
        discardBadge.setVisible(false);

        // ---- 能量：显示在抽牌堆(左下)上方 ----
        energyLabel.setTextFill(Color.rgb(251, 191, 36));
        energyLabel.setFont(Font.font(20));
        StackPane.setAlignment(energyLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(energyLabel, new Insets(0, 0, 128, 22));
        getChildren().add(energyLabel);

        // ---- 结束回合：手牌右方、弃牌堆(右下)左上方 ----
        endTurnBtn.setFont(Font.font(17));
        endTurnBtn.setPrefSize(160, 44);
        endTurnBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; "
                + "-fx-background-radius: 12; -fx-cursor: hand;");
        endTurnBtn.setOnAction(e -> endPlayerTurn());
        StackPane.setAlignment(endTurnBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(endTurnBtn, new Insets(0, 116, 132, 0));
        getChildren().add(endTurnBtn);

        // 牌堆浏览弹层（默认隐藏，铺满整个画面）
        buildOverlay();
        overlay.setVisible(false);
        getChildren().add(overlay);

        // 胜利奖励弹层（三选一加牌，默认隐藏）
        buildRewardOverlay();
        rewardOverlay.setVisible(false);
        getChildren().add(rewardOverlay);

        // 死亡提示页（默认隐藏）
        buildDeathOverlay();
        deathOverlay.setVisible(false);
        getChildren().add(deathOverlay);

        // 开局：牌组洗入抽牌堆，角色先手
        draw.addAll(player.deck);
        Collections.shuffle(draw, rnd);

        // 遗物：保温杯 → 战斗开始恢复 10 点生命
        if (hasRelic("保温杯")) {
            player.heal(10);
            hud.refresh();
        }
        startPlayerTurn();
    }

    /** 检查玩家是否持有某个遗物（遗物效果都靠它触发） */
    private boolean hasRelic(String name) {
        for (Relic r : player.relics) {
            if (r.name.equals(name)) return true;
        }
        return false;
    }

    // ================= 面板搭建 =================

    /** 左侧：角色（立绘 → 血条(数字/数字，有格挡=金属框+盾牌) → buff/debuff图标） */
    private VBox buildLeftPanel() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(360);

        pName.setTextFill(Color.WHITE);
        pName.setFont(Font.font(26));
        pName.setStyle("-fx-font-weight: bold;");

        playerPortrait = portrait("战",
                "radial-gradient(center 35% 30%, radius 100%, #b45309, #451a03);");
        playerPortrait.setPrefSize(210, 210);
        playerPortrait.setMaxSize(210, 210);

        // 血条（数字/数字 写在条上）
        hpWrap(pHpWrap, pHpFill, pHpText, 240, "#22c55e");
        // 格挡盾牌
        pShieldNum.setTextFill(Color.WHITE);
        pShieldNum.setFont(Font.font(13));
        pShieldNum.setStyle("-fx-font-weight: bold;");
        shield(pShield, pShieldNum);
        Tooltip.install(pShield, new Tooltip("格挡：吸收等量伤害，下回合开始清除"));

        HBox cluster = new HBox(6);
        cluster.setAlignment(Pos.CENTER_LEFT);
        cluster.getChildren().addAll(pShield, pHpWrap);

        // buff/debuff 图标行（超过血条宽度自动换行）
        pChips.setPrefWrapLength(286);
        pChips.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(pName, playerPortrait, cluster, pChips);
        return box;
    }

    /** 右侧：怪物（意图图标 → 立绘 → 血条 → buff/debuff图标） */
    private VBox buildRightPanel() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(360);

        eName.setTextFill(Color.WHITE);
        eName.setFont(Font.font(26));
        eName.setStyle("-fx-font-weight: bold;");

        // 意图：图标(+攻击数字)，悬停看含义
        eIntentIcon.setPrefSize(46, 46);
        eIntentIcon.setMaxSize(46, 46);
        eIntentNum.setTextFill(Color.WHITE);
        eIntentNum.setFont(Font.font(22));
        eIntentNum.setStyle("-fx-font-weight: bold;");
        eIntentNum.setVisible(false);
        HBox intentRow = new HBox(8);
        intentRow.setAlignment(Pos.CENTER);
        intentRow.getChildren().addAll(eIntentIcon, eIntentNum);

        StackPane portrait = portrait(enemy.name.substring(0, 1),
                "radial-gradient(center 35% 30%, radius 100%, #6b7280, #1f2937);");
        portrait.setPrefSize(210, 210);
        portrait.setMaxSize(210, 210);

        hpWrap(eHpWrap, eHpFill, eHpText, 240, "#dc2626");
        eShieldNum.setTextFill(Color.WHITE);
        eShieldNum.setFont(Font.font(13));
        eShieldNum.setStyle("-fx-font-weight: bold;");
        shield(eShield, eShieldNum);
        Tooltip.install(eShield, new Tooltip("格挡：吸收等量伤害"));

        HBox cluster = new HBox(6);
        cluster.setAlignment(Pos.CENTER_LEFT);
        cluster.getChildren().addAll(eShield, eHpWrap);

        eChips.setPrefWrapLength(286);
        eChips.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(eName, intentRow, portrait, cluster, eChips);
        return box;
    }

    /** 中间一列：显示当前第几回合 */
    private VBox buildMiddlePanel() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(220);

        turnLabel.setTextFill(Color.rgb(248, 250, 252));
        turnLabel.setFont(Font.font(24));
        turnLabel.setStyle("-fx-font-weight: bold;");

        Label sub = new Label("角色先手，轮到你行动");
        sub.setTextFill(Color.rgb(120, 113, 108));
        sub.setFont(Font.font(14));

        box.getChildren().addAll(turnLabel, sub);
        return box;
    }

    private VBox buildHandPanel() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(6, 0, 4, 0));

        pilesInfo.setTextFill(Color.rgb(148, 163, 184));
        pilesInfo.setFont(Font.font(13));

        handBox.setAlignment(Pos.CENTER);
        emptyHandLabel = new Label("手牌已空 — 点击“结束回合”");
        emptyHandLabel.setTextFill(Color.rgb(148, 163, 184));
        emptyHandLabel.setFont(Font.font(15));

        box.getChildren().addAll(handBox, emptyHandLabel, pilesInfo);
        return box;
    }

    /** 血条：底槽 + 填充 + 数字/数字文字，包进 wrap（wrap 负责“有格挡=金属框”） */
    private static void hpWrap(StackPane wrap, Region fill, Label text, double width, String fillColor) {
        HBox bar = new HBox();
        bar.setPrefSize(width, 18);
        bar.setMaxSize(width, 18);
        bar.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 9;");
        bar.getChildren().add(fill);
        fill.setStyle("-fx-background-color: " + fillColor + "; -fx-background-radius: 9;");

        text.setTextFill(Color.WHITE);
        text.setFont(Font.font(13));
        text.setStyle("-fx-font-weight: bold;");

        wrap.setPrefSize(width, 18);
        wrap.setMaxSize(width, 18);
        wrap.getChildren().addAll(bar, text); // 条在底、数字在上
        wrap.setStyle("-fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 10;");
    }

    /** 有格挡时：血条包上金属框 */
    private static String frameStyle(boolean metal) {
        return metal
                ? "-fx-border-color: #e2e8f0; -fx-border-width: 2; -fx-border-radius: 10; "
                        + "-fx-effect: dropshadow(gaussian, rgba(226,232,240,0.35), 4, 0, 0, 0);"
                : "-fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 10;";
    }

    /** 格挡盾牌：圆底 + 盾牌图标 + 格挡数字 */
    private static void shield(StackPane s, Label num) {
        s.setPrefSize(32, 32);
        s.setMaxSize(32, 32);
        s.setStyle("-fx-background-color: #0ea5e9; -fx-background-radius: 16; "
                + "-fx-border-color: #bae6fd; -fx-border-width: 1; -fx-border-radius: 16;");
        s.getChildren().add(num);
        s.setVisible(false);
    }

    /** buff/debuff 图标 + 层数 */
    private static HBox statusChip(String glyph, int count, String color, String tip) {
        StackPane icon = new StackPane();
        icon.setPrefSize(24, 24);
        icon.setMaxSize(24, 24);
        icon.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12;");
        Label g = new Label(glyph);
        g.setTextFill(Color.WHITE);
        g.setFont(Font.font(12));
        g.setStyle("-fx-font-weight: bold;");
        icon.getChildren().add(g);

        Label n = new Label(String.valueOf(count));
        n.setTextFill(Color.WHITE);
        n.setFont(Font.font(13));
        n.setStyle("-fx-font-weight: bold;");

        HBox chip = new HBox(3);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getChildren().addAll(icon, n);
        Tooltip.install(chip, new Tooltip(tip));
        return chip;
    }

    /** 怪物意图刷新：攻击=图标+数字；其余只图标；悬停显示含义 */
    private void refreshIntent() {
        Enemy.Step s = enemy.current();
        String glyph;
        String color;
        String tip;
        int number = 0;

        switch (s.intent) {
            case ATTACK -> {
                glyph = "攻";
                color = "#dc2626";
                number = s.value + enemy.power;
                tip = "意图·攻击：将对玩家造成 " + number + " 伤害";
            }
            case DEFEND -> {
                glyph = "防";
                color = "#0284c7";
                tip = "意图·防御：获得 " + s.value + " 格挡";
            }
            case BUFF   -> {
                glyph = "强";
                color = "#d97706";
                tip = "意图·强化自身：力量 +" + s.value;
            }
            default     -> {
                glyph = "弱";
                color = "#7c3aed";
                tip = "意图·虚弱我方：" + s.value + " 回合，你造成的伤害 ×0.75";
            }
        }

        eIntentIcon.getChildren().clear();
        eIntentIcon.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 23;");
        Label g = new Label(glyph);
        g.setTextFill(Color.WHITE);
        g.setFont(Font.font(20));
        g.setStyle("-fx-font-weight: bold;");
        eIntentIcon.getChildren().add(g);

        eIntentNum.setText(String.valueOf(number));
        eIntentNum.setVisible(s.intent == Enemy.Intent.ATTACK); // 只有攻击意图显示数字
        Tooltip.install(eIntentIcon, new Tooltip(tip));
    }

    /** 立绘圆（占位，以后换成 ImageView） */
    private static StackPane portrait(String glyph, String gradient) {
        StackPane p = new StackPane();
        p.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 105;");
        Label g = new Label(glyph);
        g.setTextFill(Color.rgb(255, 255, 255, 0.85));
        g.setFont(Font.font(92));
        p.getChildren().add(g);
        return p;
    }

    /** 牌堆小图标（左下抽牌堆 / 右下弃牌堆），右下角带数量下标 */
    private StackPane pileIcon(String glyph, String color) {
        StackPane icon = new StackPane();
        icon.setPrefSize(76, 100);
        icon.setMaxSize(76, 100);
        icon.setCursor(javafx.scene.Cursor.HAND);
        icon.setStyle("-fx-background-color: linear-gradient(to bottom right, #e5e7eb, #9ca3af); "
                + "-fx-background-radius: 10;");
        Label g = new Label(glyph);
        g.setTextFill(Color.rgb(55, 65, 81));
        g.setFont(Font.font(26));
        g.setStyle("-fx-font-weight: bold;");
        StackPane.setAlignment(g, Pos.CENTER);
        icon.getChildren().add(g);
        return icon;
    }

    /** 把数量下标放到牌堆图标右下角 */
    private static void attachBadge(StackPane icon, Label badge) {
        badge.setTextFill(Color.WHITE);
        badge.setFont(Font.font(13));
        badge.setStyle("-fx-font-weight: bold; -fx-background-color: #dc2626; "
                + "-fx-background-radius: 9; -fx-padding: 1 6 1 6;");
        icon.getChildren().add(badge);
        StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(badge, new Insets(0, 2, 3, 0));
    }

    // ================= 牌堆详情弹层 =================

    private void buildOverlay() {
        // 半透明遮罩，点它关闭
        Pane dim = new Pane();
        dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.72);");
        dim.setOnMouseClicked(e -> hideOverlay());

        // 中间卡片浏览区
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setMaxSize(900, 520);
        box.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 16; "
                + "-fx-padding: 18;");

        overlayTitle.setTextFill(Color.WHITE);
        overlayTitle.setFont(Font.font(22));
        overlayTitle.setStyle("-fx-font-weight: bold;");

        Label sub = new Label("按牌 id 排列（点遮罩或按关闭按钮退出）");
        sub.setTextFill(Color.rgb(148, 163, 184));
        sub.setFont(Font.font(13));

        ScrollPane scroll = new ScrollPane(overlayCards);
        scroll.setPrefSize(860, 360);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #0f172a; -fx-background-color: #0f172a;");

        Button close = new Button("关闭");
        close.setPrefWidth(120);
        close.setPrefHeight(36);
        close.setStyle("-fx-background-color: #475569; -fx-text-fill: white; "
                + "-fx-background-radius: 9; -fx-cursor: hand;");
        close.setOnAction(e -> hideOverlay());

        box.getChildren().addAll(overlayTitle, sub, scroll, close);
        StackPane.setAlignment(box, Pos.CENTER);
        box.setMaxWidth(900);
        box.setMaxHeight(520);
        overlay.getChildren().addAll(dim, box);
    }

    private void openPile(String title, List<Card> pile) {
        overlayTitle.setText(title + "（" + pile.size() + " 张）");
        overlayCards.getChildren().clear();

        // 不按抽取顺序，按牌的 id 排序显示
        List<Card> sorted = new ArrayList<>(pile);
        sorted.sort(Comparator.comparingInt(c -> c.id));

        if (sorted.isEmpty()) {
            Label empty = new Label("（空的）");
            empty.setTextFill(Color.rgb(148, 163, 184));
            empty.setFont(Font.font(16));
            overlayCards.getChildren().add(empty);
        } else {
            for (Card c : sorted) {
                overlayCards.getChildren().add(miniCard(c));
            }
        }
        overlay.setVisible(true);
    }

    private void hideOverlay() {
        overlay.setVisible(false);
    }

    // ================= 胜利奖励弹层 =================

    private void buildRewardOverlay() {
        Pane dim = new Pane();
        dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.78);");

        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #111827; -fx-background-radius: 18; "
                + "-fx-padding: 22 30 18 30;");

        Label title = new Label("战斗胜利！");
        title.setTextFill(Color.rgb(251, 191, 36));
        title.setFont(Font.font(26));
        title.setStyle("-fx-font-weight: bold;");

        Label sub = new Label("选择一张牌加入牌组");
        sub.setTextFill(Color.rgb(203, 213, 225));
        sub.setFont(Font.font(15));

        rewardBox.setAlignment(Pos.CENTER);

        Button skip = new Button("跳过（不加牌）");
        skip.setFont(Font.font(14));
        skip.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; "
                + "-fx-cursor: hand;");
        skip.setOnAction(e -> {
            if (rewardChosen) return;
            rewardChosen = true;
            closeRewardAndLeave();
        });

        box.getChildren().addAll(title, sub, rewardBox, skip);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); // 内容多大就多大，不铺满
        StackPane.setAlignment(box, Pos.CENTER);
        rewardOverlay.getChildren().addAll(dim, box);
    }

    /** 详情弹层里的小牌 */
    private VBox miniCard(Card c) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefSize(88, 132);
        card.setStyle("-fx-background-color: " + cardColor(c.kind) + "; -fx-background-radius: 10;"
                + " -fx-border-color: " + rarityBorderColor(c.kind) + "; -fx-border-width: 2; -fx-border-radius: 10;");

        Label name = new Label(c.kind.label);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(13));
        name.setStyle("-fx-font-weight: bold;");

        Label type = new Label(typeLabel(c.kind.type));
        type.setTextFill(Color.rgb(255, 255, 255, 0.7));
        type.setFont(Font.font(11));

        Label value = new Label(c.damage > 0 ? "伤害 " + c.damage : "格挡 " + c.block);
        value.setTextFill(Color.rgb(254, 243, 199));
        value.setFont(Font.font(12));

        card.getChildren().addAll(name, type, value);
        return card;
    }

    // ================= 玩家回合 =================

    private void startPlayerTurn() {
        turn++; // 进入新一回合
        playerTurn = true;
        playerBlock = 0;
        drawLocked = false; // 新回合重置抽牌锁定
        if (weakTurns > 0) weakTurns--;
        if (enemyVulnerable > 0) enemyVulnerable--;
        if (enemyWeakTurns > 0) enemyWeakTurns--;
        energy = 3;

        // 遗物：请假条 → 每回合多抽 1 张
        drawHand(5 + (hasRelic("请假条") ? 1 : 0));

        // 遗物：青铜怀表 → 战斗刚开始的那一回合获得 2 点格挡
        if (turn == 1 && hasRelic("青铜怀表")) {
            playerBlock = 2;
        }

        refreshAll();
    }

    private static final int HAND_LIMIT = 10; // 手牌上限

    private void drawHand(int n) {
        if (drawLocked) return; // 抽牌被锁定，直接返回
        for (int i = 0; i < n; i++) {
            if (hand.size() >= HAND_LIMIT) break; // 手牌已满，停止抽牌，剩余抽牌数作废
            Card c = drawOne();
            if (c == null) break;
            hand.add(c);
        }
    }

    /** 抽一张；抽牌堆空则把弃牌堆洗入抽牌堆 */
    private Card drawOne() {
        if (draw.isEmpty()) {
            if (discard.isEmpty()) return null;
            draw.addAll(discard);
            discard.clear();
            Collections.shuffle(draw, rnd);
        }
        return draw.remove(draw.size() - 1);
    }

    private void play(Card c) {
        if (!playerTurn || battleOver) return;
        if (c.cost > energy) return;

        energy -= c.cost;
        if (c.damage > 0) {
            for (int i = 0; i < c.hits; i++) {
                int dmg = c.damage + playerStrength;
                if (weakTurns > 0) dmg = dmg * 3 / 4;
                if (enemyVulnerable > 0) dmg = dmg * 3 / 2; // 易伤：+50%
                damageEnemy(dmg);
                if (battleOver) break;
            }
        }
        if (c.kind == Card.Kind.BASH) {
            enemyVulnerable += 2; // 痛击：给敌人 2 层易伤
        }
        if (c.kind == Card.Kind.LIGHTNING) {
            enemyVulnerable += 1; // 闪电霹雳：给敌人 1 层易伤
        }
        if (c.kind == Card.Kind.KINDLE) {
            playerStrength += 2; // 燃烧：获得 2 层力量
        }
        if (c.kind == Card.Kind.BLEED) {
            energy += 2; // 放血：获得 2 点能量
            player.damage(3); // 自己失去 3 点生命
            hud.refresh();
            if (player.hp() == 0) { playerDied(); return; }
        }
        if (c.kind == Card.Kind.RAGE) {
            energy += 2; // 盛怒：获得 2 点能量
        }
        if (c.kind == Card.Kind.OFFERING) {
            player.damage(6); // 祭品：自己失去 6 点生命
            energy += 2; // 获得 2 点能量
            hud.refresh();
            if (player.hp() == 0) { playerDied(); return; }
        }
        if (c.kind == Card.Kind.FORTIFY) {
            playerBlock *= 2; // 巩固：格挡翻倍
        }
        if (c.kind == Card.Kind.FOCUS) {
            drawHand(3); // 战斗专注：抽 3 张牌
            drawLocked = true; // 本回合不能再抽牌
        }
        if (c.kind == Card.Kind.SHOCKWAVE) {
            enemyWeakTurns += 4; // 震荡波：给敌人 4 层虚弱
            enemyVulnerable += 4; // 震荡波：给敌人 4 层易伤
        }
        if (c.block > 0) playerBlock += c.block;
        if (c.draw > 0) drawHand(c.draw); // 剑柄打击等：额外抽牌

        hand.remove(c);
        if (c.exhaust) {
            // 消耗：不进入弃牌堆，本场战斗无法再次使用
        } else {
            discard.add(c); // 打出 → 进弃牌堆
        }
        if (!battleOver) refreshAll();
    }

    private void damageEnemy(int dmg) {
        if (enemy.block > 0) {
            int absorb = Math.min(enemy.block, dmg);
            enemy.block -= absorb;
            dmg -= absorb;
        }
        enemy.hp = Math.max(0, enemy.hp - dmg);
        if (enemy.hp == 0) victory();
    }

    // ================= 怪物回合 =================

    private void endPlayerTurn() {
        if (!playerTurn || battleOver) return;
        playerTurn = false;

        discard.addAll(hand);
        hand.clear();
        refreshAll();

        PauseTransition pause = new PauseTransition(Duration.millis(700));
        pause.setOnFinished(e -> enemyAct());
        pause.play();
    }

    private void enemyAct() {
        if (battleOver || paused) return; // 暂停时先不动，等恢复

        enemy.block = 0; // 怪物格挡在自己回合开始清零

        Enemy.Step s = enemy.current();
        switch (s.intent) {
            case ATTACK -> {
                int dmg = s.value + enemy.power;
                if (enemyWeakTurns > 0) dmg = dmg * 3 / 4; // 敌人虚弱：伤害 ×0.75
                if (playerBlock > 0) {
                    int absorb = Math.min(playerBlock, dmg);
                    playerBlock -= absorb;
                    dmg -= absorb;
                }
                player.damage(dmg);
                hud.refresh();
                if (player.hp() == 0) { playerDied(); return; }
            }
            case DEFEND -> enemy.block += s.value;
            case BUFF   -> enemy.power += s.value;
            case WEAKEN -> weakTurns = Math.max(weakTurns, s.value);
        }
        enemy.advance();

        refreshAll();
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> {
            if (battleOver) return;
            if (paused) { pendingTurnStart = true; return; } // 暂停时错过：等恢复再开下一回合
            startPlayerTurn();
        });
        pause.play();
    }

    /** 暂停 / 恢复（切去只读地图时暂停；回到战斗时恢复） */
    public void setPaused(boolean p) {
        paused = p;
        if (!paused && !battleOver) {
            if (pendingTurnStart) {           // 有错过的下一回合 → 补开
                pendingTurnStart = false;
                startPlayerTurn();
            } else if (!playerTurn) {         // 怪物还没行动 → 补行动
                enemyAct();
            }
        }
    }

    // ================= 胜负 =================

    /** 怪物被击败 → 屏幕中央三选一奖励牌 */
    private void victory() {
        if (battleOver) return;
        battleOver = true;
        showReward();
    }

    /** 玩家阵亡：背景变暗、角色倒地，然后弹出死亡页 */
    private void playerDied() {
        if (diedShown) return;
        diedShown = true;
        battleOver = true;

        deadDim.setVisible(true); // 背景变暗，角色和怪物保持高亮（它在内容层下面）

        // 角色立绘缓缓“倒地”：旋转 + 下坠
        RotateTransition rotate = new RotateTransition(Duration.millis(900), playerPortrait);
        rotate.setToAngle(85);
        TranslateTransition fall = new TranslateTransition(Duration.millis(900), playerPortrait);
        fall.setToY(80);
        ParallelTransition both = new ParallelTransition(rotate, fall);
        both.setOnFinished(e -> {
            deadDim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.62);"); // 再压暗一档
            deathOverlay.setVisible(true);
        });
        both.play();
    }

    private void buildDeathOverlay() {
        Pane dim = new Pane();
        dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.45);");

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 16; "
                + "-fx-padding: 24 40 20 40;");

        Label title = new Label("你倒下了…");
        title.setTextFill(Color.rgb(248, 113, 113));
        title.setFont(Font.font(30));
        title.setStyle("-fx-font-weight: bold;");

        Label body = new Label("你被 " + enemy.name + " 击败，本局结束");
        body.setTextFill(Color.rgb(226, 232, 240));
        body.setFont(Font.font(16));

        Button back = new Button("返回主菜单");
        back.setFont(Font.font(16));
        back.setPrefSize(170, 42);
        back.setStyle("-fx-background-color: #475569; -fx-text-fill: white; "
                + "-fx-background-radius: 10; -fx-cursor: hand;");
        back.setOnAction(e -> onFinish.accept(false));

        box.getChildren().addAll(title, body, back);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(box, Pos.CENTER);
        deathOverlay.getChildren().addAll(dim, box);
    }

    /** 屏幕中央出现三张随机牌，点一张加入牌组（或跳过），然后离开战斗 */
    private void showReward() {
        rewardChosen = false;
        overlay.setVisible(false); // 如果开着牌堆浏览层，先关掉

        // 奖励池（不含打击、防御）+ 权重：数值越大越容易被抽到
        //   高权重: 痛击、铁斩波
        //   中权重: 剑柄打击、耸肩无视、放血
        //   低权重: 重锤、岿然不动
        List<Card> pool = List.of(
                Card.bash(), Card.sweep(),
                Card.pommelStrike(), Card.shrug(), Card.bleed(),
                Card.hammer(), Card.impregnable(),
                Card.doubleStrike(), Card.kindle(), Card.lightning(),
                Card.rage(), Card.offering(),
                Card.fortify(), Card.focus(),
                Card.shockwave());
        List<Integer> weights = List.of(
                4, 4,   // 痛击、铁斩波
                3, 3, 3, // 剑柄打击、耸肩无视、放血
                2, 2,   // 重锤、岿然不动
                4, 2, 4, // 双重打击、燃烧、闪电霹雳
                3, 2,   // 盛怒、祭品
                3, 3,   // 巩固、战斗专注
                3);     // 震荡波

        // 按权重无放回随机抽取 3 张不重复的牌
        List<Card> offers = new ArrayList<>();
        List<Card> remaining = new ArrayList<>(pool);
        List<Integer> remainingWeights = new ArrayList<>(weights);
        for (int i = 0; i < 3; i++) {
            int total = remainingWeights.stream().mapToInt(Integer::intValue).sum();
            int r = rnd.nextInt(total);
            int cumulative = 0;
            for (int j = 0; j < remaining.size(); j++) {
                cumulative += remainingWeights.get(j);
                if (r < cumulative) {
                    offers.add(remaining.get(j));
                    remaining.remove(j);
                    remainingWeights.remove(j);
                    break;
                }
            }
        }

        rewardBox.getChildren().clear();
        for (Card c : offers) {
            rewardBox.getChildren().add(buildRewardButton(c));
        }

        rewardOverlay.setVisible(true);
    }

    /** 奖励牌按钮：点它就加入牌组并离开战斗 */
    private Button buildRewardButton(Card c) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(10));
        card.setPrefSize(150, 205);
        card.setStyle("-fx-background-color: " + cardColor(c.kind) + "; -fx-background-radius: 14;"
                + " -fx-border-color: " + rarityBorderColor(c.kind) + "; -fx-border-width: 2; -fx-border-radius: 14;");

        Label cost = new Label("费用 " + c.cost);
        cost.setTextFill(Color.rgb(254, 243, 199));
        cost.setFont(Font.font(13));

        Label name = new Label(c.kind.label);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(18));
        name.setStyle("-fx-font-weight: bold;");

        Label type = new Label(typeLabel(c.kind.type));
        type.setTextFill(Color.rgb(255, 255, 255, 0.7));
        type.setFont(Font.font(13));

        Label value = new Label(
                c.damage > 0 && c.block > 0 ? "伤害 " + c.damage + "  格挡 " + c.block
                : c.damage > 0 ? "伤害 " + c.damage
                : c.block > 0 ? "格挡 " + c.block + (c.exhaust ? " 消耗" : "")
                : c.kind.desc);
        value.setTextFill(Color.WHITE);
        value.setFont(Font.font(14));

        Label desc = new Label(c.kind.desc);
        desc.setTextFill(Color.rgb(226, 232, 240));
        desc.setFont(Font.font(11));
        desc.setWrapText(true);

        card.getChildren().addAll(cost, name, type, value, desc);

        Button btn = new Button();
        btn.setGraphic(card);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            if (rewardChosen) return;
            rewardChosen = true;
            player.deck.add(c); // 加入牌组
            closeRewardAndLeave();
        });
        return btn;
    }

    /** 选完（或跳过）→ 隐藏奖励层，告诉外面战斗结束 */
    private void closeRewardAndLeave() {
        rewardOverlay.setVisible(false);
        onFinish.accept(true);
    }

    // ================= 刷新 =================

    private void refreshAll() {
        // 中间：第几回合
        turnLabel.setText("第 " + turn + " 回合");

        // 左下抽牌堆上方：能量
        energyLabel.setText("能量 " + energy + " / 3");

        // ---- 角色(左)：血条数字/数字 + 金属框 + 盾牌 + buff/debuff ----
        double pRatio = (double) player.hp() / player.maxHp;
        pHpText.setText(player.hp() + " / " + player.maxHp);
        pHpFill.setPrefWidth(Math.max(0, 240.0 * pRatio));
        pHpFill.setStyle("-fx-background-color: " + (pRatio < 0.4 ? "#ef4444" : "#22c55e")
                + "; -fx-background-radius: 9;");
        pHpWrap.setStyle(frameStyle(playerBlock > 0));
        pShield.setVisible(playerBlock > 0);
        pShieldNum.setText(String.valueOf(playerBlock));

        pChips.getChildren().clear();
        if (weakTurns > 0) {
            pChips.getChildren().add(statusChip("弱", weakTurns, "#7c3aed",
                    "虚弱 ×" + weakTurns + "：你造成的伤害 ×0.75"));
        }
        if (playerStrength > 0) {
            pChips.getChildren().add(statusChip("力", playerStrength, "#f59e0b",
                    "力量 +" + playerStrength + "：每段攻击伤害增加"));
        }

        // ---- 怪物(右)：同上 + 意图图标 ----
        eName.setText(enemy.name);
        double eRatio = (double) enemy.hp / enemy.maxHp;
        eHpText.setText(enemy.hp + " / " + enemy.maxHp);
        eHpFill.setPrefWidth(Math.max(0, 240.0 * eRatio));
        eHpFill.setStyle("-fx-background-color: #dc2626; -fx-background-radius: 9;");
        eHpWrap.setStyle(frameStyle(enemy.block > 0));
        eShield.setVisible(enemy.block > 0);
        eShieldNum.setText(String.valueOf(enemy.block));

        eChips.getChildren().clear();
        if (enemy.power > 0) {
            eChips.getChildren().add(statusChip("力", enemy.power, "#f59e0b",
                    "力量 +" + enemy.power + "：攻击伤害增加"));
        }
        if (enemyVulnerable > 0) {
            eChips.getChildren().add(statusChip("伤", enemyVulnerable, "#dc2626",
                    "易伤 " + enemyVulnerable + " 回合：承受伤害 ×1.5"));
        }
        if (enemyWeakTurns > 0) {
            eChips.getChildren().add(statusChip("弱", enemyWeakTurns, "#7c3aed",
                    "虚弱 " + enemyWeakTurns + " 回合：造成伤害 ×0.75"));
        }
        refreshIntent();

        // 手牌
        handBox.getChildren().clear();
        for (Card c : hand) {
            handBox.getChildren().add(buildCardButton(c));
        }
        emptyHandLabel.setVisible(hand.isEmpty());
        pilesInfo.setText("抽牌堆 " + draw.size() + "  ·  弃牌堆 " + discard.size());

        // 牌堆图标下标
        drawBadge.setText(String.valueOf(draw.size()));
        drawBadge.setVisible(draw.size() > 0);
        discardBadge.setText(String.valueOf(discard.size()));
        discardBadge.setVisible(discard.size() > 0);

        // 结束回合按钮：只在玩家回合可用
        endTurnBtn.setDisable(!playerTurn || battleOver);
    }

    private void refreshHandEnabled() {
        for (var node : handBox.getChildren()) {
            if (node instanceof Button btn) {
                btn.setDisable(!playerTurn || battleOver);
            }
        }
    }

    private static String intentColor(Enemy.Intent i) {
        return switch (i) {
            case ATTACK -> "#dc2626";
            case DEFEND -> "#0284c7";
            case BUFF   -> "#d97706";
            case WEAKEN -> "#7c3aed";
        };
    }

    private static String cardColor(Card.Kind k) {
        return switch (k) {
            case STRIKE -> "#991b1b";
            case DEFEND -> "#1d4ed8";
            case BASH   -> "#b45309";
            case SWEEP  -> "#4338ca";
            case POMMEL -> "#92400e";
            case SHRUG  -> "#475569";
            case BLEED  -> "#881337";
            case HAMMER -> "#7f1d1d";
            case IMPREGNABLE -> "#1e3a5f";
            case DOUBLE_STRIKE -> "#c2410c";
            case KINDLE -> "#9a3412";
            case LIGHTNING -> "#ca8a04";
            case RAGE -> "#7e22ce";
            case OFFERING -> "#581c87";
            case FORTIFY -> "#0369a1";
            case FOCUS -> "#065f46";
            case SHOCKWAVE -> "#6d28d9";
        };
    }

    /** 稀有度边框颜色：权重 4=灰色(白卡)，权重 3=蓝色(蓝卡)，权重 2=金色(金卡) */
    private static String rarityBorderColor(Card.Kind k) {
        return switch (k.weight) {
            case 4 -> "#9ca3af"; // 灰色（白卡/普通）
            case 3 -> "#3b82f6"; // 蓝色（蓝卡/罕见）
            case 2 -> "#f59e0b"; // 金色（金卡/稀有）
            default -> "#9ca3af";
        };
    }

    /** 卡牌类型文字标签 */
    private static String typeLabel(Card.Type t) {
        return switch (t) {
            case ATTACK -> "攻击";
            case SKILL  -> "技能";
            case POWER  -> "能力";
        };
    }

    private Button buildCardButton(Card c) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(8));
        card.setPrefSize(116, 152);
        card.setStyle("-fx-background-color: " + cardColor(c.kind) + "; -fx-background-radius: 12;"
                + " -fx-border-color: " + rarityBorderColor(c.kind) + "; -fx-border-width: 2; -fx-border-radius: 12;");

        Label cost = new Label(String.valueOf(c.cost));
        cost.setTextFill(Color.WHITE);
        cost.setFont(Font.font(15));
        cost.setStyle("-fx-font-weight: bold;");

        Label name = new Label(c.kind.label);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(14));
        name.setStyle("-fx-font-weight: bold;");

        Label type = new Label(typeLabel(c.kind.type));
        type.setTextFill(Color.rgb(255, 255, 255, 0.65));
        type.setFont(Font.font(11));

        String valueText;
        if (c.damage > 0 && c.block > 0) {
            valueText = "伤害 " + c.damage + "  格挡 " + c.block;
        } else if (c.damage > 0 && c.hits > 1) {
            valueText = "伤害 " + c.damage + "×" + c.hits;
        } else if (c.damage > 0) {
            valueText = c.kind.desc;
        } else if (c.block > 0) {
            valueText = "格挡 " + c.block;
            if (c.exhaust) valueText += " 消耗";
        } else {
            valueText = c.kind.desc;
        }
        Label value = new Label(valueText);
        value.setTextFill(Color.rgb(254, 243, 199));
        value.setFont(Font.font(11));

        card.getChildren().addAll(cost, name, type, value);

        Button btn = new Button();
        btn.setGraphic(card);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
        btn.setDisable(c.cost > energy || !playerTurn || battleOver);
        btn.setOnAction(e -> {
            play(c);
            refreshHandEnabled();
        });
        return btn;
    }
}
