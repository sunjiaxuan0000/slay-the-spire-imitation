package com.example.demo.card;

/**
 * 出牌规则与卡牌效果结算（从 BattleView 抽取）。
 * <p>
 * 只依赖 {@link BattleState} 上下文，不直接操作 UI / 具体战斗流程，
 * 便于出牌逻辑的集中维护与复用。
 */
public final class CardPlay {

    private CardPlay() {
    }

    /** 判断一张牌当前是否满足打出条件（牌本身可打出、轮到玩家、战斗未结束、能量足够）。 */
    public static boolean canPlay(Card c, BattleState s) {
        return c.isPlayable() && s.isPlayerTurn() && !s.isBattleOver() && c.cost <= s.getEnergy();
    }

    /**
     * 打出卡牌并结算其效果。
     * <p>
     * 结算顺序（与原 BattleView.play 保持一致）：
     * 先按段数结算攻击伤害（叠加力量 / 虚弱 / 易伤），
     * 再结算各牌种特殊效果（易伤、力量、回能、自伤等），
     * 然后结算格挡与抽牌，最后移出手牌并按是否消耗入弃牌堆。
     */
    public static void play(Card c, BattleState s) {
        if (!canPlay(c, s)) return;

        s.spendEnergy(c.cost);

        // 断魂术：先失去 2 点生命作为代价（致死则终止结算，不造成伤害）
        if (c.kind == Card.Kind.SOUL_SEVER && s.loseHp(2, false)) return;

        // 御血术：先失去 2 点生命作为代价（致死则终止结算，不造成伤害）
        if (c.kind == Card.Kind.HEMOKINESIS && s.loseHp(2, false)) return;

        // 攻击：按段数结算，段间若战斗已结束（击杀）则停止
        if (c.damage > 0) {
            int strength = s.getStrength();
            if (c.kind == Card.Kind.HEAVY_BLADE) strength *= 3; // 重刃：力量按 3 倍计入
            for (int i = 0; i < c.hits; i++) {
                int dmg = c.damage + strength;
                if (s.getWeakTurns() > 0) dmg = dmg * 3 / 4;
                if (s.getEnemyVulnerable() > 0) dmg = dmg * 3 / 2;
                s.damageEnemy(dmg);
                if (s.isBattleOver()) break;
            }
        }

        // 各牌种的附加效果
        switch (c.kind) {
            case BASH -> s.addEnemyVulnerable(2);
            case LIGHTNING -> s.addEnemyVulnerable(1);
            case KINDLE -> s.gainStrength(2);
            case WILD_STRIKE -> s.addToDrawPile(Card.wound());
            case BLEED -> {
                s.gainEnergy(2);
                if (s.loseHp(3, false)) return; // 自伤致死：终止结算
            }
            case RAGE -> s.gainEnergy(2);
            case OFFERING -> {
                if (s.loseHp(6, true)) return;  // 自伤致死：终止结算
                s.gainEnergy(2);
            }
            case FORTIFY -> s.doubleBlock();            // 巩固：当前格挡翻倍
            case FOCUS -> {                             // 战斗专注：抽 3 张，本回合禁抽
                s.drawCards(3);
                s.forbidDrawThisTurn();
            }
            case SHOCKWAVE -> {                         // 震荡波：敌人 4 虚弱 / 4 易伤
                s.addEnemyWeak(4);
                s.addEnemyVulnerable(4);
            }
            case ADAMANT_ARM -> s.addEnemyWeak(2);      // 金刚臂：敌人 2 层虚弱
            case BRUTALITY -> s.activatePower(Card.Kind.BRUTALITY); // 残暴：每回合失去 1 血多抽 1 张
            case FLEX -> {                              // 活动肌肉：+2 力量，回合结束 -2
                s.gainStrength(2);
                s.loseStrengthAtTurnEnd(2);
            }
            case POWER_THROUGH -> {                      // 硬撑：将两张伤口加入手牌
                s.addToHand(Card.wound());
                s.addToHand(Card.wound());
            }
            case UPPERCUT ->{                       // 上勾拳：敌人 1 虚弱 / 1 易伤
                s.addEnemyWeak(1);
                s.addEnemyVulnerable(1);
            } 
            case BODY_SLAM -> s.damageEnemy(s.getBlock()); // 全身撞击：对敌人当造成前格挡值伤害
            case LIMIT_BREAK -> s.gainStrength(s.getStrength()); // 突破极限：将你的力量翻倍
            default -> {
            }
        }

        if (c.block > 0) s.addBlock(c.block);
        if (c.draw > 0) s.drawCards(c.draw);

        s.onCardPlayed(c);
        s.refreshIfAlive();
    }
}
