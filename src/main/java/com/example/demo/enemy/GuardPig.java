package com.example.demo.enemy;

import java.util.List;

/**
 * 精英怪：卫士猪
 * 行动轮盘：攻击 12 / 反伤 2 回合 / 防御 20 / 攻击 15 循环。
 * 反伤：处于反伤状态时，玩家对其造成伤害的 30% 会反弹给玩家。
 */
public class GuardPig extends Enemy {

    public GuardPig() {
        super("卫士猪", 70, true, false, true, List.of(
                new Step(Intent.ATTACK, 15),
                new Step(Intent.REFLECT, 2),
                new Step(Intent.DEFEND, 20),
                new Step(Intent.ATTACK, 18)
        ));
        this.reflectRate = 0.3;   // 30% 反伤
        this.isElite = true;      // 精英怪：战斗胜利后使用专属卡牌奖励权重
    }
}
