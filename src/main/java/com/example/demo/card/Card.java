package com.example.demo.card;

/**
 * 一张卡牌（后续战斗系统会用到 damage/block/cost/draw）。
 * 以后再加新牌就照着打击/防御的样子加一个静态工厂方法。
 */
public class Card {

    /** 牌的类型 */
    public enum Type {
        ATTACK,  // 攻击牌
        SKILL,   // 技能牌
        POWER,   // 能力牌
        STATUS   // 状态牌（战斗中产生，战后从牌组移除）
    }

    /** 牌的种类 */
    public enum Kind {
        STRIKE("打击", "造成 6 点伤害", Type.ATTACK, 3),
        DEFEND("防御", "获得 5 点格挡", Type.SKILL, 3),
        BASH("痛击", "造成 8 点伤害，给予敌人 2 层易伤", Type.ATTACK, 3),
        SWEEP("铁斩波", "造成 5 点伤害，获得 5 点格挡", Type.ATTACK, 3),
        POMMEL("剑柄打击", "造成 9 点伤害，抽 1 张牌", Type.ATTACK, 2),
        SHRUG("耸肩无视", "获得 8 点格挡，抽 1 张牌", Type.SKILL, 2),
        BLEED("放血", "获得 2 点能量，自己失去 3 点生命", Type.SKILL, 2),
        HAMMER("重锤", "造成 32 点伤害", Type.ATTACK, 1),
        IMPREGNABLE("岿然不动", "获得 30 点格挡，消耗", Type.SKILL, 1),
        DOUBLE_STRIKE("双重打击", "造成 5 点伤害两次", Type.ATTACK, 3),
        KINDLE("燃烧", "获得 2 层力量", Type.POWER, 2),
        LIGHTNING("闪电霹雳", "对敌人造成 6 点伤害，给予 1 层易伤", Type.ATTACK, 3),
        RAGE("盛怒", "获得 2 点能量，消耗", Type.SKILL, 2),
        OFFERING("祭品", "自己失去 6 点生命，获得 2 点能量，抽 3 张牌，消耗", Type.SKILL, 1),
        FORTIFY("巩固", "将你当前的格挡翻倍", Type.SKILL, 2),
        FOCUS("战斗专注", "抽 3 张牌，本回合不能再抽牌", Type.SKILL, 2),
        SHOCKWAVE("震荡波", "给予敌人 4 层虚弱，4 层易伤，消耗", Type.SKILL, 2),
        HEAVY_BLADE("重刃", "造成 14 点伤害，力量在重刃上发挥 3 倍效果", Type.ATTACK, 3),
        WILD_STRIKE("狂野打击", "造成 12 点伤害，将一张“伤口”放入你的抽牌堆", Type.ATTACK, 3),
        ADAMANT_ARM("金刚臂", "造成 12 点伤害，给予 2 层虚弱", Type.ATTACK, 3),
        BRUTALITY("残暴", "使用后每回合开始时失去一点体力，多抽一张牌", Type.POWER, 1),
        FLEX("活动肌肉", "获得 2 点力量，回合结束时失去 2 点力量", Type.SKILL, 3),
        POWER_THROUGH("硬撑", "获得 15 点格挡，将两张伤口加入手牌", Type.SKILL, 2),
        SOUL_SEVER("断魂斩", "消耗手牌中所有的非攻击牌，造成 16 点伤害", Type.ATTACK, 2),
        BODY_SLAM("全身撞击","造成等同于你格挡值的伤害",Type.ATTACK,3),
        HEMOKINESIS("御血术","失去2点生命，造成15点伤害",Type.ATTACK,2),
        UPPERCUT("上勾拳","造成13点伤害，给予1层虚弱，给予1层易伤",Type.ATTACK,2),
        LIMIT_BREAK("突破极限","将你的力量翻倍，消耗",Type.SKILL,1),
        DEMON_FORM("恶魔形态","每回合增加两点力量",Type.POWER,1),
        TRUE_GRIT("坚毅","获得7点格挡，随机消耗一张手牌",Type.SKILL,3),
        FEEL_NO_PAIN("无惧疼痛","每有一张牌被消耗，获得 3 点格挡",Type.POWER,2),
        WOUND("伤口", "无法被打出", Type.STATUS, 3),
        SLIME("黏液", "消耗", Type.STATUS, 3);
        

        public final String label;
        public final String desc;
        public final Type type;
        public final int weight; // 品质：1=金卡，2=蓝卡，3=白卡（仅表示品质，不再是抽取权重）

        Kind(String label, String desc, Type type, int weight) {
            this.label = label;
            this.desc = desc;
            this.type = type;
            this.weight = weight;
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

     /** 能否被打出：约定 cost &lt; 0 表示不可打出（如“伤口”）。 */
    public boolean isPlayable() {
        return cost >= 0;
    }

    /**
     * 打出后是否离场（消耗）：显式消耗牌，或能力牌默认消耗。
     * <p>与字段 {@link #exhaust} 区分：能力牌默认消耗但卡面不显示“消耗”文字。
     */
    public boolean isExhaustOnPlay() {
        return exhaust || kind.type == Type.POWER;
    }

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
    public static Card fortify()     { return new Card(Kind.FORTIFY, 2, 0, 0); }
    public static Card focus()       { return new Card(Kind.FOCUS, 0, 0, 0); }
    public static Card shockwave()   { return new Card(Kind.SHOCKWAVE, 2, 0, 0, 0, true); }
    public static Card uppercut()    { return new Card(Kind.UPPERCUT,2,13,0);}
    public static Card heavyBlade()  { return new Card(Kind.HEAVY_BLADE, 2, 14, 0); } // 力量 3 倍效果在 play() 中处理
    public static Card wildStrike()  { return new Card(Kind.WILD_STRIKE, 1, 12, 0); } // 加伤口效果在 play() 中处理
    public static Card adamantArm()  { return new Card(Kind.ADAMANT_ARM, 2, 12, 0); } // 给虚弱效果在 play() 中处理
    public static Card brutality()   { return new Card(Kind.BRUTALITY, 0, 0, 0); } // 每回合效果在 play() 中处理
    public static Card flex()        { return new Card(Kind.FLEX, 0, 0, 0); } // 临时力量效果在 play() 中处理
    public static Card powerThrough(){ return new Card(Kind.POWER_THROUGH, 1, 0, 15); } // 往手牌塞两张伤口的效果在 play() 中处理
    public static Card soulSever()   { return new Card(Kind.SOUL_SEVER, 2, 16, 0); } // 消耗手牌中非攻击牌的效果在 play() 中处理
    public static Card limitBreak()  { return new Card(Kind.LIMIT_BREAK, 1, 0, 0,0,true); } // 将你的力量翻倍，消耗
    public static Card hemokinesis() { return new Card(Kind.HEMOKINESIS, 1, 15, 0); } // 造成15点伤害，失去2点生命值
    public static Card bodySlam()    { return new Card(Kind.BODY_SLAM, 1, 0, 0); } // 造成你当前格挡值的伤害
    public static Card demonForm()   { return new Card(Kind.DEMON_FORM, 3, 0, 0); } // 每回合增加两点力量
    public static Card feelNoPain()  { return new Card(Kind.FEEL_NO_PAIN, 1, 0, 0); } // 每有一张牌被消耗时获得格挡
    public static Card trueGrit()    { return new Card(Kind.TRUE_GRIT, 1, 0, 7); } // 获得7点格挡，随机消耗一张手牌
    public static Card wound()       { return new Card(Kind.WOUND, -1, 0, 0); } // -1 表示无法打出
    public static Card slime()       { return new Card(Kind.SLIME, 1, 0, 0, 0, true); } // 可打出，消耗
    
    

    /** 英雄宝典遗物：随机生成一张不消耗能量的能力牌 */
    public static Card freePower() {
        Kind[] powers = { Kind.KINDLE, Kind.BRUTALITY };
        Kind k = powers[new java.util.Random().nextInt(powers.length)];
        return new Card(k, 0, 0, 0, 0);
    }

    /**
     * 按种类造一张标准数值的牌（开发者模式面板的"加牌"列表用）。
     * 新增卡牌种类时，这里补一个 case 即可。
     */
    public static Card of(Kind kind) {
        return switch (kind) {
            case STRIKE -> strike();
            case DEFEND -> defend();
            case BASH -> bash();
            case SWEEP -> sweep();
            case POMMEL -> pommelStrike();
            case SHRUG -> shrug();
            case BLEED -> bleed();
            case HAMMER -> hammer();
            case IMPREGNABLE -> impregnable();
            case DOUBLE_STRIKE -> doubleStrike();
            case KINDLE -> kindle();
            case LIGHTNING -> lightning();
            case RAGE -> rage();
            case OFFERING -> offering();
            case FORTIFY -> fortify();
            case FOCUS -> focus();
            case SHOCKWAVE -> shockwave();
            case HEAVY_BLADE -> heavyBlade();
            case WILD_STRIKE -> wildStrike();
            case ADAMANT_ARM -> adamantArm();
            case BRUTALITY -> brutality();
            case FLEX -> flex();
            case POWER_THROUGH -> powerThrough();
            case SOUL_SEVER -> soulSever();
            case WOUND -> wound();
            case SLIME -> slime();
            case UPPERCUT -> uppercut();
            case BODY_SLAM -> bodySlam();
            case HEMOKINESIS -> hemokinesis();
            case LIMIT_BREAK -> limitBreak();
            case DEMON_FORM -> demonForm();
            case FEEL_NO_PAIN -> feelNoPain();
            case TRUE_GRIT -> trueGrit();
        };
    }
}
