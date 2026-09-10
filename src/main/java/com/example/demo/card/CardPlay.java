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

    /** 判断一张牌当前是否满足打出条件（轮到玩家、战斗未结束、能量足够）。 */
    public static boolean canPlay(Card c, BattleState s) {
        return s.isPlayerTurn() && !s.isBattleOver() && c.cost <= s.getEnergy();
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

        // 攻击：按段数结算，段间若战斗已结束（击杀）则停止
        if (c.damage > 0) {
            for (int i = 0; i < c.hits; i++) {
                int dmg = c.damage + s.getStrength();
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
            case BLEED -> {
                s.gainEnergy(2);
                if (s.loseHp(3, false)) return; // 自伤致死：终止结算
            }
            case RAGE -> s.gainEnergy(2);
            case OFFERING -> {
                if (s.loseHp(6, true)) return;  // 自伤致死：终止结算
                s.gainEnergy(2);
            }
            default -> {
            }
        }

        if (c.block > 0) s.addBlock(c.block);
        if (c.draw > 0) s.drawCards(c.draw);

        s.onCardPlayed(c);
        s.refreshIfAlive();
    }
}
