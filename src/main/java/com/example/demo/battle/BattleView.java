package com.example.demo.battle;

import com.example.demo.card.BattleState;
import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;
import com.example.demo.card.CardPlay;
import com.example.demo.character.Player;
import com.example.demo.character.Relic;
import com.example.demo.character.RelicFun;
import com.example.demo.enemy.Enemy;
import com.example.demo.sound.SoundFx;
import com.example.demo.view.BattleUiFactory;
import com.example.demo.view.DeathOverlay;
import com.example.demo.view.PileOverlay;
import com.example.demo.view.RewardOverlay;
import com.example.demo.view.SpriteAnimator;
import com.example.demo.view.RunHud;

import javafx.animation.*;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.text.Font;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private Relic bossRelicObtained = null; // Boss 战获得的遗物

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
    private final javafx.scene.layout.StackPane drawIcon = BattleUiFactory.pileIcon("抽", "#78350f");
    private final javafx.scene.layout.StackPane discardIcon = BattleUiFactory.pileIcon("弃", "#1e293b");
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

        // 遗物战斗开始效果（请假条、小血瓶、忘情牛肉面、金刚杵）
        playerStrength += RelicFun.onBattleStart(player);
        // 请假条：设置怪物血量为 1
        if (RelicFun.isLeaveNoteActive(player)) {
            enemy.hp = 1;
        }
        hud.refresh();
        initAnimators();
        gameTimer.start();
        SoundFx.playAny(enemyOink());
        startPlayerTurn();
        // 牛来：对敌人造成伤害
        int bsd = RelicFun.battleStartDamage(player);
        if (bsd > 0) {
            enemy.hp = Math.max(0, enemy.hp - bsd);
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

        pilesInfo.setTextFill(Color.rgb(148, 163, 184));
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
        // 遗物额外能量（奴隶贩子颈环、古茶具套装、孙子兵法）
        energy += RelicFun.extraEnergy(player, enemy, turn, playedCardThisTurn);
        playedCardThisTurn = false;
        attackCardsPlayedThisTurn = 0;
        fanBonusApplied = false;
        skillCardsPlayedThisTurn = 0;
        letterOpenerUsed = false;

        // 残暴：按层数每回合开始失去等量生命，随后多抽等量张（可叠加）
        int brutality = powerStacks.getOrDefault(Card.Kind.BRUTALITY, 0);
        if (brutality > 0 && loseHp(brutality, true)) return;

        drawHand(5 + brutality);

        // 英雄宝典：第一回合添加免费能力牌
        RelicFun.addTurnStartCards(player, hand, turn);

        playerBlock += RelicFun.startBlock(player, turn);

        refreshAll();
    }

    private void drawHand(int n) {
        if (noDrawThisTurn) return; // 战斗专注：本回合禁止再抽牌
        for (int i = 0; i < n; i++) {
            if (hand.size() >= HAND_LIMIT) break;
            Card c = drawOne();
            if (c == null) break;
            hand.add(c);
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
        if (!playerTurn || battleOver) return;
        if (c.cost < 0 || c.cost > energy) return;
        // 先移出手牌进入“打出中”状态再结算：为效果生成的牌腾出槽位，
        // 且结算中的抽牌不会把刚打出的牌从弃牌堆洗回。最终去向由 onCardPlayed 决定。
        hand.remove(c);
        CardPlay.play(c, this);
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
        hand.remove(c); // 出牌时已移出，此处仅作兜底
        if (c.isExhaustOnPlay()) {
            // 消耗（含能力牌）：不进入弃牌堆
        } else {
            discard.add(c);
        }
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
                && RelicFun.shouldDrawOnFirstDamage(player, true)) {
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
        if (RelicFun.shouldRemoveNoodleBonus(player, turn)) {
            playerStrength -= 3;
        }

        // 遗物回合结束格挡（奥利哈钢、taffy）
        playerBlock += RelicFun.endTurnBlock(player, playerBlock, player.hp(), player.maxHp);

        discard.addAll(hand);
        hand.clear();
        refreshAll();

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
        // Boss 战胜利：获得 Boss 遗物（池为空时跳过）
        if (enemy.isBoss) {
            bossRelicObtained = RelicFun.randomBossRelic(player);
        }
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

        String action = RelicFun.bossVictoryAction(bossRelicObtained);
        if ("REMOVE_CARDS".equals(action)) {
            removeCardsFromDeck(2, this::showCardReward);
        } else if ("CHOOSE_ELITE".equals(action)) {
            RelicFun.showEliteRelicChoice(player, this::showCardReward);
        } else {
            showCardReward();
        }
    }

    /** 显示卡牌奖励选择 */
    private void showCardReward() {

        List<Card> pool = List.of(
                Card.sweep(), Card.bleed(),
                Card.pommelStrike(), Card.shrug(),
                Card.hammer(), Card.impregnable(),
                Card.doubleStrike(), Card.kindle(), Card.lightning(),
                Card.rage(), Card.offering(), Card.wildStrike(),
                Card.fortify(), Card.focus(), Card.shockwave(),
                Card.heavyBlade(), Card.adamantArm(), Card.brutality(), Card.flex(),
                Card.powerThrough(), Card.soulSever(), Card.uppercut());
        // 权重直接取自 Card.Kind.weight（4=白/普通，3=蓝/罕见，1=金/稀有），
        // 避免与卡池硬编码的双份数据源不同步。
        List<Integer> weights = pool.stream()
                .map(c -> c.kind.weight)
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

        rewardOverlay.show(offers, c -> {
            player.deck.add(c);
            rewardOverlay.hide();
            onFinish.accept(true);
        });
    }

    /** 移除卡牌：显示 UI 让玩家选择移除 count 张牌，完成后调用 onDone */
    private void removeCardsFromDeck(int count, Runnable onDone) {
        if (player.deck.size() <= count) {
            // 牌组不足，直接跳过
            onDone.run();
            return;
        }
        removeOneCard(count, onDone);
    }

    /** 递归移除单张卡牌 */
    private void removeOneCard(int remaining, Runnable onDone) {
        if (remaining <= 0 || player.deck.isEmpty()) {
            onDone.run();
            return;
        }

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("空鸟笼");
        alert.setHeaderText("选择一张卡牌从卡组中移除（还剩 " + remaining + " 张）");
        alert.getDialogPane().setPrefSize(500, 400);

        javafx.scene.layout.VBox cardList = new javafx.scene.layout.VBox(8);
        cardList.setPadding(new javafx.geometry.Insets(10));

        for (Card c : player.deck) {
            javafx.scene.control.Button cardBtn = new javafx.scene.control.Button(c.kind.label + "  ——  " + c.kind.desc);
            cardBtn.setPrefWidth(460);
            cardBtn.setStyle("-fx-background-color: #2d3748; -fx-text-fill: white; "
                    + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10;");
            cardBtn.setOnMouseEntered(e ->
                cardBtn.setStyle("-fx-background-color: #4a5568; -fx-text-fill: white; "
                        + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10;"));
            cardBtn.setOnMouseExited(e ->
                cardBtn.setStyle("-fx-background-color: #2d3748; -fx-text-fill: white; "
                        + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10;"));
            cardBtn.setOnAction(e -> {
                player.deck.remove(c);
                alert.close();
                removeOneCard(remaining - 1, onDone);
            });
            cardList.getChildren().add(cardBtn);
        }

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(cardList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        alert.getDialogPane().setContent(scrollPane);

        // 移除默认按钮，只保留卡牌选择和取消
        alert.getDialogPane().getButtonTypes().clear();
        alert.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);

        alert.showAndWait().ifPresent(btnType -> {
            if (btnType == javafx.scene.control.ButtonType.CANCEL) {
                onDone.run();
            }
        });
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

        endTurnBtn.setDisable(!playerTurn || battleOver);
    }

    private void refreshHandEnabled() {
        // 仅处理“非玩家回合 / 战斗结束”时的整体禁用；
        // 其余情况保留 refreshAll 中按 CardPlay.canPlay 逐张计算的结果，
        // 避免把能量不足或不可打出的牌（如“伤口”）错误地重新启用。
        if (!playerTurn || battleOver) {
            for (var node : handBox.getChildren()) {
                if (node instanceof Button btn) {
                    btn.setDisable(true);
                }
            }
        }
    }

    private Button buildCardButton(Card c) {
        // 多层贴图卡面（固定尺寸容器，手牌高度稳定，防止打牌/换回合时画面跳动）
        javafx.scene.layout.StackPane face = CardFaceView.buildAt(c, 128);

        Button btn = new Button();
        btn.setGraphic(face);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
        btn.setDisable(c.cost < 0 || c.cost > energy || !playerTurn || battleOver);
        btn.setDisable(!CardPlay.canPlay(c, this));
        btn.setOnAction(e -> {
            play(c);
            refreshHandEnabled();
        });
        return btn;
    }
}
