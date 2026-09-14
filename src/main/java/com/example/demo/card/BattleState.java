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

    /**
     * 玩家敏捷层数。
     *
     * <p>和力量对称，但作用在<b>格挡</b>上：每次获得格挡，实际得到「原数值 + 敏捷」。
     * （力量的加成写在攻击伤害里，敏捷的加成写在格挡里。）</p>
     */
    int getDexterity();

    /** 增加玩家敏捷 */
    void gainDexterity(int amount);

    /** 玩家剩余虚弱回合 */
    int getWeakTurns();

    /** 敌人剩余易伤层数 */
    int getEnemyVulnerable();

    /** 增加敌人易伤层数 */
    void addEnemyVulnerable(int amount);

    /** 玩家获得易伤层数：受到的攻击伤害 ×1.5，每回合开始时减少 1 层（狂暴） */
    void addPlayerVulnerable(int amount);

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

    /**
     * 将一张牌直接加入手牌（如“硬撑”塞入“伤口”），不受抽牌禁令影响；
     * 若手牌已达上限，则改放入抽牌堆。
     */
    void addToHand(Card c);

    /** 将玩家当前格挡翻倍（巩固） */
    void doubleBlock();

    /** 记录回合结束时需扣除的力量（活动肌肉的临时力量） */
    void loseStrengthAtTurnEnd(int amount);

    /**
     * 取走「本场第一次攻击」的额外伤害（赤牛 +8），取完归零。
     *
     * <p>⚠ 只在<b>真的打出攻击牌</b>时调用 —— 悬停预览卡面不能调这个
     * （那会把一次性加成白白耗掉）。</p>
     */
    int consumeFirstAttackBonus();

    /**
     * <b>只看不拿</b>地询问「本场第一次攻击」的额外伤害（赤牛 +8）。
     *
     * <p>给<b>卡面显示 / 悬停预览</b>用 —— 它们要显示含加成的数值，
     * 但显然不能把一次性加成耗光，所以调这个而不是
     * {@link #consumeFirstAttackBonus()}（那个是取走即作废）。</p>
     */
    int peekFirstAttackBonus();

    /** 本回合禁止再抽牌（战斗专注） */
    void forbidDrawThisTurn();

    /**
     * 激活一张能力牌（如残暴）：
     * 由战斗流程记录其持续效果，并在状态栏常驻显示其触发的能力。
     */
    void activatePower(Card card);

    /**
     * 消耗手牌中所有非攻击牌（断魂斩）。
     * <p>这些牌确实被消耗，会触发“无惧疼痛”等消耗联动效果。
     */
    void exhaustNonAttackCardsInHand();

    /**
     * 随机消耗手牌中一张牌（坚毅）。
     * <p>手牌为空时不做任何处理。被消耗的牌会触发“无惧疼痛”等消耗联动效果。
     */
    void exhaustRandomHandCard();

    /** 卡牌结算后移出玩家手牌，并按“是否消耗”决定去向 */
    void onCardPlayed(Card c);

    /** 战斗未结束时刷新整场战斗界面 */
    void refreshIfAlive();
}
