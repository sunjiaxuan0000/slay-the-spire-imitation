package com.example.demo.enemy;

import java.util.List;

/**
 * 普通怪：自爆猪
 * 行动轮盘：攻击 10 / 防御 5 / 蓄势 1 层 循环。
 * 特殊机制：
 *   1. 每层蓄势使自爆伤害 +16；
 *   2. HP 归 0 时不立即死亡，锁血 1 点并强制增加一次自爆意图，
 *      自爆造成「蓄势层数 × 16」伤害，自爆后自身死亡。
 */
public class BoomPig extends Enemy {

    /** 每层蓄势对应的自爆伤害 */
    public static final int DAMAGE_PER_STACK = 16;

    public BoomPig() {
        super("自爆猪", 42, true, false, List.of(
                new Step(Intent.ATTACK, 10),
                new Step(Intent.DEFEND, 5),
                new Step(Intent.CHARGE, 1)
        ));
        chargeDamagePerStack = DAMAGE_PER_STACK;
        portraitName = "自爆猪";
    }

    /** 锁血后：当前意图强制变为自爆（伤害随蓄势层数实时计算） */
    @Override
    public Step current() {
        return deathLocked
                ? new Step(Intent.EXPLODE, explodeDamage())
                : super.current();
    }

    /** HP 归 0 时锁血，拦截死亡一次 */
    @Override
    public boolean triggerDeathLock() {
        if (deathLocked) return false; // 已锁过，不再拦截
        deathLocked = true;
        return true;
    }
}
