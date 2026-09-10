package com.example.demo.enemy;

import java.util.List;

/**
 * 普通怪：老兵邪教猪（第 6 层起替换邪教猪）
 * 与邪教猪同机制：第一回合施放仪式（永久获得每回合 +2 力量），之后攻击循环。
 * 行动轮盘：仪式 / 攻击 8（仪式只出现一次）。
 */
public class Veteran_Cultist_Pig extends Enemy {

    private final List<Step> cyclePlan;
    private boolean ritualDone = false;

    public Veteran_Cultist_Pig() {
        super("老兵邪教猪", 75, true, false, List.of(
                new Step(Intent.RITUAL, 2),
                new Step(Intent.ATTACK, 8)
        ));
        this.cyclePlan = List.of(
                new Step(Intent.ATTACK, 8)
        );
        portraitName = "邪教猪";  // 立绘复用邪教猪的图
        portraitSize = 170;       // 贴图更小
    }

    @Override
    protected List<Step> getActivePlan() {
        return ritualDone ? cyclePlan : super.getActivePlan();
    }

    @Override
    public void advance() {
        if (!ritualDone) {
            ritualDone = true;
            resetPlanIndex();  // 仪式执行完，切到攻击循环并归零指针
        } else {
            super.advance();
        }
    }
}
