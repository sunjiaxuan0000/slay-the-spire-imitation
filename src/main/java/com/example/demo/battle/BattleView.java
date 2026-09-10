package com.example.demo.battle;

import com.example.demo.card.BattleState;
import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;
import com.example.demo.card.CardPlay;
import com.example.demo.card.CardView;
import com.example.demo.character.Player;
import com.example.demo.character.Relic;
import com.example.demo.enemy.Enemy;
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
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * 回合制战斗界面（纯战斗逻辑 + 面板拼装）。
 *
 * 卡牌渲染委托 {@link CardView}，静态 UI 构件委托 {@link BattleUiFactory}，
 * 弹层（牌堆浏览/奖励/死亡）委托 view 包中各自的 Overlay 类。
 */
public class BattleView extends javafx.scene.layout.StackPane implements BattleState {
    private StackPane enemyPortrait;
    private ImageView enemyPortraitImg;
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
    private int playerStrength = 0;
    private boolean playerTurn = true;
    private boolean battleOver = false;
    private boolean paused = false;
    private boolean pendingTurnStart = false;

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

        if (hasRelic("保温杯")) {
            player.heal(10);
            hud.refresh();
        }
        initAnimators();
        gameTimer.start();
        startPlayerTurn();
    }

    // ================= 动画（统一委托 SpriteAnimator） =================

    /** 初始化两个立绘动画器，在面板建好后调用 */
    private void initAnimators() {
        playerAnim = new SpriteAnimator(playerPortrait, true, 90, -32);
        enemyAnim = new SpriteAnimator(enemyPortrait, false, 42, -32);
    }

    private boolean hasRelic(String name) {
        for (Relic r : player.relics) {
            if (r.name.equals(name)) return true;
        }
        return false;
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

        StackPane portrait;
        if (enemy.hasPortrait) {
            enemyPortraitImg = new ImageView();
            Image img = new Image(getClass().getResourceAsStream("/com/example/demo/portrait/" + enemy.name + ".png"));
            enemyPortraitImg.setImage(img);
            enemyPortraitImg.setFitWidth(210);
            enemyPortraitImg.setFitHeight(210);
            enemyPortraitImg.setPreserveRatio(true);
            portrait = new StackPane(enemyPortraitImg);
        } else {
            portrait = BattleUiFactory.portrait(enemy.name.substring(0, 1),
                    "radial-gradient(center 35% 30%, radius 100%, #6b7280, #1f2937);");
        }
        portrait.setPrefSize(210, 210);
        portrait.setMaxSize(210, 210);
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
                tip = "意图·攻击：将对玩家造成 " + number + " 伤害";
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
            case SPIT -> {
                glyph = "黏";
                color = "#16a34a";
                tip = "意图·吐黏液：向你的抽牌堆塞入 " + s.value + " 张黏液";
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
        energy = 3;

        drawHand(5 + (hasRelic("请假条") ? 1 : 0));

        if (turn == 1 && hasRelic("青铜怀表")) {
            playerBlock = 2;
        }

        refreshAll();
    }

    private void drawHand(int n) {
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
    public int getBlock() {
        return playerBlock;
    }

    @Override
    public void addBlock(int amount) {
        playerBlock += amount;
    }

    @Override
    public void damageEnemy(int dmg) {
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
    public void onCardPlayed(Card c) {
        hand.remove(c);
        if (c.exhaust) {
            // 消耗：不进入弃牌堆
        } else {
            discard.add(c);
        }
    }

    @Override
    public void refreshIfAlive() {
        if (!battleOver) refreshAll();
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
        hud.refresh();
        if (player.hp == 0) playerDied();
    }
    // ================= 怪物回合 =================

    private void endPlayerTurn() {
        if (!playerTurn || battleOver) return;
        playerTurn = false;
        if (reflectTurns > 0) reflectTurns--;

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
            playPhaseTransition(this::performEnemyAction);
        } else {
            performEnemyAction();
        }
    }

    private void performEnemyAction() {
        Enemy.Step s = enemy.current();
        switch (s.intent) {
            case ATTACK -> {
                enemyAnim.triggerAttackDash();
                int dmg = s.value + enemy.power;
                if (enemy.isBoss && enemy.isSecondPhase && playerBlock > 0) {
                    dmg = (int) Math.floor(dmg * 1.60);
                }
                if (playerBlock > 0) {
                    int absorb = Math.min(playerBlock, dmg);
                    playerBlock -= absorb;
                    dmg -= absorb;
                }
                player.damage(dmg);
                playerAnim.triggerHurt();
                hud.refresh();
                if (player.hp() == 0) { playerDied(); return; }
            }
            case DEFEND -> enemy.block += s.value;
            case BUFF -> enemy.power += s.value;
            case WEAKEN -> weakTurns = Math.max(weakTurns, s.value);
            case REFLECT -> reflectTurns = Math.max(reflectTurns,s.value);
            case SPIT -> {
                for (int i = 0; i < s.value; i++) {
                    draw.add(Card.slime());
                }
            }
            case RITUAL -> enemy.setRitualPower(s.value);
        }
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
        second.setFitWidth(210);
        second.setFitHeight(210);
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

    private void victory() {
        gameTimer.stop();
        if (battleOver) return;
        battleOver = true;
        playerAnim.stop();
        showReward();
    }

    private void playerDied() {
        gameTimer.stop();
        if (diedShown) return;
        diedShown = true;
        battleOver = true;
        playerAnim.stop();

        deadDim.setVisible(true);

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
                Card.bash(), Card.sweep(),
                Card.pommelStrike(), Card.shrug(), Card.bleed(),
                Card.hammer(), Card.impregnable(),
                Card.doubleStrike(), Card.kindle(), Card.lightning(),
                Card.rage(), Card.offering());
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
        for (var node : handBox.getChildren()) {
            if (node instanceof Button btn) {
                btn.setDisable(!playerTurn || battleOver);
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
        btn.setOnAction(e -> {
            play(c);
            refreshHandEnabled();
        });
        return btn;
    }
}
