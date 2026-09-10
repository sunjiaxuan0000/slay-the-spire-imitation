package com.example.demo.enemy;

import java.util.List;

/**
 * 普通怪：大史莱姆（第 6 层起替换史莱姆）
 * 行动轮盘：攻击 16 / 防御 14 / 弱化 2 循环
 * 特殊：攻击后向玩家抽牌堆塞入 1 张黏液
 */
public class BigSlime extends Enemy {

    public BigSlime() {
        super("大史莱姆", 68, true, false, List.of(
                new Step(Intent.ATTACK, 16),
                new Step(Intent.DEFEND, 14),
                new Step(Intent.WEAKEN, 2)
        ));
        slimeOnAttack = 1;
        portraitName = "史莱姆";  // 立绘复用史莱姆的图
        portraitSize = 250;       // 贴图更大
    }
}
