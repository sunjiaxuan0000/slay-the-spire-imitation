package com.example.demo.event;

import com.example.demo.character.Player;
import com.example.demo.character.Relic;
import com.example.demo.character.RelicFun;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 地图事件：名字 + 描述 + 若干选项（选项名 + 选项实际效果说明 + 效果参数）。
 * 效果实际结算在 HelloApplication（需要访问 Player），这里只存“要做什么”。
 */
public class EventDef {

    /** 选项实际会做的事 */
    public enum Action {
        HEAL,          // 回血（amount = 回多少）
        DAMAGE,        // 扣血（amount = 扣多少）
        ADD_CARD,      // 往牌组加一张随机牌
        ADD_RELIC,     // 获得一件未持有的随机遗物
        /**
         * 获得<b>指定名字</b>的遗物（{@link Option#relicName}）。
         * 专门给「猪雪峰」这种「三选一固定三件专属遗物」的事件用 ——
         * 那三件不在任何抽取池里，只能这样点名发放。
         */
        ADD_NAMED_RELIC,
        NOTHING,// 无事发生
        RANDOM_WATER,//随机掉血或回血
        CURSE//接受诅咒：扣 10 点生命 + 往牌组加一张"伤口"（诅咒祭坛专用）
    }

    /** 一个选项 */
    public static class Option {
        public final String label;      // 选项名
        public final String effectDesc; // “选项实际效果”文字（显示在选项里）
        public final Action action;     // 真实效果种类
        public final int amount;        // 效果数值（HEAL/DAMAGE 用）
        /** 只在 {@link Action#ADD_NAMED_RELIC} 时有效：要点名发放的遗物名字 */
        public final String relicName;

        public Option(String label, String effectDesc, Action action, int amount) {
            this(label, effectDesc, action, amount, null);
        }

        public Option(String label, String effectDesc, Action action, int amount, String relicName) {
            this.label = label;
            this.effectDesc = effectDesc;
            this.action = action;
            this.amount = amount;
            this.relicName = relicName;
        }
    }

    public final String name;
    public final String desc;
    public final String imageName;
    public final List<Option> options;
    /**
     * 事件专属背景图的文件名（相对 {@code /com/example/demo/}）。
     * 为 null 时 {@link EventView} 退回通用的 {@code event_bg.png}。
     */
    public final String bgName;


    public EventDef(String name, String desc,String imageName ,List<Option> options,String bgName) {
        this.name = name;
        this.desc = desc;
        this.imageName = imageName;
        this.options = options;
        this.bgName = bgName;
    }

    // ================= 事件池（进事件节点时随机挑一个） =================

    /**
     * 常规事件池 —— {@link #pick(Player)} 默认在里面挑。
     *
     * <p>⚠ 猪雪峰<b>不在</b>这里：它一局只出一次、概率还低，
     * 由 {@link #pick(Player)} 单独掷一次骰子决定出不出。</p>
     */
    private static final List<EventDef> NORMAL_POOL = List.of(
                new EventDef("岔路口的雕像",
                        "一尊古老的石像立在路中间，底座刻着几行模糊的字："
                                + "“向它献上你的血，它将予你回应。”雕像的眼窝似乎亮了一下。",
                        "Event-Goldenldol.png",
                        List.of(
                                new Option("以血相献", "失去 8 点生命", Action.DAMAGE, 8),
                                new Option("轻叩石像", "无事发生，雕像纹丝不动", Action.NOTHING, 0),
                                new Option("转身离开", "没有冒险的必要", Action.NOTHING, 0)
                        )),

                new EventDef("废弃的营地",
                        "地上散落着焦黑的木柴与一顶破帐篷。有人曾在这里扎营，"
                                + "营火早已熄灭，但木头深处似乎还留着一点余温。",
                        "Event-BonfireSpirits.png",
                        List.of(
                                new Option("拨弄余烬", "回复 15 点生命", Action.HEAL, 15),
                                new Option("翻找遗物", "获得一件未持有的随机遗物", Action.ADD_RELIC, 0),
                                new Option("不作停留", "继续赶路", Action.NOTHING, 0)
                        )),

                new EventDef("迷雾中的书摊",
                        "一阵风吹过，路旁不知何时多了一个书摊。摊主蒙着面，"
                                + "声音沙哑：“挑一样吧，凡人——是磨刀石，还是暖炉？”",
                        "Event-Cleric.png",
                        List.of(
                                new Option("要一张卡牌", "随机一张卡牌加入牌组", Action.ADD_CARD, 0),
                                new Option("喝口热茶", "回复 10 点生命", Action.HEAL, 10),
                                new Option("无视摊主", "径直走开", Action.NOTHING, 0)
                        )),
                new EventDef(
                        "神秘泉水",
                        "一汪清澈的泉水出现在你面前。\n"
                        +"水面平静得有些诡异，你无法判断它是否安全。",
                        "Event-DivineFountain.png",
                        List.of(
                                new Option("饮用泉水",
                                "50% 概率回复 15 点生命，50% 概率失去 10 点生命",
                                Action.RANDOM_WATER,
                                0
                        ),
                                new Option(
                                "离开",
                                "什么也没发生",
                                Action.NOTHING,
                                0
                        ))
                        ),
                new EventDef(
                       "神秘书籍" ,
                        "一本落满灰尘的古老书籍静静地躺在石台上。\n"
                        +"书页没有文字，只有一道道看不懂的符号。\n"
                                + "当你靠近时，书页竟自行翻动起来……",
                        "Event-CursedTome.png",
                        List.of(
                                new Option("阅读书籍",
                                "获得一张随机卡牌",
                                Action.ADD_CARD,
                                0
                        ),
                                new Option(
                                "离开",
                                "什么也没发生",
                                Action.NOTHING,
                                0
                        )
                        )
                ),
                new EventDef(
                        "诅咒祭坛",
                        "一座漆黑的祭坛矗立在道路中央。\n"
                                + "祭坛周围没有任何声音，只有中央的一枚黑色符文散发着微弱的光。\n"
                                + "你能感觉到其中蕴含着某种力量，但这股力量似乎并不免费。",
                        "Event-ForgottenAltar.png",
                        List.of(
                                new Option("接受诅咒",
                                        "失去 10 点生命，获得一张“伤口”",
                                        Action.CURSE,
                                        10
                                ),
                                new Option(
                                        "离开",
                                        "什么也不发生",
                                        Action.NOTHING,
                                        0
                                )
                        )
                )


        );

    // ================= 猪雪峰（一局只出一次的低概率专属事件） =================

    /** 猪雪峰的出现概率（每次进事件节点掷一次；已经出过就直接跳过） */
    private static final double XUEFENG_CHANCE = 0.15;

    /**
     * 猪雪峰：三选一，拿走「猪爆气 / 猪冰棍 / 猪疾速」中的一件。
     *
     * <p>这三件遗物<b>只能通过本事件获得</b>（见 {@code Relic.XUEFENG_RELICS}），
     * 所以这里用 {@code ADD_NAMED_RELIC} 点名发放，而不是走随机池。</p>
     */
    public static final EventDef XUEFENG = new EventDef(
            "猪雪峰",
            "你在猪塔中探索，发现了一个奇特的雕像。\n"
                    + "像是一头天使猪站在雪峰上的雕像。\n"
                    + "你从中吸取到了一些力量",
            List.of(
                    new Option("拿起猪爆气",
                            "战斗开始时，对自身造成不可格挡的 2 点伤害，对怪物造成 13 点伤害",
                            Action.ADD_NAMED_RELIC, 0, "猪爆气"),
                    new Option("拿起猪冰棍",
                            "战斗的前两回合开始时额外获得 1 点能量",
                            Action.ADD_NAMED_RELIC, 0, "猪冰棍"),
                    new Option("拿起猪疾速",
                            "战斗开始时获得 1 点敏捷；篝火的「休息」改为「练起来」，不回血，"
                                    + "改为使本遗物提供的敏捷 +1",
                            Action.ADD_NAMED_RELIC, 0, "猪疾速")
            ),
            "encounter/xuefeng.png");   // 专属背景

    /**
     * 进事件节点时挑一个事件。
     *
     * <p>猪雪峰是「一局一次 + 低概率」：先掷一次 {@link #XUEFENG_CHANCE} 的骰子，
     * 中了就出它并把 {@link Player#xuefengSeen} 置位（本局不再出）；
     * 没中就在常规池里均匀挑。</p>
     *
     * <p>已经出过的判据是「{@code xuefengSeen} 或已经持有三件之一」——
     * 双保险：万一存档没记下 xuefengSeen（旧档），手上已经有遗物也足以说明来过了。</p>
     *
     * @param player 当前玩家（用于判断这局出没出过猪雪峰）；传 null 时只挑常规池
     */
    public static EventDef pick(Player player) {
        if (!xuefengDone(player) && Math.random() < XUEFENG_CHANCE) {
            player.xuefengSeen = true;
            return XUEFENG;
        }
        return NORMAL_POOL.get((int) (Math.random() * NORMAL_POOL.size()));
    }

    private static boolean xuefengDone(Player player) {
        return player != null && (player.xuefengSeen || RelicFun.hasXuefengRelic(player));
    }

    /**
     * 全部事件（常规池 + 猪雪峰）。
     *
     * <p>存档按名字找回事件时要用它（{@code HelloApplication.findEvent}），
     * 所以猪雪峰必须在里面；但别拿它去随机抽，那会让它跟常规事件等概率。</p>
     */
    public static List<EventDef> pool() {
        List<EventDef> all = new ArrayList<>(NORMAL_POOL);
        all.add(XUEFENG);
        return all;
    }

    /** 按名字在点名发放的遗物里找（猪雪峰的三件）；找不到返回 null。 */
    public static Relic relicNamed(String name) {
        return Relic.findByName(name);
    }
}
