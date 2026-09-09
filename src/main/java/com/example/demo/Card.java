package com.example.demo;

/**
 * 一张卡牌（后续战斗系统会用到 damage/block/cost/draw）。
 * 以后再加新牌就照着打击/防御的样子加一个静态工厂方法。
 */
public class Card {

    /** 牌的种类 */
    public enum Kind {
        STRIKE("打击", "造成 6 点伤害"),
        DEFEND("防御", "获得 5 点格挡"),
        BASH("痛击", "造成 8 点伤害，给予敌人 2 层易伤"),
        SWEEP("铁斩波", "造成 5 点伤害，获得 5 点格挡"),
        POMMEL("剑柄打击", "造成 9 点伤害，抽 1 张牌"),
        SHRUG("耸肩无视", "获得 8 点格挡，抽 1 张牌"),
        BLEED("放血", "获得 2 点能量，自己失去 3 点生命"),
        HAMMER("重锤", "造成 32 点伤害"),
        IMPREGNABLE("岿然不动", "获得 30 点格挡"),
        DOUBLE_STRIKE("双重打击", "造成 5 点伤害两次"),
        KINDLE("燃烧", "获得 2 层力量"),
        LIGHTNING("闪电霹雳", "对敌人造成 6 点伤害，给予 1 层易伤"),
        RAGE("盛怒", "获得 2 点能量，消耗"),
        OFFERING("祭品", "自己失去 6 点生命，获得 2 点能量，抽 3 张牌，消耗");


        public final String label;
        public final String desc;

        Kind(String label, String desc) {
            this.label = label;
            this.desc = desc;
        }
    }

    public final int id;      // 每张牌的全局唯一编号（按创建顺序递增），用于"按 id 排序显示"
    public final Kind kind;
    public final int cost;    // 打出消耗的能量
    public final int damage;  // 攻击数值（防御牌为 0）
    public final int block;   // 格挡数值（攻击牌为 0）
    public final int draw;    // 打出后额外抽牌数（大多数牌为 0）
    public final boolean exhaust; // 消耗：打出后不进入弃牌堆，每场战斗只能用一次
    public final int hits;    // 攻击段数（默认 1，双重打击为 2）

    private static int nextId = 0;

    private Card(Kind kind, int cost, int damage, int block, int draw, boolean exhaust, int hits) {
        this.id = ++nextId;
        this.kind = kind;
        this.cost = cost;
        this.damage = damage;
        this.block = block;
        this.draw = draw;
        this.exhaust = exhaust;
        this.hits = hits;
    }

    private Card(Kind kind, int cost, int damage, int block, int draw, boolean exhaust) {
        this(kind, cost, damage, block, draw, exhaust, 1);
    }

    private Card(Kind kind, int cost, int damage, int block, int draw, int hits) {
        this(kind, cost, damage, block, draw, false, hits);
    }

    private Card(Kind kind, int cost, int damage, int block, int draw) {
        this(kind, cost, damage, block, draw, false, 1);
    }

    private Card(Kind kind, int cost, int damage, int block) {
        this(kind, cost, damage, block, 0, false, 1);
    }

    public static Card strike()      { return new Card(Kind.STRIKE, 1, 6, 0); }
    public static Card defend()      { return new Card(Kind.DEFEND, 1, 0, 5); }
    public static Card bash()        { return new Card(Kind.BASH, 2, 8, 0); }
    public static Card sweep()       { return new Card(Kind.SWEEP, 1, 5, 5); }
    public static Card pommelStrike(){ return new Card(Kind.POMMEL, 1, 9, 0, 1); }
    public static Card shrug()       { return new Card(Kind.SHRUG, 1, 0, 8, 1); }
    public static Card bleed()       { return new Card(Kind.BLEED, 0, 0, 0); }
    public static Card hammer()      { return new Card(Kind.HAMMER, 3, 32, 0); }
    public static Card impregnable() { return new Card(Kind.IMPREGNABLE, 2, 0, 30, 0, true); }
    public static Card doubleStrike(){ return new Card(Kind.DOUBLE_STRIKE, 1, 5, 0, 0, 2); }
    public static Card kindle()      { return new Card(Kind.KINDLE, 1, 0, 0); }
    public static Card lightning()   { return new Card(Kind.LIGHTNING, 1, 6, 0); }
    public static Card rage()        { return new Card(Kind.RAGE, 1, 0, 0, 0, true); }
    public static Card offering()    { return new Card(Kind.OFFERING, 0, 0, 0, 3, true); }
}
