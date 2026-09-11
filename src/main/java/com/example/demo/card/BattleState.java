package com.example.demo.card;
/**
 * 打出一张牌时所需的战斗状态视图（从 BattleView 抽取）。
 * <p>
 * 由战斗界面（{@code BattleView}）实现。{@link CardPlay} 只依赖该接口结算卡牌，
 * 从而把“出牌规则与卡牌效果”同 UI / 战斗流程解耦。
 */
public interface BattleState {

    // ---------- 基础守卫 ----------

    /** 当前是否轮到玩家回合 */
    boolean isPlayerTurn();

    /** 战斗是否已结束 */
    boolean isBattleOver();

    // ---------- 能量 ----------

    /** 玩家剩余能量 */
    int getEnergy();

    /** 消耗能量 */
    void spendEnergy(int amount);

    /** 获得能量 */
    void gainEnergy(int amount);

    // ---------- 玩家临时状态 ----------

    /** 玩家力量层数 */
    int getStrength();

    /** 增加玩家力量 */
    void gainStrength(int amount);

    /** 玩家剩余虚弱回合 */
    int getWeakTurns();

    /** 敌人剩余易伤层数 */
    int getEnemyVulnerable();

    /** 增加敌人易伤层数 */
    void addEnemyVulnerable(int amount);

    /** 给予敌人虚弱（敌人攻击伤害 ×0.75） */
    void addEnemyWeak(int amount);

    /** 玩家当前格挡值 */
    int getBlock();

    /** 增加玩家格挡 */
    void addBlock(int amount);

    // ---------- 结算动作 ----------

    /**
     * 对敌人造成一次伤害（格挡吸收、扣血、受击演出与击杀判定由战斗流程负责）。
     */
    void damageEnemy(int dmg);

    /**
     * 玩家失去生命。{@code withHurtAnim} 为 true 时触发受击动画。
     *
     * @return true 表示玩家因此死亡（死亡流程已被处理，后续卡牌效果应停止结算）
     */
    boolean loseHp(int hp, boolean withHurtAnim);

    /** 抽 n 张牌（受手牌上限约束，具体规则由战斗流程实现） */
    void drawCards(int n);

    /** 将一张牌放入抽牌堆（如“狂野打击”塞入一张“伤口”） */
    void addToDrawPile(Card c);

    /** 将玩家当前格挡翻倍（巩固） */
    void doubleBlock();

    /** 记录回合结束时需扣除的力量（活动肌肉的临时力量） */
    void loseStrengthAtTurnEnd(int amount);

    /** 本回合禁止再抽牌（战斗专注） */
    void forbidDrawThisTurn();

    /**
     * 激活一张能力牌（如残暴）：
     * 由战斗流程记录其持续效果，并在状态栏常驻显示其触发的能力。
     */
    void activatePower(Card.Kind kind);

    /** 卡牌结算后移出玩家手牌，并按“是否消耗”决定去向 */
    void onCardPlayed(Card c);

    /** 战斗未结束时刷新整场战斗界面 */
    void refreshIfAlive();
}
