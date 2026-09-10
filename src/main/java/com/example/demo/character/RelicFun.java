package com.example.demo.character;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RelicFun {

    private static final List<Relic> STARTER_RELICS = List.of(
            new Relic("青铜怀表", "战斗开始时获得 2 点格挡"),
            new Relic("请假条", "每回合多抽 1 张牌"),
            new Relic("燃烧之血", "每次战斗结束后恢复 6 点生命")
    );

    private static final List<Relic> ELITE_RELICS = List.of(
            // —— 普通遗物（各 7%）——
            new Relic("红头骨", "当生命值 ≤ 50% 时，获得额外 3 点力量"),
            new Relic("猫", "每场战斗开始时获得 10 点格挡"),
            new Relic("孙子兵法", "若一回合未出牌，下回合获得 1 点额外能量"),
            new Relic("发条靴", "造成 ≤ 5 的未被格挡伤害时，提升为 8"),
            new Relic("赤牛", "每场战斗第一次攻击造成 8 点额外伤害"),
            new Relic("金刚杵", "每场战斗开始时获得 1 点力量"),
            new Relic("小血瓶", "每场战斗开始时恢复 2 点生命"),
            new Relic("草莓", "最大生命值提升 7 点"),
            new Relic("百年积木", "每场战斗第一次失去生命值时抽 3 张牌"),
            new Relic("奥利哈钢", "回合结束时若无格挡，获得 6 点格挡"),
            new Relic("古茶具套装", "篝火休息后下一场战斗开始时获得 2 点额外能量"),
            // —— 罕见遗物（各 5%）——
            new Relic("荔枝", "最大生命值提升 13 点"),
            new Relic("精致折扇", "一回合打出 3 张攻击牌时获得 4 点格挡"),
            new Relic("开信刀", "一回合打出 3 张技能牌时对敌人造成 5 点伤害"),
            new Relic("带骨肉", "战斗结束时若生命值 < 50%，恢复 12 点生命"),
            // —— 稀有遗物（3%）——
            new Relic("鸟面翁", "每打出一张技能牌恢复 2 点生命")
    );

    /** 与 ELITE_RELICS 一一对应的权重（百分比） */
    private static final List<Integer> ELITE_WEIGHTS = List.of(
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,   // 普通 ×11
            5, 5, 5, 5,                           // 罕见 ×4
            3                                      // 稀有 ×1
    );

    private static final List<Relic> EVENT_RELICS = List.of(
            new Relic("英雄宝典", "每场战斗开始时增加一张不消耗能量的能力牌"),
            new Relic("老牧师", "每场战斗结束后最大生命值增加 1"),
            new Relic("忘情牛肉面", "每场战斗开始时获得 3 点力量，仅第一回合有效"),
            new Relic("taffy", "每回合结束时生命值高于 50% 额外获得 5 点格挡"),
            new Relic("牛来", "每场战斗开始时对敌人造成 3 点伤害"),
            new Relic("奶龙", "第二回合开始时获得 12 点格挡")
    );

    public static boolean hasRelic(Player player, String name) {
        for (Relic r : player.relics) {
            if (r.name.equals(name)) return true;
        }
        return false;
    }

    public static void onBattleStart(Player player) {
        // 小血瓶：战斗开始时恢复 2 点生命
        if (hasRelic(player, "小血瓶")) {
            player.heal(2);
        }
    }

    public static void onBattleEnd(Player player) {
        if (hasRelic(player, "燃烧之血")) {
            player.heal(6);
        }
        // 带骨肉：战斗结束时若生命值 < 50%，恢复 12 点生命
        if (hasRelic(player, "带骨肉") && player.hp() * 2 < player.maxHp) {
            player.heal(12);
        }
        // 老牧师：战斗结束后最大生命值永久 +1
        if (hasRelic(player, "老牧师")) {
            player.increaseMaxHp(1);
        }
    }

    public static int extraDraw(Player player) {
        return hasRelic(player, "请假条") ? 1 : 0;
    }

    public static int startBlock(Player player, int turn) {
        int block = 0;
        if (turn == 1) {
            if (hasRelic(player, "青铜怀表")) {
                block += 2;
            }
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

    public static int extraStrength(Player player) {
        int strength = 0;
        // 红头骨：生命值 ≤ 50% 时 +3 力量
        if (hasRelic(player, "红头骨") && player.hp() * 2 <= player.maxHp) {
            strength += 3;
        }
        // 金刚杵：战斗开始时 +1 力量
        if (hasRelic(player, "金刚杵")) {
            strength += 1;
        }
        return strength;
    }

    public static int boostLowDamage(Player player, int dmg) {
        if (hasRelic(player, "发条靴") && dmg > 0 && dmg <= 5) {
            return 8;
        }
        return dmg;
    }

    public static int firstAttackBonus(Player player) {
        if (hasRelic(player, "赤牛")) {
            return 8;
        }
        return 0;
    }

    public static int teaSetEnergy(Player player) {
        // 古茶具套装：篝火休息后下一场战斗 +2 能量
        if (hasRelic(player, "古茶具套装") && player.restedAtCampfire) {
            player.restedAtCampfire = false; // 消耗标记
            return 2;
        }
        return 0;
    }

    public static List<Relic> starterRelics() {
        return new ArrayList<>(STARTER_RELICS);
    }

    public static Relic randomEliteRelic(Player player) {
        // 过滤已持有的遗物，同时保留对应权重
        List<Relic> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (int i = 0; i < ELITE_RELICS.size(); i++) {
            Relic r = ELITE_RELICS.get(i);
            if (player.relics.stream().noneMatch(h -> h.name.equals(r.name))) {
                pool.add(r);
                weights.add(ELITE_WEIGHTS.get(i));
            }
        }
        if (pool.isEmpty()) return null;

        // 加权随机选择
        int total = 0;
        for (int w : weights) total += w;
        int roll = new Random().nextInt(total);
        int cumulative = 0;
        Relic gained = pool.get(pool.size() - 1); // 默认最后一个
        for (int i = 0; i < pool.size(); i++) {
            cumulative += weights.get(i);
            if (roll < cumulative) {
                gained = pool.get(i);
                break;
            }
        }

        player.addRelic(gained);
        // 草莓：获得时立即增加 7 点最大生命值
        if (gained.name.equals("草莓")) {
            player.increaseMaxHp(7);
        }
        // 荔枝：获得时立即增加 13 点最大生命值
        if (gained.name.equals("荔枝")) {
            player.increaseMaxHp(13);
        }
        return gained;
    }

    public static Relic randomEventRelic(Player player) {
        List<Relic> pool = filterOwned(EVENT_RELICS, player);
        if (pool.isEmpty()) return null;
        Relic gained = pool.get(new Random().nextInt(pool.size()));
        player.addRelic(gained);
        return gained;
    }

    private static List<Relic> filterOwned(List<Relic> pool, Player player) {
        List<Relic> available = new ArrayList<>(pool);
        available.removeIf(r -> player.relics.stream().anyMatch(h -> h.name.equals(r.name)));
        return available;
    }
}