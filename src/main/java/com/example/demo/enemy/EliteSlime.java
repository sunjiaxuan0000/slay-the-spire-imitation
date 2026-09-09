package com.example.demo.enemy;

import java.util.List;

/**
 * 精英怪：精英史莱姆
 * 血量更高、行动更凶（含虚弱我方）
 * 行动轮盘：强化+2 / 攻击 10 / 防御 8 / 虚弱 2 / 攻击 12
 */
public class EliteSlime extends Enemy {

    public EliteSlime() {
        super("精英史莱姆", 45, false, false, List.of(
                new Step(Intent.BUFF, 2),
                new Step(Intent.ATTACK, 10),
                new Step(Intent.DEFEND, 8),
                new Step(Intent.WEAKEN, 2),
                new Step(Intent.ATTACK, 12)
        ));
    }
}
