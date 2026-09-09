package com.example.demo.enemy;

import java.util.List;

/**
 * 普通怪：史莱姆
 * 行动轮盘：攻击 8 / 防御 5 / 攻击 7 / 强化+2 循环
 */
public class Slime extends Enemy {

    public Slime() {
        super("史莱姆", 28, true, false, List.of(
                new Step(Intent.ATTACK, 8),
                new Step(Intent.DEFEND, 5),
                new Step(Intent.ATTACK, 7),
                new Step(Intent.BUFF, 2)
        ));
    }
}
