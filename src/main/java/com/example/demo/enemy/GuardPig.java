package com.example.demo.enemy;

import java.util.List;

/**
 * 精英怪：卫士猪
 * 行动轮盘：攻击 12 / 反伤 2 回合 / 防御 20 / 攻击 15 循环。
 * 反伤：处于反伤状态时，玩家对其造成伤害的 30% 会反弹给玩家。
 */
public class GuardPig extends Enemy {

    public GuardPig() {
        super("卫士猪", 50, true, false, List.of(
                new Step(Intent.ATTACK, 12),
                new Step(Intent.REFLECT, 2),
                new Step(Intent.DEFEND, 20),
                new Step(Intent.ATTACK, 15)
        ));
        this.reflectRate = 0.3;   // 30% 反伤
    }
}
