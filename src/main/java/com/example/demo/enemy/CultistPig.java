package com.example.demo.enemy;

import java.util.List;

/**
 * 普通怪：邪教猪（含老兵邪教猪变体）。
 * 第一回合施放仪式（永久获得每回合 +2 力量），之后攻击循环。
 */
public class CultistPig extends Enemy {

    private final List<Step> cyclePlan;
    private boolean ritualDone = false;

    private CultistPig(String name, String portraitName, int hp, int attack, int portraitSize) {
        super(name, hp, true, false, List.of(
                new Step(Intent.RITUAL, 2),
                new Step(Intent.ATTACK, attack)
        ));
        this.cyclePlan = List.of(
                new Step(Intent.ATTACK, attack)
        );
        this.portraitName = portraitName;
        this.portraitSize = portraitSize;
    }

    /** 基础邪教猪（前 5 层） */
    public static CultistPig base() {
        return new CultistPig("邪教猪", null, 45, 6, 210);
    }

    /** 老兵邪教猪（第 6 层起，复用邪教猪立绘但更小） */
    public static CultistPig veteran() {
        return new CultistPig("老兵邪教猪", "邪教猪", 75, 8, 170);
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
