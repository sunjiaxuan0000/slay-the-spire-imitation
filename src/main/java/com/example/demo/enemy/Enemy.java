package com.example.demo.enemy;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗中怪物的抽象基类。
 * 每个怪物有自己的"意图轮盘"（attack/defend/buff/weaken 循环），
 * 每回合怪物的行动 = 当前轮到的意图，执行完转到下一个。
 *
 * 子类：{@link Slime}（普通怪）、{@link GuardPig}（精英怪）、{@link DukePorcodraco}（BOSS）。
 */
public abstract class Enemy {

    /** 意图类型：攻击、防御、强化自身、弱化我方 */
    public enum Intent {
        ATTACK("攻击"),
        DEFEND("防御"),
        BUFF("强化"),
        WEAKEN("虚弱"),
        REFLECT("反伤"),
        RITUAL("仪式");

        public final String label;
        Intent(String label) { this.label = label; }
    }

    /** 轮盘上的一步：做什么 + 数值 */
    public static class Step {
        public final Intent intent;
        public final int value;

        public Step(Intent intent, int value) {
            this.intent = intent;
            this.value = value;
        }
    }

    // ===== 共享状态 =====
    public final String name;
    public final int maxHp;
    public int hp;
    public int block;       // 当前格挡值
    public int power;// 力量：加到攻击伤害上（强化获得）
    public final boolean isBoss;
    public boolean isSecondPhase = false;  // BOSS 二阶段标记（仅 Boss 子类会触发）
    public final boolean hasPortrait;

    /** 反伤比例（0~1）：玩家对其造成伤害时反弹该比例的伤害，0 表示无反伤。子类在构造中设置。 */
    protected double reflectRate = 0;

    /** 仪式：每回合开始自动增加的力量值，0 表示无仪式。由 RITUAL 意图激活。 */
    protected int ritualPower = 0;

    /** 攻击后向玩家抽牌堆塞入的黏液牌数量，0 表示无此效果（史莱姆特有）。 */
    protected int slimeOnAttack = 0;

    /** 立绘文件名（不含扩展名），null 表示用 name 作为文件名。 */
    protected String portraitName = null;

    /** 立绘贴图尺寸（正方形边长），默认 210。 */
    protected int portraitSize = 210;

    // ===== 意图轮盘 =====
    private final List<Step> plan;
    private int planIndex = 0;

    protected Enemy(String name, int maxHp, boolean hasPortrait, boolean isBoss, List<Step> plan) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.hasPortrait = hasPortrait;
        this.isBoss = isBoss;
        this.plan = new ArrayList<>(plan);
    }

    /** 子类可覆写：返回当前生效的意图轮盘（BOSS 二阶段切轮盘） */
    protected List<Step> getActivePlan() {
        return plan;
    }

    /** 当前要执行的意图（显示给玩家看） */
    public Step current() {
        return getActivePlan().get(planIndex);
    }

    /** 本次行动执行完后，转到下一个意图 */
    public void advance() {
        planIndex = (planIndex + 1) % getActivePlan().size();
    }

    /** 重置轮盘指针到起始位置（BOSS 切阶段时调用） */
    protected void resetPlanIndex() {
        planIndex = 0;
    }

    /** 二阶段检测，默认空实现，BOSS 覆写 */
    public void checkPhaseTransition() { }

    /** 反伤比例（0~1），0 表示无反伤 */
    public double getReflectRate() {
        return reflectRate;
    }

    /** 仪式每回合增加的力量值 */
    public int getRitualPower() {
        return ritualPower;
    }

    /** 攻击后塞入玩家抽牌堆的黏液牌数量 */
    public int getSlimeOnAttack() {
        return slimeOnAttack;
    }

    /** 立绘文件名（BigSlime 可复用史莱姆的图） */
    public String getPortraitName() {
        return portraitName != null ? portraitName : name;
    }

    /** 立绘贴图尺寸 */
    public int getPortraitSize() {
        return portraitSize;
    }

    /** 设置仪式效果（由 RITUAL 意图触发） */
    public void setRitualPower(int value) {
        this.ritualPower = value;
    }

    /** 每回合开始时调用：仪式生效则自动增加力量 */
    public void applyRitual() {
        if (ritualPower > 0) {
            power += ritualPower;
        }
    }

    /** 描述当前意图的文字 */
    public String intentText() {
        Step s = current();
        int v = s.value;
        return switch (s.intent) {
            case ATTACK -> s.intent.label + " " + (v + power);
            case DEFEND -> s.intent.label + " " + v;
            case BUFF   -> s.intent.label + " 力量 +" + v;
            case WEAKEN -> s.intent.label + " 我方 " + v + " 回合";
            case REFLECT -> s.intent.label +"我方" + v + "回合";
            case RITUAL -> s.intent.label + " 每回合力量 +" + v;
        };
    }

}
