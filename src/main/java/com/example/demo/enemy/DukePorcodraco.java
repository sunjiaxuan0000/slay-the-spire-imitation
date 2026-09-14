package com.example.demo.enemy;

import java.util.List;

/**
 * BOSS：猪龙鱼公爵
 * 拥有双阶段机制 — 血量降至 50% 时切换到二阶段轮盘并重置指针。
 */
public class DukePorcodraco extends Enemy {

    private final List<Step> secondPlan;

    public DukePorcodraco() {
        super("猪龙鱼公爵", 200, true, true, List.of(
                new Step(Intent.BUFF, 3),
                new Step(Intent.ATTACK, 13),
                new Step(Intent.WEAKEN, 3),
                new Step(Intent.DEFEND, 10),
                new Step(Intent.ATTACK, 16)
        ));
        portraitSize = 280;  // BOSS 立绘更大
        this.secondPlan = List.of(
                new Step(Intent.ATTACK, 18),
                new Step(Intent.DEFEND, 16),
                new Step(Intent.ATTACK, 20),
                new Step(Intent.WEAKEN, 4)
        );
    }

    @Override
    protected List<Step> getActivePlan() {
        return isSecondPhase ? secondPlan : super.getActivePlan();
    }

    /** 血量降至 50% 时进入二阶段：切换轮盘、重置指针 */
    @Override
    public void checkPhaseTransition() {
        if (!isSecondPhase && hp <= maxHp / 2) {
            isSecondPhase = true;
            resetPlanIndex();
        }
    }
}
