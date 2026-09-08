package com.example.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗中的怪物。
 * 每个怪物有自己的“意图轮盘”（attack/defend/buff/weaken 循环），
 * 每回合怪物的行动 = 当前轮到的意图，执行完转到下一个。
 */
public class Enemy {

    /** 意图类型：攻击、防御、强化自身、弱化我方 */
    public enum Intent {
        ATTACK("攻击"),
        DEFEND("防御"),
        BUFF("强化"),
        WEAKEN("虚弱");

        public final String label;
        Intent(String label) { this.label = label; }
    }

    /** 轮盘上的一步：做什么 + 数值 */
    public static class Step {
        public final Intent intent;
        public final int value;

        Step(Intent intent, int value) {
            this.intent = intent;
            this.value = value;
        }
    }

    public final String name;
    public final int maxHp;
    public int hp;
    public int block; // 当前格挡值
    public int power; // 力量：加到攻击伤害上（强化获得）

    private final List<Step> plan = new ArrayList<>();
    private int planIndex = 0;

    private Enemy(String name, int maxHp, List<Step> plan) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.plan.addAll(plan);
    }

    /** 当前要执行的意图（显示给玩家看） */
    public Step current() {
        return plan.get(planIndex);
    }

    /** 本次行动执行完后，转到下一个意图 */
    public void advance() {
        planIndex = (planIndex + 1) % plan.size();
    }

    /** 描述当前意图的文字，例如：攻击 9 / 强化 力量+2 / 虚弱 我方2回合 */
    public String intentText() {
        Step s = current();
        int v = s.value;
        return switch (s.intent) {
            case ATTACK -> s.intent.label + " " + (v + power);
            case DEFEND -> s.intent.label + " " + v;
            case BUFF   -> s.intent.label + " 力量 +" + v;
            case WEAKEN -> s.intent.label + " 我方 " + v + " 回合";
        };
    }

    // ================= 各档位怪物的工厂方法 =================

    /** 普通怪：攻击 7 / 防御 5 / 攻击 7 / 强化+2 循环 */
    public static Enemy slime() {
        return new Enemy("史莱姆", 28, List.of(
                new Step(Intent.ATTACK, 8),
                new Step(Intent.DEFEND, 5),
                new Step(Intent.ATTACK, 7),
                new Step(Intent.BUFF, 2)
        ));
    }

    /** 精英怪：血量更高、行动更凶（含虚弱我方） */
    public static Enemy eliteSlime() {
        return new Enemy("精英史莱姆", 45, List.of(
                new Step(Intent.BUFF, 2),
                new Step(Intent.ATTACK, 10),
                new Step(Intent.DEFEND, 8),
                new Step(Intent.WEAKEN, 2),
                new Step(Intent.ATTACK, 12)
        ));
    }

    /** BOSS */
    public static Enemy boss() {
        return new Enemy("巨塔守卫", 70, List.of(
                new Step(Intent.BUFF, 3),
                new Step(Intent.ATTACK, 13),
                new Step(Intent.WEAKEN, 3),
                new Step(Intent.DEFEND, 10),
                new Step(Intent.ATTACK, 16)
        ));
    }
}
