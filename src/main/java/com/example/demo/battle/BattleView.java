package com.example.demo.battle;

import com.example.demo.card.BattleState;
import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;
import com.example.demo.card.CardPlay;
import com.example.demo.character.Player;
import com.example.demo.character.RelicFun;
import com.example.demo.enemy.Enemy;
import com.example.demo.sound.SoundFx;
import com.example.demo.view.BattleUiFactory;
import com.example.demo.view.DeathOverlay;
import com.example.demo.view.CardFlyFx;
import com.example.demo.view.PileOverlay;
import com.example.demo.view.RewardOverlay;
import com.example.demo.view.SpriteAnimator;
import com.example.demo.view.RunHud;

import javafx.animation.*;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 回合制战斗界面（纯战斗逻辑 + 面板拼装）。
 *
 * 卡牌渲染委托 {@link CardFaceView}，静态 UI 构件委托 {@link BattleUiFactory}，
 * 弹层（牌堆浏览/奖励/死亡）委托 view 包中各自的 Overlay 类。
 */
public class BattleView extends javafx.scene.layout.StackPane implements BattleState {
    private StackPane enemyPortrait;
    private ImageView enemyPortraitImg;
    private double enemyPortraitSize = 210;  // 敌人立绘尺寸（BOSS 放大）
    private SpriteAnimator playerAnim;
    private SpriteAnimator enemyAnim;
    private long lastFrameTime = 0;
    private final AnimationTimer gameTimer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if(lastFrameTime == 0){
                lastFrameTime = now;
                return;
            }
            double deltaSec = (now - lastFrameTime) / 1_000_000_000.0;
            lastFrameTime = now;

            if(!battleOver && !paused){
                playerAnim.update(deltaSec);
                enemyAnim.update(deltaSec);
            }
        }
    };
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
    private int reflectTurns = 0;
    private int enemyVulnerable = 0;
    private int enemyWeak = 0;
    private int pendingStrengthLoss = 0;
    private boolean noDrawThisTurn = false;

    /** 已激活的能力牌层数（能力牌可叠加，效果按层数累加；LinkedHashMap 保持登记顺序） */
    private final Map<Card.Kind, Integer> powerStacks = new LinkedHashMap<>();

    private record PowerBadge(String glyph, String color, String tip) {
    }

    private int playerStrength = 0;
    private boolean playerTurn = true;
    private boolean battleOver = false;
    private boolean paused = false;
    private boolean pendingTurnStart = false;
    private boolean playedCardThisTurn = false;
    private boolean firstAttackUsed = false;
    private boolean firstDamageTriggered = false;
    private int attackCardsPlayedThisTurn = 0;
    private boolean fanBonusApplied = false;
    private int skillCardsPlayedThisTurn = 0;
    private boolean letterOpenerUsed = false;
    private boolean noodleBonusUsed = false;

    // ===== 卡牌飞行演出（抽牌 / 进弃牌堆 / 消耗） =====
    /** 手牌卡面宽度（与 buildCardButton 保持一致） */
    private static final double HAND_FACE_W = 128;
    private static final double DRAW_FLY_MS = 280;     // 抽牌：单张飞入时长
    private static final double DRAW_STAGGER_MS = 85;  // 连抽时每张之间的错峰
    private static final double DISCARD_FLY_MS = 300;  // 飞入弃牌堆时长
    private static final double DUMP_STAGGER_MS = 55;  // 回合结束整手弃牌的错峰
    private static final double EXHAUST_FX_MS = 420;   // 消耗演出时长

    /** 本轮新抽到、还没播“飞入”演出的牌（refreshAll 末尾统一消费） */
    private final List<Card> pendingDrawFx = new ArrayList<>();

    /**
     * 正在等待 / 正在飞入的牌：这些牌的**真身**必须一直隐身，直到各自的 ghost 落位。
     * <p>和 {@link #pendingDrawFx} 的区别：pendingDrawFx 在 flushDrawFx 里就清空了，
     * 而本集合要一直留到 ghost 飞完 —— 因为错峰飞行期间手牌可能被 refreshAll 重建，
     * 重建出来的新按钮默认是可见的，会把还没起飞的牌又亮出来。
     */
    private final Set<Card> drawFxInFlight = new LinkedHashSet<>();

    /** 手牌按钮的基础 inline 样式 */
    private static final String CARD_BTN_STYLE =
            "-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;";

    /**
     * 手牌按钮的隐身样式。
     *
     * ★ 必须用 inline {@code -fx-opacity}，不能只用 {@code setOpacity(0)}：
     * 抽牌演出期间 {@code animating=true} 会把所有手牌 {@code setDisable(true)}，
     * 而 modena 对 {@code .button:disabled} 有 {@code -fx-opacity: 0.4}，
     * CSS 优先级高于代码 setter，会把 setOpacity(0) 盖回去 ——
     * 那正是“抽牌前先看到半透明真牌”的原因。
     */
    private static final String CARD_BTN_HIDDEN_STYLE = CARD_BTN_STYLE + " -fx-opacity: 0;";

    /**
     * 落位瞬间用的样式：全亮。
     * 也要走 inline —— 演出还没结束，按钮仍是 :disabled，
     * 一撤掉 inline 就会被 CSS 的 0.4 压暗，出现“亮一帧又变暗”的闪烁。
     */
    private static final String CARD_BTN_LANDED_STYLE = CARD_BTN_STYLE + " -fx-opacity: 1;";

    /** 抽牌演出期间锁住出牌与“结束回合”，避免演出和玩家操作打架 */
    private boolean animating = false;

    // ===== 动画已统一委托给 SpriteAnimator =====

    // ================= UI =================
    // 角色(左)
    private final Label pName = new Label(Player.CHARACTER_NAME);
    private final Label pHpText = new Label();
    private final javafx.scene.layout.Region pHpFill = new javafx.scene.layout.Region();
    private final javafx.scene.layout.StackPane pHpWrap = new javafx.scene.layout.StackPane();
    private final javafx.scene.layout.StackPane pShield = new javafx.scene.layout.StackPane();
    private final Label pShieldNum = new Label();
    private final FlowPane pChips = new FlowPane(4, 4);
    // 怪物(右)
    private final Label eName = new Label();
    private final javafx.scene.layout.StackPane eIntentIcon = new javafx.scene.layout.StackPane();
    private final Label eIntentNum = new Label();
    private Tooltip eIntentTip; // 意图悬停描述（只建一次）
    private String currentIntentTip = ""; // 意图描述文字（供屏幕描述条用）
    private final Label hoverBar = new Label(); // 悬停描述条（屏幕上方显示）
    private final Label eHpText = new Label();
    private final javafx.scene.layout.Region eHpFill = new javafx.scene.layout.Region();
    private final javafx.scene.layout.StackPane eHpWrap = new javafx.scene.layout.StackPane();
    private final javafx.scene.layout.StackPane eShield = new javafx.scene.layout.StackPane();
    private final Label eShieldNum = new Label();
    private final FlowPane eChips = new FlowPane(4, 4);
    // 左下角仪式状态栏
    private final Label ritualStatus = new Label();
    // 破甲状态栏（BOSS 二阶段）
    private final Label armorBreakStatus = new Label();
    // 中下
    private int turn = 0;
    private final Label turnLabel = new Label();
    private final Label energyLabel = new Label();
    private final Label pilesInfo = new Label();
    private final HBox handBox = new HBox(10);
    private final Button endTurnBtn = new Button("结束回合");
    private Label emptyHandLabel;
    // 牌堆图标 + 计数下标
    private final Label drawBadge = new Label();
    private final Label discardBadge = new Label();
    // 牌堆图标字形色：和卡面卡名 / 牌组图标「牌」统一用暖深咖 #4a3624
    private final javafx.scene.layout.StackPane drawIcon = BattleUiFactory.pileIcon("抽", "#4a3624");
    private final javafx.scene.layout.StackPane discardIcon = BattleUiFactory.pileIcon("弃", "#4a3624");
    // 弹层（委托给 view 包）
    private final PileOverlay pileOverlay = new PileOverlay();
    private RewardOverlay rewardOverlay;
    private final DeathOverlay deathOverlay;
    // 死亡演出
    private StackPane playerPortrait;
    private VBox leftCol;
    private VBox rightCol;
    private final Ellipse playerShadow = new Ellipse();
    private final Ellipse enemyShadow = new Ellipse();
    private final Pane deadDim = new Pane();
    private boolean diedShown = false;

    private Image battleBg;

    public BattleView(Player player, RunHud hud, Enemy enemy, Consumer<Boolean> onFinish,
                      boolean bossBattle) {
        this.player = player;
        this.hud = hud;
        this.enemy = enemy;
        this.onFinish = onFinish;
        this.deathOverlay = new DeathOverlay(enemy.name, () -> onFinish.accept(false));
        this.rewardOverlay = new RewardOverlay(() -> { rewardOverlay.hide(); onFinish.accept(true); });

        // 战斗背景
        battleBg = BattleUiFactory.loadImage(bossBattle ? "icons/boss.png" : "icons/default.png");
        if (battleBg != null) {
            setBackground(BattleUiFactory.makeCoverBackground(battleBg));
        } else {
            setStyle("-fx-background-color: linear-gradient(to bottom, #191511, #23201c);");
        }

        deadDim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.5);");
        deadDim.setVisible(false);
        getChildren().add(deadDim);

        javafx.scene.layout.BorderPane main = new javafx.scene.layout.BorderPane();
        main.setStyle("-fx-background-color: transparent;");

        HBox fighters = new HBox(10);
        fighters.setAlignment(Pos.CENTER);
        fighters.setFillHeight(false); // 列不随高度拉伸，避免“画面拉伸”感

        VBox left = buildLeftPanel();
        leftCol = left;
        VBox middle = buildMiddlePanel();
        VBox right = buildRightPanel();
        rightCol = right;
        fighters.getChildren().addAll(left, middle, right);
        main.setCenter(fighters);

        main.setBottom(buildHandPanel());
        main.setPadding(new Insets(8, 10, 8, 10));

        getChildren().add(main);

        // ---- 牌堆图标 ----
        drawIcon.setOnMouseClicked(e -> pileOverlay.showPile("抽牌堆", draw));
        discardIcon.setOnMouseClicked(e -> pileOverlay.showPile("弃牌堆", discard));
        StackPane.setAlignment(drawIcon, Pos.BOTTOM_LEFT);
        StackPane.setMargin(drawIcon, new Insets(0, 0, 16, 22));
        StackPane.setAlignment(discardIcon, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(discardIcon, new Insets(0, 22, 16, 0));
        getChildren().addAll(drawIcon, discardIcon);

        BattleUiFactory.attachBadge(drawIcon, drawBadge);
        BattleUiFactory.attachBadge(discardIcon, discardBadge);
        drawBadge.setVisible(false);
        discardBadge.setVisible(false);

        energyLabel.setTextFill(Color.rgb(251, 191, 36));
        energyLabel.setFont(Font.font(20));
        StackPane.setAlignment(energyLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(energyLabel, new Insets(0, 0, 128, 22));
        getChildren().add(energyLabel);

        endTurnBtn.setFont(Font.font(17));
        endTurnBtn.setPrefSize(160, 44);
        endTurnBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; "
                + "-fx-background-radius: 12; -fx-cursor: hand;");
        endTurnBtn.setOnAction(e -> endPlayerTurn());
        StackPane.setAlignment(endTurnBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(endTurnBtn, new Insets(0, 116, 132, 0));
        getChildren().add(endTurnBtn);

        // 悬停描述条：鼠标停在意图/buff/debuff 图标上时，在这里显示说明文字
        hoverBar.setTextFill(Color.WHITE);
        hoverBar.setFont(Font.font(14));
        hoverBar.setMouseTransparent(true);
        hoverBar.setStyle("-fx-background-color: rgba(15,23,42,0.88); "
                + "-fx-background-radius: 8; -fx-padding: 4 12 4 12;");
        hoverBar.setVisible(false);
        StackPane.setAlignment(hoverBar, Pos.TOP_CENTER);
        StackPane.setMargin(hoverBar, new Insets(6, 0, 0, 0));
        getChildren().add(hoverBar);
        BattleUiFactory.hoverConsumer = msg -> {
            hoverBar.setText(msg == null ? "" : msg);
            hoverBar.setVisible(msg != null && !msg.isEmpty());
        };

        // 弹层
        getChildren().addAll(pileOverlay, rewardOverlay, deathOverlay);

        // 开局
        draw.addAll(player.deck);
        Collections.shuffle(draw, rnd);

        RelicFun.onBattleStart(player);
        playerStrength += RelicFun.extraStrength(player);
        // 忘情牛肉面：战斗开始时获得 3 点力量，仅第一回合有效
        if (RelicFun.hasRelic(player, "忘情牛肉面")) {
            playerStrength += 3;
        }
        hud.refresh();
        initAnimators();
        gameTimer.start();
        SoundFx.playAny(enemyOink()); // 怪物登场音效（BOSS 用鱼龙叫，普通怪用普通猪叫）
        startPlayerTurn();
        // 牛来：战斗开始时对敌人造成 3 点伤害
        if (RelicFun.hasRelic(player, "牛来")) {
            enemy.hp = Math.max(0, enemy.hp - 3);
            if (enemy.hp == 0) victory();
        }
    }

    // ================= 动画（统一委托 SpriteAnimator） =================

    /** 初始化两个立绘动画器，在面板建好后调用 */
    private void initAnimators() {
        playerAnim = new SpriteAnimator(playerPortrait, true, 90, -32);
        enemyAnim = new SpriteAnimator(enemyPortrait, false, 42, -32);
    }


    // ================= 面板搭建 =================

    private VBox buildLeftPanel() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(360);

        pName.setTextFill(Color.WHITE);
        pName.setFont(Font.font(26));
        pName.setStyle("-fx-font-weight: bold;");

        playerPortrait = new StackPane();
        playerPortrait.setPrefSize(210, 210);
        playerPortrait.setMaxSize(210, 210);
        ImageView pImg = new ImageView(new Image(
                getClass().getResourceAsStream("/com/example/demo/icons/char.png")));
        pImg.setPreserveRatio(true);
        pImg.setFitWidth(210);
        pImg.setFitHeight(210);
        pImg.setMouseTransparent(true);
        playerPortrait.getChildren().add(pImg);

        BattleUiFactory.hpWrap(pHpWrap, pHpFill, pHpText, 240, "#22c55e");
        pShieldNum.setTextFill(Color.WHITE);
        pShieldNum.setFont(Font.font(13));
        pShieldNum.setStyle("-fx-font-weight: bold;");
        BattleUiFactory.shield(pShield, pShieldNum);
        Tooltip.install(pShield, new Tooltip("格挡：吸收等量伤害，下回合开始清除"));
        BattleUiFactory.attachHover(pShield, "格挡：吸收等量伤害，下回合开始清除");

        HBox cluster = new HBox(6);
        cluster.setAlignment(Pos.CENTER_LEFT);
        cluster.getChildren().addAll(pShield, pHpWrap);

        pChips.setPrefWrapLength(286);
        pChips.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(pName, playerPortrait, cluster, pChips);
        return box;
    }

    private VBox buildRightPanel() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(360);

        eName.setTextFill(Color.WHITE);
        eName.setFont(Font.font(26));
        eName.setStyle("-fx-font-weight: bold;");

        eIntentIcon.setPrefSize(46, 46);
        eIntentIcon.setMaxSize(46, 46);
        eIntentIcon.setPickOnBounds(true);  // 整块都能触发悬停
        // 意图提示只建一次，之后只改文字（避免刷新时悬停被打断）
        eIntentTip = new Tooltip();
        Tooltip.install(eIntentIcon, eIntentTip);
        // 悬停时同时把描述写到屏幕描述条上
        eIntentIcon.setOnMouseEntered(e -> {
            if (BattleUiFactory.hoverConsumer != null) {
                BattleUiFactory.hoverConsumer.accept(currentIntentTip);
            }
        });
        eIntentIcon.setOnMouseExited(e -> {
            if (BattleUiFactory.hoverConsumer != null) {
                BattleUiFactory.hoverConsumer.accept("");
            }
        });
        eIntentNum.setTextFill(Color.WHITE);
        eIntentNum.setFont(Font.font(22));
        eIntentNum.setStyle("-fx-font-weight: bold;");
        eIntentNum.setVisible(false);
        HBox intentRow = new HBox(8);
        intentRow.setAlignment(Pos.CENTER);
        intentRow.getChildren().addAll(eIntentIcon, eIntentNum);

        enemyPortraitSize = enemy.getPortraitSize();
        StackPane portrait;
        if (enemy.hasPortrait) {
            enemyPortraitImg = new ImageView();
            Image img = new Image(getClass().getResourceAsStream("/com/example/demo/portrait/" + enemy.getPortraitName() + ".png"));
            enemyPortraitImg.setImage(img);
            enemyPortraitImg.setFitWidth(enemyPortraitSize);
            enemyPortraitImg.setFitHeight(enemyPortraitSize);
            enemyPortraitImg.setPreserveRatio(true);
            portrait = new StackPane(enemyPortraitImg);
        } else {
            portrait = BattleUiFactory.portrait(enemy.name.substring(0, 1),
                    "radial-gradient(center 35% 30%, radius 100%, #6b7280, #1f2937);");
        }
        portrait.setPrefSize(enemyPortraitSize, enemyPortraitSize);
        portrait.setMaxSize(enemyPortraitSize, enemyPortraitSize);
        enemyPortrait = portrait;

        BattleUiFactory.hpWrap(eHpWrap, eHpFill, eHpText, 240, "#dc2626");
        eShieldNum.setTextFill(Color.WHITE);
        eShieldNum.setFont(Font.font(13));
        eShieldNum.setStyle("-fx-font-weight: bold;");
        BattleUiFactory.shield(eShield, eShieldNum);
        Tooltip.install(eShield, new Tooltip("格挡：吸收等量伤害"));
        BattleUiFactory.attachHover(eShield, "格挡：吸收等量伤害");

        HBox cluster = new HBox(6);
        cluster.setAlignment(Pos.CENTER_LEFT);
        cluster.getChildren().addAll(eShield, eHpWrap);

        // 仪式状态栏（敌怪血条左下角，青底白字）
        ritualStatus.setTextFill(Color.WHITE);
        ritualStatus.setFont(Font.font(14));
        ritualStatus.setStyle("-fx-font-weight: bold; -fx-background-color: #0891b2; "
                + "-fx-background-radius: 10; -fx-padding: 4 10 4 10;");
        ritualStatus.setVisible(false);
        HBox ritualRow = new HBox();
        ritualRow.setAlignment(Pos.CENTER_LEFT);
        ritualRow.setPrefWidth(286);
        ritualRow.getChildren().add(ritualStatus);

        // 破甲状态栏（BOSS 二阶段，灰底白字）
        armorBreakStatus.setTextFill(Color.WHITE);
        armorBreakStatus.setFont(Font.font(14));
        armorBreakStatus.setStyle("-fx-font-weight: bold; -fx-background-color: #9ca3af; "
                + "-fx-background-radius: 10; -fx-padding: 4 10 4 10;");
        armorBreakStatus.setVisible(false);
        HBox armorBreakRow = new HBox();
        armorBreakRow.setAlignment(Pos.CENTER_LEFT);
        armorBreakRow.setPrefWidth(286);
        armorBreakRow.getChildren().add(armorBreakStatus);

        eChips.setPrefWrapLength(286);
        eChips.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(eName, intentRow, enemyPortrait, cluster, ritualRow, armorBreakRow, eChips);
        return box;
    }

    private VBox buildMiddlePanel() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(220);

        turnLabel.setTextFill(Color.rgb(248, 250, 252));
        turnLabel.setFont(Font.font(24));
        turnLabel.setStyle("-fx-font-weight: bold;");

        Label sub = new Label("");
        sub.setTextFill(Color.rgb(120, 113, 108));
        sub.setFont(Font.font(14));

        box.getChildren().addAll(turnLabel, sub);
        return box;
    }

    private VBox buildHandPanel() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(6, 0, 4, 0));

        pilesInfo.setTextFill(Color.rgb(252, 240, 215));
        pilesInfo.setFont(Font.font(13));

        handBox.setAlignment(Pos.CENTER);
        emptyHandLabel = new Label("手牌已空 — 点击\u201c结束回合\u201d");
        emptyHandLabel.setTextFill(Color.rgb(148, 163, 184));
        emptyHandLabel.setFont(Font.font(15));

        box.getChildren().addAll(handBox, emptyHandLabel, pilesInfo);
        return box;
    }

    /** 怪物意图刷新 */
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
                if (enemyWeak > 0) number = number * 3 / 4;
                tip = "意图·攻击：将对玩家造成 " + number + " 伤害"
                        + (enemyWeak > 0 ? "（虚弱 ×0.75）" : "");
            }
            case DEFEND -> {
                glyph = "防";
                color = "#0284c7";
                tip = "意图·防御：在下回合获得格挡";
            }
            case BUFF -> {
                glyph = "强";
                color = "#d97706";
                tip = "意图·强化：这个敌人将要为自己施加增益效果";
            }
            case REFLECT ->{
                glyph="反";
                color ="#9400D3";
                tip="意图·反弹伤害："+s.value+" 回合内，当你攻击时，受到造成伤害30%的伤害";
            }
            case RITUAL -> {
                glyph = "祭";
                color = "#8b5cf6";
                tip = "意图·仪式：每回合开始力量 +" + s.value;
            }
            default -> {
                glyph = "弱";
                color = "#7c3aed";
                tip = "意图·弱化：这个敌人将要对你施加减益效果";
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
        eIntentNum.setVisible(s.intent == Enemy.Intent.ATTACK);
        if (eIntentTip != null) eIntentTip.setText(tip); // 只更新文字，保证悬停稳定
        currentIntentTip = tip;                          // 供屏幕描述条使用
    }

    // ================= 玩家回合 =================

    private static final int HAND_LIMIT = 10;

    private void startPlayerTurn() {
        turn++;
        playerTurn = true;
        playerBlock = 0;
        if (weakTurns > 0) weakTurns--;
        if (enemyVulnerable > 0) enemyVulnerable--;
        noDrawThisTurn = false;
        energy = 3;
        // 古茶具套装：篝火休息后下一场战斗第一回合 +2 能量
        if (turn == 1) {
            energy += RelicFun.teaSetEnergy(player);
        }
        // 孙子兵法：上回合未出牌则获得 1 点额外能量
        if (!playedCardThisTurn && turn > 1 && RelicFun.hasRelic(player, "孙子兵法")) {
            energy += 1;
        }
        playedCardThisTurn = false;
        attackCardsPlayedThisTurn = 0;
        fanBonusApplied = false;
        skillCardsPlayedThisTurn = 0;
        letterOpenerUsed = false;

        // 残暴：按层数每回合开始失去等量生命，随后多抽等量张（可叠加）
        int brutality = powerStacks.getOrDefault(Card.Kind.BRUTALITY, 0);
        if (brutality > 0 && loseHp(brutality, true)) return;
        
        // 恶魔形态：每回合增加两点力量
        int demonForm = powerStacks.getOrDefault(Card.Kind.DEMON_FORM, 0);
        if (demonForm > 0) gainStrength(demonForm*2);

        drawHand(5 + (RelicFun.hasRelic(player, "请假条") ? 1 : 0) + brutality);

        // 英雄宝典：战斗开始时增加一张免费能力牌
        if (turn == 1 && RelicFun.hasRelic(player, "英雄宝典")) {
            hand.add(Card.freePower());
        }

        playerBlock += RelicFun.startBlock(player, turn);

        // refreshAll() 末尾的 flushDrawFx() 会消费 pendingDrawFx 并排好飞入演出
        refreshAll();
    }

    private void drawHand(int n) {
        if (noDrawThisTurn) return; // 战斗专注：本回合禁止再抽牌
        for (int i = 0; i < n; i++) {
            if (hand.size() >= HAND_LIMIT) break;
            Card c = drawOne();
            if (c == null) break;
            hand.add(c);
            pendingDrawFx.add(c); // 记下来，refreshAll 时补一段“从抽牌堆飞入”的演出
        }
    }

    private Card drawOne() {
        if (draw.isEmpty()) {
            if (discard.isEmpty()) return null;
            draw.addAll(discard);
            discard.clear();
            Collections.shuffle(draw, rnd);
        }
        return draw.remove(draw.size() - 1);
    }

    // ================= 出牌（规则结算委托 CardPlay） =================

    /** 出牌入口薄壳：具体结算规则见 {@link CardPlay#play(Card, BattleState)}。 */
    private void play(Card c) {
        if (!playerTurn || battleOver || animating) return;
        if (c.cost < 0 || c.cost > energy) return;
        // 先移出手牌进入“打出中”状态再结算：为效果生成的牌腾出槽位，
        // 且结算中的抽牌不会把刚打出的牌从弃牌堆洗回。最终去向由 onCardPlayed 决定。
        hand.remove(c);
        CardPlay.play(c, this);
    }

    // ================= 卡牌飞行演出 =================
    //
    // 做法：数据照常即时结算（抽/弃/消耗都不变），只是在“牌离开手牌”的瞬间
    // 复制一张只用于观看的卡面（ghost）挂在棋盘上层，让它从起点飞到终点，
    // 飞完就摘掉。所以演出永远不会影响战斗逻辑，中途被打断也不会出错。

    /** 手牌里这张牌对应的按钮（用 Card 身份比较，同名牌互不干扰） */
    private Node findCardNode(Card c) {
        for (Node n : handBox.getChildren()) {
            if (n.getUserData() == c) return n;
        }
        return null;
    }

    /** 节点中心 → BattleView 局部坐标（节点还没挂上场景时返回 null） */
    private Point2D centerInLocal(Node n) {
        if (n == null || n.getScene() == null) return null;
        Bounds b = n.localToScene(n.getLayoutBounds());
        if (b == null) return null;
        return sceneToLocal(b.getMinX() + b.getWidth() / 2, b.getMinY() + b.getHeight() / 2);
    }

    /** 手牌中某张牌当前的中心点（拿不到就返回 null） */
    private Point2D centerOfCardNode(Card c) {
        return centerInLocal(findCardNode(c));
    }

    /** 造一张只用来飞的卡面：不吃鼠标事件，也不参与父容器布局 */
    private static StackPane ghostOf(Card c) {
        StackPane g = CardFaceView.buildAt(c, HAND_FACE_W);
        g.setMouseTransparent(true);
        g.setManaged(false);
        return g;
    }

    /** 把飞行卡面挂到“棋盘之上、弹窗之下”，并摆在指定中心点上 */
    private void mountGhost(StackPane g, Point2D center) {
        double w = HAND_FACE_W, h = HAND_FACE_W * 1.4;
        g.resize(w, h);
        g.relocate(center.getX() - w / 2, center.getY() - h / 2);
        int idx = getChildren().indexOf(pileOverlay);
        if (idx < 0) getChildren().add(g);
        else getChildren().add(idx, g);
    }

    private void unmountGhost(Node g) {
        getChildren().remove(g);
    }

    /**
     * 等“已挂上场景 + 布局完成”后再执行。
     * 构造期会直接调 startPlayerTurn()，那时场景还没挂上、节点也还没布局，
     * 拿不到坐标，必须推迟到第一帧之后。
     */
    private void afterLayout(Runnable r) {
        if (getScene() == null) {
            sceneProperty().addListener(new ChangeListener<Scene>() {
                @Override
                public void changed(ObservableValue<? extends Scene> o, Scene oldS, Scene newS) {
                    if (newS == null) return;
                    sceneProperty().removeListener(this);
                    Platform.runLater(() -> runLaidOut(r));
                }
            });
            return;
        }
        Platform.runLater(() -> runLaidOut(r));
    }

    /** 强制走一遍 CSS + 布局，保证接下来读到的坐标是最新的 */
    private void runLaidOut(Runnable r) {
        Scene s = getScene();
        if (s == null) return;
        if (s.getRoot() != null) {
            s.getRoot().applyCss();
            s.getRoot().layout();
        }
        r.run();
    }

    private static void delay(double ms, Runnable r) {
        if (ms <= 0) { r.run(); return; }
        PauseTransition wait = new PauseTransition(Duration.millis(ms));
        wait.setOnFinished(e -> r.run());
        wait.play();
    }

    // ---- 演出一：抽牌堆 → 手牌 ----

    /** 待播的飞入演出统一在这里排出去（由 refreshAll 末尾调用） */
    private void flushDrawFx() {
        if (pendingDrawFx.isEmpty()) return;
        List<Card> cards = new ArrayList<>(pendingDrawFx);
        pendingDrawFx.clear();
        drawFxInFlight.addAll(cards);

        // 演出期间锁住出牌与“结束回合”，免得玩家点到一张还没落位的牌。
        // 锁的时长跟真正的演出对齐（都放在 afterLayout 里起算），
        // 这样首回合那种“场景还没挂上”的情况也不会提前解锁。
        animating = true;
        refreshHandEnabled();

        // ★ 立刻把真牌藏掉，就在这一帧、这个调用栈里。
        //   不能等到 afterLayout 之后的 flyInOne —— 那时至少已经过了一帧，
        //   错峰的最后一张更是要等 DRAW_STAGGER_MS*(n-1) 才轮到，
        //   玩家会先看到整手牌闪一下（而且是 :disabled 的 0.4 半透明）。
        hideInFlightCards();

        double total = DRAW_STAGGER_MS * (cards.size() - 1) + DRAW_FLY_MS + 80;

        afterLayout(() -> {
            for (int i = 0; i < cards.size(); i++) {
                final Card c = cards.get(i);
                delay(DRAW_STAGGER_MS * i, () -> flyInOne(c));
            }
            delay(total, () -> {
                animating = false;
                if (!battleOver) refreshHandEnabled();
                // 整段演出结束，把 inline 样式撤掉，交还给 CSS 决定手牌明暗
                // （打不起的牌该是 0.4 就该是 0.4）
                for (Node n : handBox.getChildren()) setCardNodeVisible(n);
            });
        });
    }

    /** 把「等待飞入」的牌的真身全部藏起来（幂等） */
    private void hideInFlightCards() {
        for (Card c : drawFxInFlight) setCardNodeHidden(findCardNode(c));
    }

    /** 真身隐身：走 inline style，避免和 {@code .button:disabled} 的 0.4 打架 */
    private static void setCardNodeHidden(Node cardNode) {
        if (cardNode instanceof Button b) b.setStyle(CARD_BTN_HIDDEN_STYLE);
    }

    /** ghost 落位：真身以全亮显示（演出未结束前仍是 disabled，不能撤 inline） */
    private static void setCardNodeLanded(Node cardNode) {
        if (cardNode instanceof Button b) b.setStyle(CARD_BTN_LANDED_STYLE);
    }

    /** 整段演出结束：撤掉 inline 的 -fx-opacity，让 CSS 重新决定明暗 */
    private static void setCardNodeVisible(Node cardNode) {
        if (cardNode instanceof Button b) b.setStyle(CARD_BTN_STYLE);
    }

    /** 一张牌从抽牌堆图标飞到它在手牌里的位置，落位后真牌才显现 */
    private void flyInOne(Card c) {
        Node target = findCardNode(c);
        Point2D from = centerInLocal(drawIcon);
        Point2D to = centerInLocal(target);
        if (from == null || to == null) {   // 拿不到坐标就别演了，直接把牌显示出来
            drawFxInFlight.remove(c);
            setCardNodeVisible(target);
            return;
        }

        StackPane ghost = ghostOf(c);
        mountGhost(ghost, from);
        ghost.setScaleX(0.5);
        ghost.setScaleY(0.5);
        ghost.setRotate(-20);
        ghost.setOpacity(0.85);
        setCardNodeHidden(target); // 兜底：正常情况 flushDrawFx 里已经藏好了

        TranslateTransition move = new TranslateTransition(Duration.millis(DRAW_FLY_MS), ghost);
        move.setToX(to.getX() - from.getX());
        move.setToY(to.getY() - from.getY());
        move.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition grow = new ScaleTransition(Duration.millis(DRAW_FLY_MS), ghost);
        grow.setToX(1);
        grow.setToY(1);
        grow.setInterpolator(Interpolator.EASE_OUT);

        RotateTransition spin = new RotateTransition(Duration.millis(DRAW_FLY_MS), ghost);
        spin.setToAngle(0);
        spin.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(DRAW_FLY_MS), ghost);
        fade.setToValue(1);

        ParallelTransition all = new ParallelTransition(move, grow, spin, fade);
        all.setOnFinished(e -> {
            unmountGhost(ghost);
            drawFxInFlight.remove(c);
            setCardNodeLanded(target); // 全亮落位；整段演出结束时才交还给 CSS
        });
        all.play();
    }

    // ---- 演出二：手牌 → 弃牌堆 ----

    /** 打出非消耗牌：卡面从手牌位置飞进右下角弃牌堆（边飞边缩小旋转） */
    private void flyToDiscard(Card c, Point2D from) {
        if (from == null) return;
        Point2D to = centerInLocal(discardIcon);
        if (to == null) return;
        dumpOne(c, from, to, 0, 35);
    }

    /** 回合结束：整手牌错峰飞进弃牌堆 */
    private void flyHandToDiscard(List<Card> cards, List<Point2D> froms) {
        if (cards.isEmpty()) return;
        afterLayout(() -> {
            Point2D to = centerInLocal(discardIcon);
            if (to == null) return;
            for (int i = 0; i < cards.size(); i++) {
                Point2D from = froms.get(i);
                if (from == null) continue;
                dumpOne(cards.get(i), from, to, DUMP_STAGGER_MS * i, 0);
            }
        });
    }

    private void dumpOne(Card c, Point2D from, Point2D to, double delayMs, double spinAngle) {
        delay(delayMs, () -> {
            StackPane ghost = ghostOf(c);
            mountGhost(ghost, from);

            TranslateTransition move = new TranslateTransition(Duration.millis(DISCARD_FLY_MS), ghost);
            move.setToX(to.getX() - from.getX());
            move.setToY(to.getY() - from.getY());
            move.setInterpolator(Interpolator.EASE_BOTH);

            ScaleTransition shrink = new ScaleTransition(Duration.millis(DISCARD_FLY_MS), ghost);
            shrink.setToX(0.25);
            shrink.setToY(0.25);

            FadeTransition fade = new FadeTransition(Duration.millis(DISCARD_FLY_MS), ghost);
            fade.setFromValue(1);
            fade.setToValue(0.1);

            RotateTransition spin = new RotateTransition(Duration.millis(DISCARD_FLY_MS), ghost);
            spin.setToAngle(spinAngle);

            ParallelTransition all = new ParallelTransition(move, shrink, fade, spin);
            all.setOnFinished(e -> unmountGhost(ghost));
            all.play();
        });
    }

    // ---- 演出三：消耗牌燃尽 ----

    /** 打出消耗牌：卡面在原地上浮、泛金、燃尽消失（不进弃牌堆） */
    private void playExhaustFx(Card c, Point2D from) {
        if (from == null) return;
        StackPane ghost = ghostOf(c);
        mountGhost(ghost, from);

        Rectangle glow = new Rectangle(HAND_FACE_W, HAND_FACE_W * 1.4);
        glow.setArcWidth(16);
        glow.setArcHeight(16);
        glow.setFill(Color.rgb(251, 191, 36, 0.85));
        glow.setOpacity(0);
        glow.setMouseTransparent(true);
        ghost.getChildren().add(glow);

        TranslateTransition rise = new TranslateTransition(Duration.millis(EXHAUST_FX_MS), ghost);
        rise.setToY(-70);
        rise.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition blow = new ScaleTransition(Duration.millis(EXHAUST_FX_MS), ghost);
        blow.setToX(1.18);
        blow.setToY(1.18);

        FadeTransition burn = new FadeTransition(Duration.millis(EXHAUST_FX_MS), ghost);
        burn.setFromValue(1);
        burn.setToValue(0);

        FadeTransition flash = new FadeTransition(Duration.millis(130), glow);
        flash.setFromValue(0);
        flash.setToValue(0.9);

        SequentialTransition seq = new SequentialTransition(
                flash, new ParallelTransition(rise, blow, burn));
        seq.setOnFinished(e -> unmountGhost(ghost));
        seq.play();
    }

    // ================= BattleState 实现 =================

    @Override
    public boolean isPlayerTurn() {
        return playerTurn;
    }

    @Override
    public boolean isBattleOver() {
        return battleOver;
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public void spendEnergy(int amount) {
        energy -= amount;
    }

    @Override
    public void gainEnergy(int amount) {
        energy += amount;
    }

    @Override
    public int getStrength() {
        return playerStrength;
    }

    @Override
    public void gainStrength(int amount) {
        playerStrength += amount;
    }

    @Override
    public int getWeakTurns() {
        return weakTurns;
    }

    @Override
    public int getEnemyVulnerable() {
        return enemyVulnerable;
    }

    @Override
    public void addEnemyVulnerable(int amount) {
        enemyVulnerable += amount;
    }

    @Override
    public void addEnemyWeak(int amount) {
        enemyWeak += amount;
    }

    @Override
    public void doubleBlock() {
        playerBlock *= 2;
    }

    @Override
    public void loseStrengthAtTurnEnd(int amount) {
        pendingStrengthLoss += amount;
    }

    @Override
    public void forbidDrawThisTurn() {
        noDrawThisTurn = true;
    }

    @Override
    public void activatePower(Card.Kind kind) {
        // 能力牌可叠加：层数 +1
        powerStacks.merge(kind, 1, Integer::sum);
    }

    /** 能力牌 → 状态栏角标（新增常驻能力牌时在此登记即可自动显示；stacks 为当前层数） */
    private PowerBadge powerBadgeOf(Card.Kind kind, int stacks) {
        return switch (kind) {
            case BRUTALITY -> new PowerBadge("残", "#701a75",
                    "残暴 ×" + stacks + "：每回合开始失去 " + stacks + " 点生命，随后多抽 " + stacks + " 张");
            case DEMON_FORM -> new PowerBadge("恶魔", "#701a75",
                    "恶魔形态 ×" + stacks + "：每回合增加 " + stacks*2 + " 点力量");
            case FEEL_NO_PAIN -> new PowerBadge("无惧", "#701a75",
                    "无惧疼痛 ×" + stacks + "：每有一张牌被消耗，获得 " + stacks*3 + " 点格挡");
            default -> null;
        };
    }

    @Override
    public int getBlock() {
        return playerBlock;
    }

    @Override
    public void addBlock(int amount) {
        if (amount > 0) SoundFx.play("GainDefense"); // 玩家获得格挡音效
        playerBlock += amount;
    }

    @Override
    public void damageEnemy(int dmg) {
        if (dmg > 0) SoundFx.play("ironclad_attack"); // 玩家攻击牌命中怪物音效
        enemyAnim.triggerHitKnock();
        if (enemy.block > 0) {
            int absorb = Math.min(enemy.block, dmg);
            enemy.block -= absorb;
            dmg -= absorb;
        }
        enemy.hp = Math.max(0, enemy.hp - dmg);
        applyReflect(dmg);
        if (enemy.hp == 0) victory();
    }

    @Override
    public boolean loseHp(int hp, boolean withHurtAnim) {
        if (hp > 0) SoundFx.play("GetHurt"); // 玩家受伤音效（含自伤牌）
        player.damage(hp);
        if (withHurtAnim) playerAnim.triggerHurt();
        hud.refresh();
        if (player.hp() == 0) { playerDied(); return true; }
        return false;
    }

    @Override
    public void drawCards(int n) {
        drawHand(n);
    }

    @Override
    public void addToDrawPile(Card c) {
        draw.add(c);
    }

    @Override
    public void addToHand(Card c) {
        // 手牌已满（打出中的牌已提前移出 hand）：溢出的牌放入抽牌堆
        if (hand.size() >= HAND_LIMIT) {
            draw.add(c);
        } else {
            hand.add(c);
        }
    }

    @Override
    public void onCardPlayed(Card c) {
        // 先趁牌还在手牌里把位置记下来，之后 hand.remove + refreshAll 就找不到它了
        Point2D from = centerOfCardNode(c);
        hand.remove(c);
        if (c.isExhaustOnPlay()) {
            playExhaustFx(c, from);   // 消耗（含能力牌）：原地燃尽，不进弃牌堆
            // 无惧疼痛：只有带“消耗”词条的牌才算“被消耗”，能力牌使用离场不计
            if (c.exhaust) gainBlockFromExhaust();
        } else {
            discard.add(c);
            flyToDiscard(c, from);    // 普通牌：飞进弃牌堆
        }
    }

    /** 无惧疼痛：每当一张牌被消耗，按层数获得 3×层数 点格挡 */
    private void gainBlockFromExhaust() {
        int stacks = powerStacks.getOrDefault(Card.Kind.FEEL_NO_PAIN, 0);
        if (stacks > 0) addBlock(3 * stacks);
    }

    @Override
    public void exhaustNonAttackCardsInHand() {
        // 先快照：遍历中会从 hand 移除，且消耗会触发格挡等副作用
        List<Card> targets = new ArrayList<>();
        for (Card c : hand) {
            if (c.kind.type != Card.Type.ATTACK) targets.add(c);
        }
        for (Card c : targets) {
            Point2D from = centerOfCardNode(c);
            hand.remove(c);
            playExhaustFx(c, from);
            gainBlockFromExhaust();   // 这些牌确实被消耗，触发无惧疼痛
        }
    }

    @Override
       public void exhaustRandomHandCard() {
        if (hand.isEmpty()) return;   // 手牌为空：无目标，不做处理
        Card c = hand.get(rnd.nextInt(hand.size()));
        Point2D from = centerOfCardNode(c);
        hand.remove(c);
        playExhaustFx(c, from);
        gainBlockFromExhaust();       // 这张牌确实被消耗，触发无惧疼痛
    }

    @Override
    public void refreshIfAlive() {
        if (!battleOver) refreshAll();
    }

    // ================= 供开发者面板（com.example.demo.operator）使用的通用接口 =================
    // 说明：手牌/抽牌堆/弃牌堆都是 BattleView 的私有状态，operator 包碰不到，
    // 所以这里只暴露三个「不带游戏规则」的通用操作，开发者语义留在 operator 包里。

    /** 手牌（活引用，可直接增删，改完调用 {@link #refreshUi()} 重画） */
    public List<Card> handCards() {
        return hand;
    }

    /** 把一张牌从本场战斗的手牌 / 抽牌堆 / 弃牌堆里删掉（不动玩家的牌组） */
    public void removeCardFromBattle(Card c) {
        if (c == null) return;
        hand.remove(c);
        draw.remove(c);
        discard.remove(c);
        refreshUi();
    }

    /** 界面重画（改了手牌/牌组/遗物之后调用） */
    public void refreshUi() {
        if (battleOver) return;
        refreshAll();
        hud.refresh();
    }

    /** 反伤：怪物处于反伤状态时，把玩家造成伤害的一定比例反弹给玩家（先扣格挡再扣血）。 */
    private void applyReflect(int dmg) {
        if (reflectTurns <= 0) return;
        int reflectDmg = (int)(dmg * enemy.getReflectRate());
        if (reflectDmg <= 0) return;
        if (playerBlock > 0) {
            int absorb = Math.min(playerBlock, reflectDmg);
            playerBlock -= absorb;
            reflectDmg -= absorb;
        }
        player.hp = Math.max(0, player.hp - reflectDmg);
        SoundFx.play("GetHurt"); // 反伤受击音效
        hud.refresh();
        if (player.hp == 0) playerDied();
    }
    /** 玩家受到伤害，处理百年积木遗物效果 */
    private void takeDamage(int dmg) {
        if (dmg <= 0) return;
        int hpBefore = player.hp();
        player.damage(dmg);
        // 百年积木：每场战斗第一次失去生命值时抽 3 张牌
        if (!firstDamageTriggered && player.hp() < hpBefore
                && RelicFun.hasRelic(player, "百年积木")) {
            firstDamageTriggered = true;
            drawHand(3);
        }
    }

    // ================= 怪物回合 =================

    private void endPlayerTurn() {
        if (!playerTurn || battleOver) return;
        SoundFx.play("EndTurn"); // 结束玩家回合音效
        playerTurn = false;
        if (reflectTurns > 0) reflectTurns--;

        // 活动肌肉：回合结束时扣除本回合临时获得的力量
        if (pendingStrengthLoss > 0) {
            playerStrength -= pendingStrengthLoss;
            pendingStrengthLoss = 0;
        }

        // 忘情牛肉面：第一回合结束后移除临时 3 点力量
        if (turn == 1 && !noodleBonusUsed && RelicFun.hasRelic(player, "忘情牛肉面")) {
            noodleBonusUsed = true;
            playerStrength -= 3;
        }

        // 奥利哈钢：回合结束时若无格挡，获得 6 点格挡
        if (playerBlock == 0 && RelicFun.hasRelic(player, "奥利哈钢")) {
            playerBlock += 6;
        }
        // taffy：回合结束时生命值高于 50% 额外获得 5 点格挡
        if (player.hp() * 2 > player.maxHp && RelicFun.hasRelic(player, "taffy")) {
            playerBlock += 5;
        }

        // 先记下每张手牌的位置，再把它们一起丢进弃牌堆（之后就找不到节点了）
        List<Card> dumped = new ArrayList<>(hand);
        List<Point2D> froms = new ArrayList<>();
        for (Card c : dumped) froms.add(centerOfCardNode(c));

        discard.addAll(hand);
        hand.clear();
        refreshAll();
        flyHandToDiscard(dumped, froms);

        PauseTransition pause = new PauseTransition(Duration.millis(700));
        pause.setOnFinished(e -> enemyAct());
        pause.play();
    }

    private void enemyAct() {
        if (battleOver || paused) return;

        enemy.block = 0;
        enemy.applyRitual();
        boolean wasSecondPhase = enemy.isSecondPhase;
        enemy.checkPhaseTransition();
        if (enemy.isSecondPhase && !wasSecondPhase) {
            SoundFx.play("zhou"); // 二阶段变身音效
            playPhaseTransition(this::performEnemyAction);
        } else {
            performEnemyAction();
        }
    }

    /**
     * 怪物行为音效（含 BOSS 专属版本）。
     *
     * 命名约定（都放在 resources/com/example/demo/sound/ 下）：
     *   normalOink / normalDie    —— 普通怪（卫兵猪、史莱姆……）
     *   fishronOink / fishronDie  —— BOSS 鱼龙
     *   zhou                      —— 强化/减益/反伤/吐黏液/仪式 等施法类通用音效
     *
     * BOSS 优先用自己的音效，文件缺失时自动退回普通怪音效（见 {@link SoundFx#playAny}），
     * 所以只做一个 BOSS 的叫声也不会出现「静音」的怪。
     */
    private List<String> enemySoundOf(Enemy.Intent intent) {
        return switch (intent) {
            case ATTACK -> enemyOink();              // 出手叫声：BOSS 鱼龙叫 / 普通猪叫
            case DEFEND -> List.of("GainDefense");   // 复用已有的加盾音效
            case BUFF, WEAKEN, REFLECT, RITUAL -> List.of("zhou");
        };
    }

    /** 怪物出手叫声：BOSS 用 fishronOink，没有就退回 normalOink */
    private List<String> enemyOink() {
        return enemy.isBoss ? List.of("fishronOink", "normalOink") : List.of("normalOink");
    }

    /** 怪物死亡音效：BOSS 用 fishronDie，没有就退回 normalDie */
    private List<String> enemyDie() {
        return enemy.isBoss ? List.of("fishronDie", "normalDie") : List.of("normalDie");
    }

    private void performEnemyAction() {
        Enemy.Step s = enemy.current();
        SoundFx.playAny(enemySoundOf(s.intent)); // 怪物行为音效，映射见 enemySoundOf
        switch (s.intent) {
            case ATTACK -> {
                enemyAnim.triggerAttackDash();
                int dmg = s.value + enemy.power;
                if (enemyWeak > 0) dmg = dmg * 3 / 4;
                if (enemy.isBoss && enemy.isSecondPhase && playerBlock > 0) {
                    dmg = (int) Math.floor(dmg * 1.60);
                }
                if (playerBlock > 0) {
                    int absorb = Math.min(playerBlock, dmg);
                    playerBlock -= absorb;
                    dmg -= absorb;
                }
                takeDamage(dmg);
                playerAnim.triggerHurt();
                if (dmg > 0) SoundFx.play("GetHurt"); // 玩家被怪物攻击的受伤音效
                hud.refresh();
                if (player.hp() == 0) { playerDied(); return; }
                // 攻击后附加效果：向玩家抽牌堆塞入黏液（史莱姆特有）
                for (int i = 0; i < enemy.getSlimeOnAttack(); i++) {
                    draw.add(Card.slime());
                }
            }
            case DEFEND -> enemy.block += s.value; // 音效由 enemySoundOf(DEFEND) 播放
            case BUFF -> enemy.power += s.value;
            case WEAKEN -> weakTurns = Math.max(weakTurns, s.value);
            case REFLECT -> reflectTurns = Math.max(reflectTurns,s.value);
            case RITUAL -> enemy.setRitualPower(s.value);
        }
        if (enemyWeak > 0) enemyWeak--;
        enemy.advance();

        refreshAll();
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> {
            if (battleOver) return;
            if (paused) { pendingTurnStart = true; return; }
            startPlayerTurn();
        });
        pause.play();
    }

    /** 转阶段动画：一阶段立绘淡出后，二阶段立绘在原地淡入。 */
    private void playPhaseTransition(Runnable onDone) {
        ImageView second = new ImageView();
        Image img = new Image(getClass().getResourceAsStream(
                "/com/example/demo/portrait/" + enemy.name + "二阶段.png"));
        second.setImage(img);
        second.setFitWidth(enemyPortraitSize);
        second.setFitHeight(enemyPortraitSize);
        second.setPreserveRatio(true);
        second.setOpacity(0);
        enemyPortrait.getChildren().add(second);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), enemyPortraitImg);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), second);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        SequentialTransition seq = new SequentialTransition(fadeOut, fadeIn);
        seq.setOnFinished(e -> onDone.run());
        seq.play();
    }

    public void setPaused(boolean p) {
        paused = p;
        if (!paused && !battleOver) {
            if (pendingTurnStart) {
                pendingTurnStart = false;
                startPlayerTurn();
            } else if (!playerTurn) {
                enemyAct();
            }
        }
    }

    // ================= 胜负 =================

    /** 战斗结束：清除战斗中产生的状态牌（伤口/黏液等），不遗留到后续牌组。 */
    private void clearStatusCards() {
        player.deck.removeIf(c -> c.kind.type == Card.Type.STATUS);
    }

    private void victory() {
        if (battleOver) return;
        battleOver = true;
        playerAnim.stop();
        RelicFun.onBattleEnd(player);
        hud.refresh();
        playerAnim.stop();   // 冻结双方待机呼吸，交给倒地动画接管
        enemyAnim.stop();
        clearStatusCards();
        refreshAll();        // 先把怪物血条刷成 0、手牌置灰
        refreshHandEnabled();

        // ---- 敌人倒地演出：向后倒下 + 下沉，然后才弹胜利奖励 ----
        SoundFx.playAny(enemyDie()); // 怪物倒地/死亡音效（BOSS 用 fishronDie）
        RotateTransition rotate = new RotateTransition(Duration.millis(750), enemyPortrait);
        rotate.setToAngle(82);           // 顺时针倒下（朝远离玩家的方向）
        rotate.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition fall = new TranslateTransition(Duration.millis(750), enemyPortrait);
        fall.setToX(18);
        fall.setToY(70);
        fall.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition fallDown = new ParallelTransition(rotate, fall);
        SequentialTransition seq = new SequentialTransition(
                fallDown, new PauseTransition(Duration.millis(180)));
        seq.setOnFinished(e -> {
            gameTimer.stop();
            showReward();
        });
        seq.play();
    }

    private void playerDied() {
        gameTimer.stop();
        if (diedShown) return;
        diedShown = true;
        battleOver = true;
        playerAnim.stop();
        clearStatusCards();

        deadDim.setVisible(true);
        SoundFx.play("normalDie");

        RotateTransition rotate = new RotateTransition(Duration.millis(900), playerPortrait);
        rotate.setToAngle(85);
        TranslateTransition fall = new TranslateTransition(Duration.millis(900), playerPortrait);
        fall.setToY(80);
        ParallelTransition both = new ParallelTransition(rotate, fall);
        both.setOnFinished(e -> {
            deadDim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.62);");
            deathOverlay.setVisible(true);
        });
        both.play();
    }

    /** 屏幕中央出现三张随机牌，点一张加入牌组（或跳过），然后离开战斗 */
    private void showReward() {
        pileOverlay.hide();

        List<Card> pool = List.of(
                Card.sweep(), Card.bleed(),
                Card.pommelStrike(), Card.shrug(),
                Card.hammer(), Card.impregnable(),
                Card.doubleStrike(), Card.kindle(), Card.lightning(),
                Card.rage(), Card.offering(), Card.wildStrike(),
                Card.fortify(), Card.focus(), Card.shockwave(),
                Card.heavyBlade(), Card.adamantArm(), Card.brutality(), Card.flex(),
                Card.powerThrough(), Card.soulSever(), Card.uppercut(), Card.bodySlam(),
                Card.hemokinesis(), Card.limitBreak(), Card.feelNoPain(), Card.trueGrit());
        // 权重取自卡牌基础权重（4=白/普通，3=蓝/罕见，1=金/稀有），
        // 避免与卡池硬编码的双份数据源不同步；精英战使用专属权重。
        List<Integer> weights = pool.stream()
                .map(this::rewardWeight)
                .toList();

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

        rewardOverlay.show(offers, (c, node) -> {
            player.deck.add(c);        // 数据照常即时结算（牌组数量随之更新）
            hud.refresh();
            rewardOverlay.hide();
            // 获得的卡飞入牌组图标（ghost 卡面，落位后自动摘除，不阻塞流程）
            CardFlyFx.flyIntoDeck(getScene(), node, hud.getDeckIcon(), c,
                    () -> onFinish.accept(true));
        });
    }

    /**
     * 卡牌进入奖励池的抽取权重。
     * <p>
     * 普通战斗沿用卡牌基础权重（4=白 / 3=蓝 / 1=金）；
     * 精英战斗使用专属权重（金卡 2、蓝卡 3、白卡 3），提高金卡出现概率。
     */
    private int rewardWeight(Card c) {
        if (!enemy.isElite) return c.kind.weight;
        return switch (c.kind.weight) {
            case 1 -> 2;   // 金卡（稀有）
            case 3 -> 3;   // 蓝卡（罕见）
            default -> 3;  // 白卡（普通，基础权重 4）
        };
    }

    // ================= 刷新 =================

    private void refreshAll() {
        turnLabel.setText("第 " + turn + " 回合");
        energyLabel.setText("能量 " + energy + " / 3");

        // 角色
        double pRatio = (double) player.hp() / player.maxHp;
        pHpText.setText(player.hp() + " / " + player.maxHp);
        pHpFill.setPrefWidth(Math.max(0, 240.0 * pRatio));
        pHpFill.setStyle("-fx-background-color: " + (pRatio < 0.4 ? "#ef4444" : "#22c55e")
                + "; -fx-background-radius: 9;");
        pHpWrap.setStyle(BattleUiFactory.frameStyle(playerBlock > 0));
        pShield.setVisible(playerBlock > 0);
        pShieldNum.setText(String.valueOf(playerBlock));

        pChips.getChildren().clear();
        if (weakTurns > 0) {
            pChips.getChildren().add(BattleUiFactory.statusChip("弱", weakTurns, "#7c3aed",
                    "虚弱 ×" + weakTurns + "：你造成的伤害 ×0.75"));
        }
        if (playerStrength > 0) {
            pChips.getChildren().add(BattleUiFactory.statusChip("力", playerStrength, "#f59e0b",
                    "力量 +" + playerStrength + "：每段攻击伤害增加"));
        }
        for (Map.Entry<Card.Kind, Integer> e : powerStacks.entrySet()) {
            PowerBadge badge = powerBadgeOf(e.getKey(), e.getValue());
            if (badge != null) {
                pChips.getChildren().add(BattleUiFactory.statusChip(
                        badge.glyph(), e.getValue(), badge.color(), badge.tip()));
            }
        }

        // 怪物
        eName.setText(enemy.name);
        double eRatio = (double) enemy.hp / enemy.maxHp;
        eHpText.setText(enemy.hp + " / " + enemy.maxHp);
        eHpFill.setPrefWidth(Math.max(0, 240.0 * eRatio));
        eHpFill.setStyle("-fx-background-color: #dc2626; -fx-background-radius: 9;");
        eHpWrap.setStyle(BattleUiFactory.frameStyle(enemy.block > 0));
        eShield.setVisible(enemy.block > 0);
        eShieldNum.setText(String.valueOf(enemy.block));

        eChips.getChildren().clear();
        if (enemy.power > 0) {
            eChips.getChildren().add(BattleUiFactory.statusChip("力", enemy.power, "#f59e0b",
                    "力量：每段攻击伤害增加"+enemy.power+"点"));
        }
        if (enemyVulnerable > 0) {
            eChips.getChildren().add(BattleUiFactory.statusChip("伤", enemyVulnerable, "#dc2626",
                    "易伤： " + enemyVulnerable + " 回合内：承受伤害 ×1.5"));
        }
        if (reflectTurns > 0) {
            int pct = (int)(enemy.getReflectRate() * 100);
            eChips.getChildren().add(BattleUiFactory.statusChip("反", reflectTurns, "#9400D3",
                    "反伤：" + reflectTurns + " 回合内：你攻击时受到造成伤害 " + pct + "% 的伤害"));
        }

        // 左下角仪式状态栏：仪式激活时显示每回合力量增长效果
        int ritual = enemy.getRitualPower();
        if (ritual > 0) {
            ritualStatus.setText("仪式：每回合力量 +" + ritual);
            ritualStatus.setVisible(true);
        } else {
            ritualStatus.setVisible(false);
        }

        // 破甲状态栏：BOSS 二阶段时显示特殊破甲机制
        if (enemy.isBoss && enemy.isSecondPhase) {
            armorBreakStatus.setText("破甲：你持盾时其攻击 ×1.6");
            armorBreakStatus.setVisible(true);
        } else {
            armorBreakStatus.setVisible(false);
        }
        if (enemyWeak > 0) {
            eChips.getChildren().add(BattleUiFactory.statusChip("弱", enemyWeak, "#7c3aed",
                    "虚弱 " + enemyWeak + " 回合：敌人造成的伤害 ×0.75"));
        }
        refreshIntent();

        // 手牌
        handBox.getChildren().clear();
        for (Card c : hand) {
            handBox.getChildren().add(buildCardButton(c));
        }
        emptyHandLabel.setVisible(hand.isEmpty());
        pilesInfo.setText("抽牌堆 " + draw.size() + "  ·  弃牌堆 " + discard.size());

        drawBadge.setText(String.valueOf(draw.size()));
        drawBadge.setVisible(draw.size() > 0);
        discardBadge.setText(String.valueOf(discard.size()));
        discardBadge.setVisible(discard.size() > 0);

        endTurnBtn.setDisable(!playerTurn || battleOver || animating);

        flushDrawFx(); // 新抽到的牌在这里补上“从抽牌堆飞入”的演出
    }

    private void refreshHandEnabled() {
        // 非玩家回合 / 战斗结束 / 抽牌演出中：整体禁用；
        // 其余情况按 CardPlay.canPlay 逐张重算，
        // 避免把能量不足或不可打出的牌（如“伤口”）错误地重新启用。
        if (!playerTurn || battleOver || animating) {
            for (var node : handBox.getChildren()) {
                if (node instanceof Button btn) {
                    btn.setDisable(true);
                }
            }
            endTurnBtn.setDisable(true);
            return;
        }
        for (var node : handBox.getChildren()) {
            if (node instanceof Button btn) {
                Card c = (Card) btn.getUserData();
                if (c != null) btn.setDisable(!CardPlay.canPlay(c, this));
            }
        }
        endTurnBtn.setDisable(false);
    }

    private Button buildCardButton(Card c) {
        // 多层贴图卡面（固定尺寸容器，手牌高度稳定，防止打牌/换回合时画面跳动）
        javafx.scene.layout.StackPane face = CardFaceView.buildAt(c, HAND_FACE_W);

        Button btn = new Button();
        btn.setGraphic(face);
        btn.setUserData(c); // 飞行演出靠它把按钮和牌对上
        // 还没落位的牌（错峰飞行中手牌被重建）要保持隐身，否则会先亮出来再消失
        btn.setStyle(drawFxInFlight.contains(c) ? CARD_BTN_HIDDEN_STYLE : CARD_BTN_STYLE);
        btn.setDisable(!CardPlay.canPlay(c, this) || animating);
        btn.setOnAction(e -> {
            play(c);
            refreshHandEnabled();
        });
        return btn;
    }
}
