package com.example.demo.enemy;

import java.util.List;

/**
 * 邪教猪：第一回合施放仪式（永久获得每回合 +2 力量），之后攻击与防御循环。
 * 行动轮盘：仪式 / 攻击 6 / 防御 8（仪式只出现一次）。
 */
public class Cultist_Pig extends Enemy {

    private final List<Step> cyclePlan;
    private boolean ritualDone = false;

    public Cultist_Pig() {
        super("邪教猪", 50, true, false, List.of(
                new Step(Intent.RITUAL, 2),
                new Step(Intent.ATTACK, 6)
        ));
        this.cyclePlan = List.of(
                new Step(Intent.ATTACK, 6)
        );
    }

    @Override
    protected List<Step> getActivePlan() {
        return ritualDone ? cyclePlan : super.getActivePlan();
    }

    @Override
    public void advance() {
        if (!ritualDone) {
            ritualDone = true;
            resetPlanIndex();  // 仪式执行完，切到攻击防御循环并归零指针
        } else {
            super.advance();
        }
    }
}
