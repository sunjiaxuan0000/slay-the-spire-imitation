package com.example.demo.enemy;

import java.util.List;

/**
 * BOSS：巨猪骑士。
 *
 * 基础数值：15 血 / 500 护甲，虚弱 3 回合，强化 +3 力量。
 * 意图轮盘（全程不变）：攻击 / 虚弱 / 强化 / 攻击 / 虚弱 循环。
 *
 * 特殊机制：
 *   1. 护甲不会在回合开始时清空（普通怪的格挡每回合清空）；
 *   2. 每回合开始固定自损当前护甲值的 4%；
 *   3. 攻击数值动态变化，且吃自身力量加成：
 *      护甲掉到 50% 之前：5 + 已损护甲 × 3%
 *      护甲掉到 50% 之后：10 + 已损护甲 × 2%
 *   4. 攻击不直接扣血：按正常伤害逻辑（虚弱、玩家格挡吸收先结算）得出应造成伤害后，
 *      削减玩家血量上限，削减数 = 应造成伤害 × 80%。
 */
public class GiantBoarKnight extends Enemy {

    /** 初始（最大）护甲 */
    public static final int MAX_ARMOR = 500;

    /** 护甲剩余 50% 的阈值：掉到该值（含）以下后攻击公式切换 */
    private static final int ARMOR_BROKEN_THRESHOLD = MAX_ARMOR / 2;

    /** 每回合开始自损当前护甲的百分比 */
    private static final int ARMOR_DECAY_PERCENT = 4;

    public GiantBoarKnight() {
        super("巨猪骑士", 15, true, true, List.of(
                new Step(Intent.ATTACK, 5),
                new Step(Intent.WEAKEN, 3),
                new Step(Intent.BUFF, 2)
        ));
        block = MAX_ARMOR;
        portraitSize = 300;
    }

    /** 已损失的护甲值 */
    public int lostArmor() {
        return MAX_ARMOR - block;
    }

    /** 护甲是否已掉到 50%（含）以下 */
    public boolean isArmorBroken() {
        return block <= ARMOR_BROKEN_THRESHOLD;
    }

    @Override
    public boolean isBlockPersistent() {
        return true;
    }

    @Override
    public boolean cutsMaxHpOnAttack() {
        return true;
    }

    /** 攻击基础值（之后照常吃力量、虚弱结算） */
    @Override
    public int baseAttackDamage(Step s) {
        int lost = lostArmor();
        if (isArmorBroken()) {
            return 10 + lost * 2 / 100;
        }
        return 5 + lost * 3 / 100;
    }

    /** 回合开始：仪式（无）+ 护甲自损 4%；护甲本身不被清空 */
    @Override
    public void onTurnStart() {
        super.onTurnStart();
        block = Math.max(0, block - block * ARMOR_DECAY_PERCENT / 100);
    }
}
