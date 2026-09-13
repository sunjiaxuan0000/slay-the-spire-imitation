package com.example.demo.enemy;

import com.example.demo.view.GameMap;

/**
 * 普通怪的生成规则集中在此，供战斗入口调用。
 *
 * 前 5 层（row 0~4）：只有基础池 —— 史莱姆 60% / 邪教猪 40%。
 *
 * 第 6 层起（row >= 5）分为两个池子：
 *   基础池：史莱姆 / 邪教猪（池内 6:4），合计生成概率随层数从 40% 线性递减到 20%；
 *   高级池：大史莱姆 / 老兵邪教猪 / 神风猪（池内 4:3:3），
 *           合计生成概率随层数从 60% 线性增大到 80%。
 */
public final class EnemyFactory {

    /** 第 6 层起开放高级池（row 从 0 开始，>=5 即第 6 层） */
    private static final int UPGRADE_ROW = 5;

    /** 基础池合计概率：第 6 层为 40%（{@link #BASE_RATE_HIGH}），逐层线性降到 20%（{@link #BASE_RATE_LOW}） */
    private static final double BASE_RATE_HIGH = 0.4;
    private static final double BASE_RATE_LOW = 0.2;

    /** 基础池内部：史莱姆 60% / 邪教猪 40% */
    private static final double BASE_SLIME_RATIO = 0.6;

    // ===== 高级池内部权重：大史莱姆 4 : 老兵邪教猪 3 : 神风猪 3 =====
    private static final double BIG_SLIME_WEIGHT = 0.4;
    private static final double VETERAN_CULTIST_WEIGHT = 0.3;
    // 剩余 0.3 为神风猪

    private EnemyFactory() {
    }

    /** 生成某层的普通怪 */
    public static Enemy normal(int row) {
        double roll = Math.random();

        if (row < UPGRADE_ROW) {
            // 前 5 层：只有基础池
            return roll < BASE_SLIME_RATIO ? Slime.base() : CultistPig.base();
        }

        // 基础池概率：row 5 → 40%，逐层线性递减，末层（row = ROWS-1）→ 20%
        double progress = (double) (row - UPGRADE_ROW) / (GameMap.ROWS - 1 - UPGRADE_ROW);
        double t = Math.max(0, Math.min(1, progress));
        double baseRate = BASE_RATE_HIGH - (BASE_RATE_HIGH - BASE_RATE_LOW) * t;
        double advancedRate = 1.0 - baseRate; // 高级池：60% → 80%

        if (roll >= advancedRate) {
            // 落入基础池：池内 6:4
            double inPool = Math.random();
            return inPool < BASE_SLIME_RATIO ? Slime.base() : CultistPig.base();
        }

        // 落入高级池：大史莱姆 4 / 老兵邪教猪 3 / 神风猪 3
        double inPool = Math.random();
        if (inPool < BIG_SLIME_WEIGHT) {
            return Slime.big();
        }
        if (inPool < BIG_SLIME_WEIGHT + VETERAN_CULTIST_WEIGHT) {
            return CultistPig.veteran();
        }
        return new BoomPig();
    }

    /** BOSS 房：猪龙鱼公爵 / 巨猪骑士各 50%（没有指定种类时的兜底） */
    public static Enemy boss() {
        return Math.random() < 0.5 ? new DukePorcodraco() : new GiantBoarKnight();
    }

    /**
     * 按地图定好的种类出 BOSS。
     *
     * <p>种类在 {@link GameMap#generate(long)} 时就决定了，地图上 BOSS 节点的图标也按它画，
     * 所以进战斗必须用同一个 —— 否则会「地图上画着鱼、进去打的是猪」。
     * 传 {@code null} 时退回随机（和老的 {@link #boss()} 一样）。</p>
     */
    public static Enemy boss(GameMap.BossKind kind) {
        if (kind == GameMap.BossKind.BOAR) return new GiantBoarKnight();
        if (kind == GameMap.BossKind.DUKE) return new DukePorcodraco();
        return boss();
    }

    /**
     * 按名字重建一只敌人（<b>读档用</b>）。
     *
     * <p>存档会把敌人的名字一起记下来，就是为了读档时还能遇到<b>同一只</b>怪 ——
     * 不然玩家打不过神风猪就退出去重进，刷到史莱姆为止，存档就成了刷怪器。</p>
     *
     * <p>名字为空或认不出来（比如旧存档、手改过）时退回随机：
     * {@code boss=true} 走 BOSS 池，否则按层数走普通怪池 —— 也就是「新进这个节点」的结果。</p>
     */
    public static Enemy named(String name, int row, boolean boss) {
        if (name != null && !name.isEmpty()) {
            switch (name) {
                case "史莱姆"     -> { return Slime.base(); }
                case "大史莱姆"   -> { return Slime.big(); }
                case "邪教猪"     -> { return CultistPig.base(); }
                case "老兵邪教猪" -> { return CultistPig.veteran(); }
                case "神风猪"     -> { return new BoomPig(); }
                case "卫士猪"     -> { return new GuardPig(); }
                case "猪龙鱼公爵" -> { return new DukePorcodraco(); }
                case "巨猪骑士"   -> { return new GiantBoarKnight(); }
                default -> { }
            }
        }
        return boss ? boss() : normal(row);
    }
}
