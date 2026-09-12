package com.example.demo.character;

import com.example.demo.card.Card;
import com.example.demo.enemy.Enemy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 遗物效果逻辑层：所有遗物的触发逻辑集中在此类。
 * 新增遗物时只需修改 {@link Relic}（数据）和本类（逻辑）。
 *
 * 设计原则：本类方法只做"计算 / 查询"，不直接修改战斗状态（格挡、力量等）。
 * 调用方（BattleView / RoomView）根据返回值自行应用。
 */
public class RelicFun {

    /* ================= 查询 ================= */

    public static boolean hasRelic(Player player, String name) {
        for (Relic r : player.relics) {
            if (r.name.equals(name)) return true;
        }
        return false;
    }

    public static List<Relic> filterOwned(List<Relic> pool, Player player) {
        List<Relic> available = new ArrayList<>(pool);
        available.removeIf(r -> player.relics.stream().anyMatch(h -> h.name.equals(r.name)));
        return available;
    }

    /* ================= 战斗开始 ================= */

    /**
     * 战斗开始时触发（纯效果）：请假条、小血瓶、忘情牛肉面、金刚杵。
     * 请假条直接在 player 上递减计数器；小血瓶回血。
     * 力量增益由返回值告诉调用方。
     *
     * @return 战斗开始时应增加的力量值
     */
    public static int onBattleStart(Player player) {
        // 请假条：接下来的三场战斗怪物血量变为 1
        if (player.leaveNoteBattles > 0) {
            player.leaveNoteBattles--;
        }
        // 小血瓶：战斗开始时恢复 2 点生命
        if (hasRelic(player, "小血瓶")) {
            player.heal(2);
        }
        int strength = 0;
        // 忘情牛肉面：战斗开始时获得 3 点力量，仅第一回合有效
        if (hasRelic(player, "忘情牛肉面")) {
            strength += 3;
        }
        // 金刚杵：战斗开始时获得 1 点力量（永久）
        if (hasRelic(player, "金刚杵")) {
            strength += 1;
        }
        return strength;
    }

    /** 牛来：战斗开始时对敌人造成的伤害（3），无此遗物返回 0 */
    public static int battleStartDamage(Player player) {
        return hasRelic(player, "牛来") ? 3 : 0;
    }

    /** 请假条：战斗开始时是否将怪物血量设为 1 */
    public static boolean isLeaveNoteActive(Player player) {
        return player.leaveNoteBattles > 0;
    }

    /* ================= 回合开始 ================= */

    /**
     * 回合开始时计算额外能量：奴隶贩子颈环、古茶具套装、孙子兵法。
     *
     * @param playedAttackLastTurn 上一回合是否打出过<b>攻击牌</b>（由调用方维护）。
     *                             孙子兵法只关心攻击牌 —— 上一回合一张攻击牌都没出，
     *                             这一回合才开始时 +1 能量。
     */
    public static int extraEnergy(Player player, Enemy enemy, int turn, boolean playedAttackLastTurn) {
        int e = 0;
        // 奴隶贩子颈环：Boss 战和精英战每回合 +1 能量
        if ((enemy.isBoss || enemy.isElite) && hasRelic(player, "奴隶贩子颈环")) {
            e += 1;
        }
        // 古茶具套装：篝火休息后下一场战斗第一回合 +2 能量
        if (turn == 1 && hasRelic(player, "古茶具套装") && player.restedAtCampfire) {
            player.restedAtCampfire = false; // 消耗标记
            e += 2;
        }
        // 孙子兵法：上一回合没出过攻击牌，则本回合开始 +1 能量
        // （turn > 1：第一回合没有「上一回合」，不给）
        if (!playedAttackLastTurn && turn > 1 && hasRelic(player, "孙子兵法")) {
            e += 1;
        }
        return e;
    }

    /** 回合开始时计算额外格挡：猫、奶龙 */
    public static int startBlock(Player player, int turn) {
        int block = 0;
        if (turn == 1) {
            if (hasRelic(player, "猫")) {
                block += 10;
            }
        }
        // 奶龙：第二回合开始时获得 12 点格挡
        if (turn == 2 && hasRelic(player, "奶龙")) {
            block += 12;
        }
        return block;
    }

    /** 第一回合开始时往手牌中添加额外卡牌：英雄宝典 */
    public static void addTurnStartCards(Player player, List<Card> hand, int turn) {
        // 英雄宝典：战斗开始时增加一张免费能力牌
        if (turn == 1 && hasRelic(player, "英雄宝典")) {
            hand.add(Card.freePower());
        }
    }

    /* ================= 回合结束 ================= */

    /**
     * 回合结束时计算额外格挡：奥利哈钢、taffy。
     * @param currentBlock 当前格挡值（忘情牛肉面判断用）
     * @param hp           当前生命
     * @param maxHp        最大生命
     * @return 应额外获得的格挡值
     */
    public static int endTurnBlock(Player player, int currentBlock, int hp, int maxHp) {
        int block = 0;
        // 奥利哈钢：回合结束时若无格挡，获得 6 点格挡
        if (currentBlock == 0 && hasRelic(player, "奥利哈钢")) {
            block += 6;
        }
        // taffy：回合结束时生命值高于 50% 额外获得 5 点格挡
        if (hp * 2 > maxHp && hasRelic(player, "taffy")) {
            block += 5;
        }
        return block;
    }

    /** 回合结束时是否应移除临时力量（忘情牛肉面第一回合结束后 -3 力量） */
    public static boolean shouldRemoveNoodleBonus(Player player, int turn) {
        return turn == 1 && hasRelic(player, "忘情牛肉面");
    }

    /* ================= 受伤触发 ================= */

    /**
     * 玩家首次受伤时是否触发百年积木（抽 3 张牌）。
     * @param firstDamage 是否为本场战斗首次受伤（由调用方维护标记）
     * @return true 表示应抽牌
     */
    public static boolean shouldDrawOnFirstDamage(Player player, boolean firstDamage) {
        return firstDamage && hasRelic(player, "百年积木");
    }

    /* ================= 战斗结束 ================= */

    /** 战斗结束后触发：燃烧之血（战士被动）、带骨肉、老牧师 */
    public static void onBattleEnd(Player player) {
        // 战士被动：燃烧之血 —— 每次战斗结束后恢复 6 点生命
        player.heal(6);
        // 带骨肉：战斗结束时若生命值 < 50%，恢复 12 点生命
        if (hasRelic(player, "带骨肉") && player.hp() * 2 < player.maxHp) {
            player.heal(12);
        }
        // 老牧师：战斗结束后最大生命值永久 +1
        if (hasRelic(player, "老牧师")) {
            player.increaseMaxHp(1);
        }
    }

    /* ================= Boss 胜利奖励 ================= */

    /**
     * Boss 战胜利后，根据获得的 Boss 遗物返回后续动作类型：
     * "REMOVE_CARDS" / "BELL_OFFERS" / null
     *
     * <p>「拾取」了 Boss 遗物才该调这个 —— 丢弃了就别触发它的后续效果。</p>
     */
    public static String bossVictoryAction(Relic bossRelic) {
        if (bossRelic == null) return null;
        if (bossRelic.name.equals("空鸟笼")) return "REMOVE_CARDS";
        if (bossRelic.name.equals("召唤铃铛")) return "BELL_OFFERS";
        return null;
    }

    /* ================= 获得遗物时 ================= */

    /**
     * 获得遗物时的即时效果：保温杯、请假条、破镜、召唤铃铛、混沌。
     * @param removeCards 卡牌移除回调（破镜需要 UI 交互，由调用方提供）
     */
    public static void onRelicObtained(Player player, Relic relic, Runnable removeCards) {
        // 保温杯：获得时永久增加 8 点最大生命值
        if (relic.name.equals("保温杯")) {
            player.increaseMaxHp(8);
        }
        // 请假条：获得时设置 3 场战斗生效
        if (relic.name.equals("请假条")) {
            player.leaveNoteBattles = 3;
        }
        // 召唤铃铛：代价 —— 往牌组里永久塞一张「伤口」（三份遗物不是白拿的）
        // ⚠ 这张伤口必须留到下一场战斗。它在 Boss 战胜利之后才加进来，
        //   而 BattleView.victory() 里的 clearStatusCards() 在更早的胜利瞬间就跑完了，
        //   所以不会被清掉。以后如果调换这两步的顺序，这里会被悄悄抹掉。
        if (relic.name.equals("召唤铃铛")) {
            player.deck.add(Card.wound());
        }
        // 破镜：删除一张卡牌
        if (relic.name.equals("破镜") && removeCards != null) {
            removeCards.run();
        }
        // 混沌：本局地图变异 —— 置上标记即可，剩下的全靠它生效：
        //   · 图标：MapView 看到 player.chaos 就把非固定层节点画成「事件」
        //   · 进入：HelloApplication.handleArrive 看到 player.chaos 就等概率改判房间
        //   · 三次卡牌奖励是 UI，放在起点房间（RoomView.giveChaosCardRewards）
        if (relic.name.equals("混沌")) {
            player.chaos = true;
        }
    }

    /* ================= 遗物获取（随机池） ================= */

    public static int extraDraw(Player player) {
        return 0;
    }

    public static List<Relic> starterRelics() {
        return Relic.starterRelics();
    }

    /**
     * 起点 NPC 的候选遗物：从专属的「起点遗物池」里随机抽 {@code count} 个。
     *
     * <p>抽之前先滤掉玩家已经有的 —— 否则选中一件已有的遗物时，
     * {@link Player#addRelic} 会因重名直接忽略，玩家等于白选一次，
     * 而提示还会说「已获得」。</p>
     *
     * <p>池子不够 {@code count} 个时就给多少算多少，不报错。</p>
     */
    public static List<Relic> pickStarterOptions(Player player, int count) {
        List<Relic> pool = filterOwned(Relic.starterRelics(), player);
        Collections.shuffle(pool);
        return new ArrayList<>(pool.subList(0, Math.min(count, pool.size())));
    }

    /** 全部遗物（起点 + 精英 + 事件 + Boss），开发者模式面板用它列出所有可加/可删的遗物 */
    public static List<Relic> allRelics() {
        return Relic.allRelics();
    }

    /* ----------------- 只挑不拿（供「拾取 / 丢弃」界面用） ----------------- */

    /**
     * 按精英池权重挑一个玩家还没有的精英遗物，<b>不动玩家状态</b>。
     *
     * <p>池空时返回 null。要让遗物真正入账，得玩家点「拾取」后再调
     * {@link #grantRelic} —— 这样才能做到「丢弃」是真的没拿，
     * 而不是先加进 relics 再想办法减掉（relics 里还有即时效果，减不干净）。</p>
     */
    public static Relic pickEliteRelic(Player player) {
        return pickEliteRelic(player, Set.of());
    }

    /**
     * 同上，但额外排除 {@code exclude} 里的名字 —— 连着抽多个时用它避免重复。
     *
     * @param exclude 本次连抽里已经出现过的遗物名（可为 {@code Set.of()}）
     */
    public static Relic pickEliteRelic(Player player, Set<String> exclude) {
        List<Relic> eliteRelics = Relic.eliteRelics();
        List<Integer> eliteWeights = Relic.eliteWeights();
        List<Relic> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (int i = 0; i < eliteRelics.size(); i++) {
            Relic r = eliteRelics.get(i);
            if (exclude.contains(r.name)) continue;
            if (player.relics.stream().noneMatch(h -> h.name.equals(r.name))) {
                pool.add(r);
                weights.add(eliteWeights.get(i));
            }
        }
        if (pool.isEmpty()) return null;

        int total = 0;
        for (int w : weights) total += w;
        int roll = new Random().nextInt(total);
        int cumulative = 0;
        Relic picked = pool.get(pool.size() - 1);
        for (int i = 0; i < pool.size(); i++) {
            cumulative += weights.get(i);
            if (roll < cumulative) {
                picked = pool.get(i);
                break;
            }
        }
        return picked;
    }

    /**
     * 连着抽 {@code count} 个<b>互不重复</b>的精英遗物（只挑不拿）。
     * 召唤铃铛的「三连遗物获取界面」用它。
     *
     * <p>每次抽都按 {@link Relic#eliteWeights()} 加权，所以稀有度分布和单抽一致；
     * 已抽到的会排除掉，免得同一个遗物连着弹两次（丢弃之后尤其容易撞）。
     * 池子不够 {@code count} 个就给多少算多少。</p>
     */
    public static List<Relic> pickEliteOptions(Player player, int count) {
        List<Relic> picked = new ArrayList<>();
        Set<String> taken = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Relic r = pickEliteRelic(player, taken);
            if (r == null) break;
            picked.add(r);
            taken.add(r.name);
        }
        return picked;
    }

    /** 从事件遗物池挑一个玩家还没有的（只挑不拿；池空返回 null） */
    public static Relic pickEventRelic(Player player) {
        List<Relic> pool = filterOwned(Relic.eventRelics(), player);
        if (pool.isEmpty()) return null;
        return pool.get(new Random().nextInt(pool.size()));
    }

    /** 从 Boss 遗物池挑一个玩家还没有的（只挑不拿；池空返回 null） */
    public static Relic pickBossRelic(Player player) {
        List<Relic> pool = filterOwned(Relic.bossRelics(), player);
        if (pool.isEmpty()) return null;
        return pool.get(new Random().nextInt(pool.size()));
    }

    /* ----------------- 真正入账 ----------------- */

    /**
     * 把遗物记进玩家状态，并结算「获得时」的即时效果：
     * 草莓 +7 最大生命、荔枝 +13 最大生命。
     *
     * <p>只在玩家点「拾取」时调用。「丢弃」就什么都不做。</p>
     *
     * <p>保温杯 / 请假条 / 破镜的即时效果由 {@link Player#addRelic} 内部
     * 转交 {@link #onRelicObtained}，这里不用重复处理。</p>
     */
    public static void grantRelic(Player player, Relic r) {
        if (r == null) return;
        player.addRelic(r);
        if (r.name.equals("草莓")) {
            player.increaseMaxHp(7);
        }
        if (r.name.equals("荔枝")) {
            player.increaseMaxHp(13);
        }
    }

    /* ----------------- 挑 + 拿（给不需要询问去留的调用方） ----------------- */

    /** 随机拿一个精英遗物（直接入账，不询问去留） */
    public static Relic randomEliteRelic(Player player) {
        Relic gained = pickEliteRelic(player);
        grantRelic(player, gained);
        return gained;
    }

    /** 随机拿一个事件遗物（直接入账，不询问去留） */
    public static Relic randomEventRelic(Player player) {
        Relic gained = pickEventRelic(player);
        grantRelic(player, gained);
        return gained;
    }

    /** 击败 Boss 后随机拿一个 Boss 遗物（直接入账，不询问去留；池为空时返回 null） */
    public static Relic randomBossRelic(Player player) {
        Relic gained = pickBossRelic(player);
        grantRelic(player, gained);
        return gained;
    }
}
