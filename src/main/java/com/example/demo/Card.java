package com.example.demo;

/**
 * 一张卡牌（后续战斗系统会用到 damage/block/cost）。
 * 以后再加新牌就照着重斩/铁壁的样子加一个静态工厂方法。
 */
public class Card {

    /** 牌的种类 */
    public enum Kind {
        STRIKE("打击", "造成 6 点伤害"),
        DEFEND("防御", "获得 5 点格挡"),
        BASH("痛击", "造成 8 点伤害"),
        HEAVY("重斩", "造成 12 点伤害"),
        IRON("铁壁", "获得 10 点格挡");

        public final String label;
        public final String desc;

        Kind(String label, String desc) {
            this.label = label;
            this.desc = desc;
        }
    }

    public final int id;      // 每张牌的全局唯一编号（按创建顺序递增），用于“按 id 排序显示”
    public final Kind kind;
    public final int cost;    // 打出消耗的能量
    public final int damage;  // 攻击数值（防御牌为 0）
    public final int block;   // 格挡数值（攻击牌为 0）

    private static int nextId = 0;

    private Card(Kind kind, int cost, int damage, int block) {
        this.id = ++nextId;
        this.kind = kind;
        this.cost = cost;
        this.damage = damage;
        this.block = block;
    }

    public static Card strike()  { return new Card(Kind.STRIKE, 1, 6, 0); }
    public static Card defend()  { return new Card(Kind.DEFEND, 1, 0, 5); }
    public static Card bash()    { return new Card(Kind.BASH, 2, 8, 0); }
    public static Card heavyHit(){ return new Card(Kind.HEAVY, 2, 12, 0); }
    public static Card ironWall(){ return new Card(Kind.IRON, 2, 0, 10); }
}
