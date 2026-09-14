package com.example.demo.enemy;

import java.util.List;

/**
 * 精英怪：混沌猪（立绘 {@code portrait/混沌猪.png}）。
 *
 * <p>一只行为随机的精英 —— 每次「随机增益」都从五种里等概率抽一种。</p>
 *
 * <h3>行动轮盘</h3>
 * <pre>
 *   第 1 回合  攻击 8
 *   第 2 回合  随机增益（力量 6 / 仪式 6 / 敏捷 6 / 闪避 / 反伤 6）
 *   第 3 回合  攻击 10
 *   ───────── 之后无限循环下面三步 ─────────
 *   加 10 格挡 → 随机增益 → 攻击 10
 * </pre>
 *
 * <p>攻击是<b>基础值 + 自身力量</b>（力量会加上去）：所以第 3 回合是 10，
 * 等它叠了几次「力量 / 仪式」之后，同一招就会打出 16、22……</p>
 *
 * <h3>五种随机增益</h3>
 * <ul>
 *   <li>力量 6 —— {@code power += 6}（每段攻击 +6）</li>
 *   <li>仪式 6 —— 每回合开始自动 +6 力量</li>
 *   <li>敏捷 6 —— 之后每次获得格挡都 +6（「加 10 格挡」变成 16）</li>
 *   <li>闪避 —— 获得闪避能力（每次被攻击有 {@value #DODGE_CHANCE_UI}% 概率完全闪开）</li>
 *   <li>反伤 6 —— {@value #REFLECT_TURNS} 回合内反伤 {@value #REFLECT_RATE_UI}%（沿用卫士猪那套）</li>
 * </ul>
 *
 * <h3>随机全部走战斗种子</h3>
 * 增益抽签和闪避判定都从 {@link #rng(int, int)} 派生（种子 = 地图种子 + 节点行列），
 * 所以「读档重打这一战」拿到的增益顺序、每次闪避的成败都和第一次一模一样 —— 刷不了。
 */
public class ChaosPig extends Enemy {

    /** 随机增益的种类 */
    public enum ChaosBuff {
        POWER("力量"),
        RITUAL("仪式"),
        DEXTERITY("敏捷"),
        DODGE("闪避"),
        REFLECT("反伤");

        public final String label;

        ChaosBuff(String label) {
            this.label = label;
        }
    }

    /** 增益数值：力量 / 仪式 / 敏捷都是 6 */
    private static final int BUFF_VALUE = 6;

    /** 反伤持续回合数 */
    private static final int REFLECT_TURNS = 6;

    /** 反伤比例（0.3 = 30%，和卫士猪一致） */
    private static final double REFLECT_RATE = 0.3;

    /** 反伤比例（百分比整数，只用于文案） */
    private static final int REFLECT_RATE_UI = 30;

    /** 闪避概率 */
    private static final double DODGE_CHANCE = 0.4;

    /** 闪避概率（百分比整数，只用于文案） */
    private static final int DODGE_CHANCE_UI = 40;

    /** 随机流编号：增益抽签 / 闪避判定各一条，互不干扰 */
    private static final int STREAM_BUFF = 41;
    private static final int STREAM_DODGE = 42;

    /** 轮盘前 3 步只播一遍，之后从下标 {@value #LOOP_START}（「加 10 格挡」）开始无限循环 */
    private static final int LOOP_START = 3;

    private static final List<Step> WHEEL = List.of(
            new Step(Intent.ATTACK, 8),          // 第 1 回合
            new Step(Intent.BUFF, BUFF_VALUE),   // 第 2 回合：随机增益
            new Step(Intent.ATTACK, 10),         // 第 3 回合
            new Step(Intent.DEFEND, 10),         // ┐
            new Step(Intent.BUFF, BUFF_VALUE),   // ├ 循环段
            new Step(Intent.ATTACK, 10)          // ┘
    );

    /** 当前轮盘下标（不走基类那套，因为循环起点不是 0） */
    private int idx = 0;

    /** 已经走过的步数（从 0 起） */
    private int steps = 0;

    /** 掷过几次增益（决定用种子里第几个随机数） */
    private int buffRolls = 0;

    /** 判定过几次闪避 */
    private int dodgeRolls = 0;

    /**
     * 这一次 BUFF 步要施加的增益。
     *
     * <p>在 {@link #advance()} 落到 BUFF 步时就先掷好 —— 这样玩家在意图栏里
     * 就能看到它下回合要加什么，而不是打完了才知道。</p>
     */
    private ChaosBuff pendingBuff = null;

    /** 是否已经拿到过「闪避」增益（拿到就永久拥有，和卫士猪的反伤不同） */
    private boolean dodgeGranted = false;

    public ChaosPig() {
        super("混沌猪", 88, true, false, true, WHEEL);
        this.reflectRate = REFLECT_RATE;
        this.isElite = true;
    }

    // ================= 轮盘 =================

    /** 轮盘不走基类（基类固定在 0 处循环，混沌猪要从第 4 步起循环） */
    @Override
    public Step current() {
        return WHEEL.get(idx);
    }

    @Override
    public void advance() {
        idx++;
        if (idx >= WHEEL.size()) {
            idx = LOOP_START; // 前 3 回合只播一遍，之后在最后 3 步里循环
        }
        steps++;
        if (WHEEL.get(idx).intent == Intent.BUFF) {
            pendingBuff = rollBuff(); // 先掷好，意图栏就能显示
        }
    }

    /** 等概率抽一种增益（走战斗种子的第 {@code buffRolls} 个随机数） */
    private ChaosBuff rollBuff() {
        ChaosBuff[] all = ChaosBuff.values();
        return all[rng(STREAM_BUFF, buffRolls++).nextInt(all.length)];
    }

    // ================= 增益结算 =================

    @Override
    public void applyBuff(Step s) {
        // 正常流程下 pendingBuff 已经在 advance 里掷好了；兜底再掷一次（比如直接调 applyBuff 自测）
        ChaosBuff buff = pendingBuff != null ? pendingBuff : rollBuff();
        pendingBuff = null;

        switch (buff) {
            case POWER -> power += BUFF_VALUE;
            case RITUAL -> setRitualPower(BUFF_VALUE);
            case DEXTERITY -> gainDexterity(BUFF_VALUE);
            case DODGE -> dodgeGranted = true;
            case REFLECT -> grantReflectTurns(REFLECT_TURNS); // 真正的 reflectTurns 由 BattleView 取走
        }
    }

    @Override
    public String buffIntentTip() {
        if (pendingBuff == null) {
            return "意图·强化：混沌猪将随机获得一种增益";
        }
        String extra = switch (pendingBuff) {
            case DODGE -> "（获得闪避）";
            case REFLECT -> "（" + REFLECT_TURNS + " 回合，" + REFLECT_RATE_UI + "% 反伤）";
            default -> " +" + BUFF_VALUE;
        };
        return "意图·强化：混沌猪将获得「" + pendingBuff.label + "」" + extra;
    }

    // ================= 闪避 =================

    @Override
    public boolean hasDodge() {
        return dodgeGranted;
    }

    @Override
    public boolean dodge() {
        if (!dodgeGranted) return false;
        // 走种子：读档重打这一战，第几次闪避成不成还是同一个结果
        return rng(STREAM_DODGE, dodgeRolls++).nextDouble() < DODGE_CHANCE;
    }

    @Override
    public int dodgeChanceUi() {
        return DODGE_CHANCE_UI;
    }

    // ================= 给探针/调试看的状态 =================

    /** 已经走过的步数（从 0 起） */
    public int steps() {
        return steps;
    }

    /** 已经掷过几次增益 */
    public int buffRolls() {
        return buffRolls;
    }

    /** 这一步（BUFF 步）将要施加的增益；不在 BUFF 步时为 null */
    public ChaosBuff pendingBuff() {
        return pendingBuff;
    }

    /** 是否已经拿到「闪避」增益 */
    public boolean dodgeGranted() {
        return dodgeGranted;
    }

    /** 闪避概率（百分比整数，只用于文案/自测） */
    public static int dodgeChancePercent() {
        return DODGE_CHANCE_UI;
    }
}
