package com.example.demo.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 卡牌奖励抽取：按品质概率（白 / 蓝 / 金）从卡池中抽取不重复的奖励牌。
 *
 * <p>品质代号取自 {@link Card.Kind#weight}：1 = 金卡，2 = 蓝卡，3 = 白卡。
 *
 * <p>基础概率：
 * <ul>
 *   <li>普通卡池：白 60% / 蓝 37% / 金 3%</li>
 *   <li>精英卡池：白 50% / 蓝 40% / 金 10%</li>
 * </ul>
 *
 * <p>怜悯偏移：维护一个普通池与精英池共用的偏移值，初始 -5%。
 * 每次单张抽取时，金卡概率加上该偏移值，增减的差额由白卡概率承担
 * （偏移为负时金卡概率反而降低、白卡概率升高）。若本次抽到的不是金卡，
 * 偏移值 +1%（上限 40%）；抽到金卡则偏移值重置为初始值 -5%。
 */
public final class CardRewardPool {

    /** 品质代号（与 {@link Card.Kind#weight} 一致） */
    public static final int GOLD = 1;
    public static final int BLUE = 2;
    public static final int WHITE = 3;

    // ---- 基础概率（百分比）----
    private static final double NORMAL_WHITE = 60, NORMAL_BLUE = 37, NORMAL_GOLD = 3;
    private static final double ELITE_WHITE = 50, ELITE_BLUE = 40, ELITE_GOLD = 10;

    // ---- 怜悯偏移参数 ----
    private static final double PITY_INIT = -5;
    private static final double PITY_STEP = 1;
    private static final double PITY_MAX = 40;

    /** 怜悯偏移值（百分比）：普通池与精英池共用，跨战斗保留 */
    private static double pity = PITY_INIT;

    private static final Random RND = new Random();

    private CardRewardPool() {
    }

    /** 重置怜悯偏移为初始值（新的一局开始时调用） */
    public static void resetPity() {
        pity = PITY_INIT;
    }

    /**
     * 奖励卡池：战斗胜利、遗物「混沌」的卡牌奖励都用这一份（29 张）。
     *
     * <p>不含初始牌组的打击 / 防御 / 痛击。具体抽哪几张交给
     * {@link #draw(List, int, boolean)}，所以这里只负责「池子里有什么」。</p>
     */
    public static List<Card> rewardPool() {
        return List.of(
                Card.sweep(), Card.bleed(),
                Card.pommelStrike(), Card.shrug(),
                Card.hammer(), Card.impregnable(),
                Card.doubleStrike(), Card.kindle(), Card.lightning(),
                Card.rage(), Card.offering(), Card.wildStrike(),
                Card.fortify(), Card.focus(), Card.shockwave(),
                Card.heavyBlade(), Card.adamantArm(), Card.brutality(), Card.flex(),
                Card.powerThrough(), Card.soulSever(), Card.uppercut(), Card.bodySlam(),
                Card.hemokinesis(), Card.limitBreak(), Card.feelNoPain(), Card.trueGrit(),
                Card.barrier(), Card.berserk());
    }

    /**
     * 商店货架：从 {@link #rewardPool()} 里随机抽 {@code count} 张<b>互不重复</b>的牌。
     *
     * <p>和战斗奖励的区别：这里是<b>纯均匀随机</b>，不走品质概率 / 怜悯那一套 ——
     * 商店里金卡应该少见是因为它贵，不是因为它抽不到。稀有度由
     * {@code ShopView.cardPrice} 体现在价格上（越稀有越贵）。</p>
     *
     * <p>⚠ 会显式过滤掉<b>初始卡</b>（打击 / 防御 / 痛击）和
     * <b>状态卡</b>（伤口 / 黏液）—— 那些不该出现在商店里。
     * 现在 {@code rewardPool()} 本来就没有它们，但池子以后会加牌，这里兜一道更稳。</p>
     */
    public static List<Card> shopStock(int count, Random rnd) {
        List<Card> pool = new ArrayList<>();
        for (Card c : rewardPool()) {
            if (isStarterOrStatus(c)) continue;
            pool.add(c);
        }
        List<Card> out = new ArrayList<>();
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            out.add(pool.remove(rnd.nextInt(pool.size())));
        }
        return out;
    }

    /** 初始牌组的三张 + 状态牌：不进商店、也不该当奖励发 */
    private static boolean isStarterOrStatus(Card c) {
        if (c.kind.type == Card.Type.STATUS) return true;
        return c.kind == Card.Kind.STRIKE
                || c.kind == Card.Kind.DEFEND
                || c.kind == Card.Kind.BASH;
    }

    /** 当前怜悯偏移值（百分比），供显示 / 调试 */
    public static double pity() {
        return pity;
    }

    /**
     * 从卡池中抽取 {@code count} 张不重复的奖励牌。
     * <p>每张都单独结算一次怜悯偏移（一次战斗奖励抽 3 张即结算 3 次）。
     *
     * @param pool  候选卡池
     * @param count 抽取张数
     * @param elite 是否精英战（决定使用精英池还是普通池的基础概率）
     */
    public static List<Card> draw(List<Card> pool, int count, boolean elite) {
        List<Card> remaining = new ArrayList<>(pool);
        List<Card> offers = new ArrayList<>();
        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            Card picked = drawOne(remaining, elite);
            remaining.remove(picked);
            offers.add(picked);
        }
        return offers;
    }

    /** 抽一张：先按品质概率掷出品质，再在该品质的剩余牌中随机取一张 */
    private static Card drawOne(List<Card> remaining, boolean elite) {
        int quality = rollQuality(elite);

        List<Card> candidates = new ArrayList<>();
        for (Card c : remaining) {
            if (c.kind.weight == quality) candidates.add(c);
        }
        if (candidates.isEmpty()) candidates = remaining; // 该品质已被抽空：退回整个剩余池
        Card picked = candidates.get(RND.nextInt(candidates.size()));

        if (picked.kind.weight == GOLD) {
            pity = PITY_INIT;                                   // 抽到金卡：偏移回归初始值
        } else {
            pity = Math.min(PITY_MAX, pity + PITY_STEP);        // 否则偏移 +1%（上限 40%）
        }
        return picked;
    }

    /** 按当前概率掷出品质代号：金 → 蓝 → 白依次判定 */
    private static int rollQuality(boolean elite) {
        double baseWhite = elite ? ELITE_WHITE : NORMAL_WHITE;
        double blue = elite ? ELITE_BLUE : NORMAL_BLUE;
        double baseGold = elite ? ELITE_GOLD : NORMAL_GOLD;

        // 金卡概率叠加偏移，截断到非负；增减的差额由白卡概率承担
        double gold = Math.max(0, baseGold + pity);
        double white = baseWhite - (gold - baseGold);

        double total = gold + blue + white; // 常规情况恰为 100
        double r = RND.nextDouble() * total;
        if (r < gold) return GOLD;
        if (r < gold + blue) return BLUE;
        return WHITE;
    }
}
