package com.example.demo.enemy;

import java.util.List;

/**
 * 普通怪：史莱姆
 * 行动轮盘：攻击 8 / 防御 5 / 弱化 2 循环
 * 特殊：攻击后向玩家抽牌堆塞入 1 张黏液
 */
public class Slime extends Enemy {

    public Slime() {
        super("史莱姆", 36, true, false, List.of(
                new Step(Intent.ATTACK, 8),
                new Step(Intent.DEFEND, 5),
                new Step(Intent.WEAKEN, 2)
        ));
        slimeOnAttack = 1;
    }
}
