package com.example.demo.enemy;

import java.util.List;

/**
 * 普通怪：史莱姆（含大史莱姆变体）。
 * 行动轮盘：攻击 / 防御 / 弱化 2 循环。
 * 特殊：攻击后向玩家抽牌堆塞入 1 张黏液。
 */
public class Slime extends Enemy {

    private Slime(String name, String portraitName, int hp, int attack, int defend, int portraitSize) {
        super(name, hp, true, false, List.of(
                new Step(Intent.ATTACK, attack),
                new Step(Intent.DEFEND, defend),
                new Step(Intent.WEAKEN, 2)
        ));
        slimeOnAttack = 1;
        this.portraitName = portraitName;
        this.portraitSize = portraitSize;
    }

    /** 基础史莱姆（前 5 层） */
    public static Slime base() {
        return new Slime("史莱姆", null, 28, 8, 5, 210);
    }

    /** 大史莱姆（第 6 层起，复用史莱姆立绘但更大） */
    public static Slime big() {
        return new Slime("大史莱姆", "史莱姆", 68, 16, 14, 250);
    }
}
