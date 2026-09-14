package com.example.demo.enemy;

import java.util.List;

/**
 * 普通怪：猪？（后 5 层高级池）
 * 神秘的猪，来源不明……
 * 拥有二阶段：第 5 回合开始时切换轮盘并重置指针。
 * 注意：isBoss = false，但二阶段机制独立运作。
 */
public class MysteryPig extends Enemy {

    private final List<Step> secondPlan;
    private int turnCount = 0;

    public MysteryPig() {
        super("猪？", 100, true, false, List.of(
                new Step(Intent.ATTACK, 12),
                new Step(Intent.DEFEND, 20),
                new Step(Intent.SPIT,3),
                new Step(Intent.DEFEND,30 )
        ));
        this.secondPlan = List.of(
                new Step(Intent.ATTACK, 30)
        );
    }

    @Override
    protected List<Step> getActivePlan() {
        return isSecondPhase ? secondPlan : super.getActivePlan();
    }

    /** 第 5 回合（turnCount >= 5）时进入二阶段：切换轮盘、重置指针 */
    @Override
    public void checkPhaseTransition() {
        if (!isSecondPhase && turnCount >= 5) {
            isSecondPhase = true;
            resetPlanIndex();
        }
    }

    @Override
    public void advance() {
        super.advance();
        turnCount++;
    }
}
