package com.example.demo.event;

import com.example.demo.character.Player;
import com.example.demo.character.Relic;
import com.example.demo.character.RelicFun;

import java.util.ArrayList;
import java.util.List;

/**
 * 地图事件：
 * 事件名字 + 描述 + 若干选项。
 *
 * 一个 Option 可以包含多个 Effect。
 *
 * 例如：
 * 失去 15 点生命 + 获得一张随机卡牌
 *
 * 就可以表示为：
 *
 * List.of(
 *     new Effect(Action.DAMAGE, 15),
 *     new Effect(Action.ADD_CARD, 0)
 * )
 *
 * 具体效果结算仍然放在 HelloApplication 中，
 * EventDef 这里只负责描述“要做什么”。
 */
public class EventDef {

    // =========================================================
    // 1. Action：描述一个最基本的行为
    // =========================================================

    public enum Action {

        HEAL,               // 回血，amount = 回多少

        DAMAGE,             // 扣血，amount = 扣多少

        ADD_CARD,           // 加入一张随机卡牌

        ADD_RELIC,          // 获得一件未持有的随机遗物

        ADD_NAMED_RELIC,    // 获得指定名字的遗物，使用 relicName

        ADD_WOUND,          // 获得一张伤口

        NOTHING,            // 什么也不发生

        RANDOM_WATER        // 神秘泉水：随机回血或掉血
    }


    // =========================================================
    // 2. Effect：一个具体效果
    // =========================================================

    /**
     * 一个具体效果。
     *
     * 一个 Effect = 一个 Action + 一个参数。
     *
     * 例如：
     *
     * new Effect(Action.DAMAGE, 15)
     * 表示：失去 15 点生命。
     *
     * new Effect(Action.ADD_CARD, 0)
     * 表示：获得一张随机卡牌。
     *
     * new Effect(Action.ADD_NAMED_RELIC, 0, "猪爆气")
     * 表示：获得名为“猪爆气”的遗物。
     */
    public static class Effect {

        public final Action action;

        /**
         * 数值参数。
         *
         * HEAL / DAMAGE：
         *     表示数值。
         *
         * ADD_CARD / ADD_RELIC / ADD_WOUND：
         *     目前没有使用，可以填 0。
         */
        public final int amount;

        /**
         * 指定遗物名称。
         *
         * 只有 ADD_NAMED_RELIC 使用。
         */
        public final String relicName;


        /**
         * 普通 Effect。
         */
        public Effect(Action action, int amount) {
            this(action, amount, null);
        }


        /**
         * 带指定遗物名称的 Effect。
         */
        public Effect(
                Action action,
                int amount,
                String relicName
        ) {
            this.action = action;
            this.amount = amount;
            this.relicName = relicName;
        }
    }


    // =========================================================
    // 3. Option：一个玩家可以点击的选择
    // =========================================================

    public static class Option {

        public final String label;

        /**
         * 显示给玩家看的实际效果说明。
         */
        public final String effectDesc;

        /**
         * 一个选项可以有多个效果。
         */
        public final List<Effect> effects;


        public Option(
                String label,
                String effectDesc,
                List<Effect> effects
        ) {
            this.label = label;
            this.effectDesc = effectDesc;
            this.effects = effects;
        }
    }


    // =========================================================
    // 4. EventDef 本身
    // =========================================================

    public final String name;

    public final String desc;

    public final List<Option> options;

    /**
     * 左侧事件插图。
     */
    public final String imageName;

    /**
     * 事件专属背景。
     */
    public final String bgName;


    public EventDef(
            String name,
            String desc,
            List<Option> options
    ) {
        this(name, desc, null, options, null);
    }


    public EventDef(
            String name,
            String desc,
            String imageName,
            List<Option> options
    ) {
        this(name, desc, imageName, options, null);
    }


    public EventDef(
            String name,
            String desc,
            List<Option> options,
            String bgName
    ) {
        this(name, desc, null, options, bgName);
    }


    public EventDef(
            String name,
            String desc,
            String imageName,
            List<Option> options,
            String bgName
    ) {
        this.name = name;
        this.desc = desc;
        this.options = options;
        this.imageName = imageName;
        this.bgName = bgName;
    }


    // =========================================================
    // 5. 普通事件池
    // =========================================================

    private static final List<EventDef> NORMAL_POOL = List.of(

            // -------------------------------------------------
            // 岔路口的雕像
            // -------------------------------------------------

            new EventDef(
                    "岔路口的雕像",

                    "一尊古老的石像立在路中间，底座刻着几行模糊的字："
                            + "“向它献上你的血，它将予你回应。”"
                            + "雕像的眼窝似乎亮了一下。",

                    "Event-Goldenldol.png",

                    List.of(

                            new Option(
                                    "以少量献血祈求",
                                    "失去 5点生命，获得一张随机卡牌",
                                    List.of(
                                            new Effect(Action.DAMAGE, 5),
                                            new Effect(Action.ADD_CARD, 0)
                                    )
                            ),

                            new Option(
                                    "以鲜血献祭",
                                    "失去10点生命，获得一件随机遗物",
                                    List.of(
                                            new Effect(Action.DAMAGE, 10),
                                            new Effect(Action.ADD_RELIC, 0)
                                    )
                            ),

                            new Option(
                                    "贪婪的献祭",
                                    "失去13点生命，获得一件随机遗物和一张卡牌",
                                    List.of(
                                            new Effect(Action.DAMAGE, 13),
                                            new Effect(Action.ADD_RELIC, 0),
                                            new Effect(Action.ADD_CARD, 0)
                                    )
                            )
                    )
            ),


            // -------------------------------------------------
            // 废弃的营地
            // -------------------------------------------------

            new EventDef(
                    "废弃的营地",

                    "焦黑的木柴散落在地上，一顶破旧的帐篷歪倒在树林边。\n"
                            + "火堆早已熄灭，但灰烬下面仍残留着一点温度。\n"
                            + "这里似乎曾经属于一支没有走出去的队伍。",

                    "Event-BonfireSpirits.png",

                    List.of(

                            new Option(
                                    "坐下休息",
                                    "回复 10 点生命",
                                    List.of(
                                            new Effect(Action.HEAL, 10)
                                    )
                            ),

                            new Option(
                                    "深入废墟",
                                    "失去 8 点生命，获得一件未持有的随机遗物",
                                    List.of(
                                            new Effect(Action.DAMAGE, 8),
                                            new Effect(Action.ADD_RELIC, 0)
                                    )
                            ),

                            new Option(
                                    "翻找帐篷",
                                    "失去3点生命，获得一张随机卡牌",
                                    List.of(
                                            new Effect(Action.DAMAGE, 3),
                                            new Effect(Action.ADD_CARD, 0)
                                    )
                            )
                    )
            ),


            // -------------------------------------------------
            // 迷雾中的书摊
            // -------------------------------------------------

            new EventDef(
                    "迷雾中的书摊",

                    "一阵风吹过，路旁不知何时多了一个书摊。"
                            + "摊主蒙着面，声音沙哑："
                            + "“挑一样吧，凡人——是磨刀石，还是暖炉？”"+
                    "“知识从来不是免费的。”",

                    "Event-Cleric.png",

                    List.of(

                            new Option(
                                    "要一张卡牌",
                                    "获得一张随机卡牌，但失去 5 点生命",
                                    List.of(
                                            new Effect(Action.DAMAGE, 5),
                                            new Effect(Action.ADD_CARD, 0)
                                    )
                            ),

                            new Option(
                                    "喝口热茶",
                                    "回复 8 点生命"+ "但是会被烫伤\n" ,
                                    List.of(
                                            new Effect(Action.HEAL, 8),
                                            new Effect(Action.ADD_WOUND, 1)
                                    )
                            ),

                            new Option(
                                    "无视摊主",
                                    "径直走开",
                                    List.of(
                                            new Effect(Action.NOTHING, 0)
                                    )
                            )
                    )
            ),


            // -------------------------------------------------
            // 神秘泉水
            // -------------------------------------------------

            new EventDef(
                    "神秘泉水",

                    "一汪清澈的泉水出现在你面前。\n"
                            + "水面平静得有些诡异，"
                            + "你无法判断它是否安全。",

                    "Event-DivineFountain.png",

                    List.of(

                            new Option(
                                    "饮用泉水",
                                    "50% 概率回复 20 点生命，50% 概率失去 15 点生命",
                                    List.of(
                                            new Effect(Action.RANDOM_WATER, 0)
                                    )
                            ),

                            new Option(
                                    "离开",
                                    "什么也没发生",
                                    List.of(
                                            new Effect(Action.NOTHING, 0)
                                    )
                            )
                    )
            ),


            // -------------------------------------------------
            // 神秘书籍
            // -------------------------------------------------

            new EventDef(
                    "神秘书籍",

                    "一本落满灰尘的古老书籍静静地躺在石台上。\n"
                            + "书页没有文字，只有一道道看不懂的符号。\n"
                            + "当你靠近时，书页竟自行翻动起来……",

                    "Event-CursedTome.png",

                    List.of(

                            new Option(
                                    "阅读书籍",
                                    "获得一张随机卡牌，并获得一张“伤口”",
                                    List.of(
                                            new Effect(Action.ADD_CARD, 0),
                                            new Effect(Action.ADD_WOUND, 1)
                                    )
                            ),

                            new Option(
                                    "离开",
                                    "什么也没发生",
                                    List.of(
                                            new Effect(Action.NOTHING, 0)
                                    )
                            )
                    )
            ),


            // -------------------------------------------------
            // 诅咒祭坛
            // -------------------------------------------------

            new EventDef(
                    "诅咒祭坛",

                    "一座漆黑的祭坛矗立在道路中央。\n"
                            + "祭坛周围没有任何声音，只有中央的一枚黑色符文散发着微弱的光。\n"
                            + "你能感觉到其中蕴含着某种力量，但这股力量似乎并不免费。",

                    "Event-ForgottenAltar.png",

                    List.of(

                            new Option(
                                    "触碰符文",
                                    "失去 15 点生命，获得一张随机卡牌",
                                    List.of(
                                            new Effect(Action.DAMAGE, 5),
                                            new Effect(Action.ADD_CARD, 0)
                                    )
                            ),

                            new Option(
                                    "接受诅咒",
                                    "失去 10 点生命，获得一张“伤口”",
                                    List.of(
                                            new Effect(Action.DAMAGE, 10),
                                            new Effect(Action.ADD_WOUND, 1)
                                    )
                            ),

                            new Option(
                                    "转身离开",
                                    "你确定要转身离开吗....",
                                    List.of(
                                            new Effect(Action.DAMAGE, 20)
                                    )
                            )
                    )
            )
    );


    // =========================================================
    // 6. 猪雪峰
    // =========================================================

    private static final double XUEFENG_CHANCE = 0.15;


    public static final EventDef XUEFENG = new EventDef(

            "猪雪峰",

            "你在猪塔中探索，发现了一个奇特的雕像。\n"
                    + "像是一头天使猪站在雪峰上的雕像。\n"
                    + "你从中吸取到了一些力量",

            "encounter/xuefeng.png",

            List.of(

                    new Option(
                            "拿起猪爆气",
                            "战斗开始时，对自身造成不可格挡的 2 点伤害，"
                                    + "对怪物造成 13 点伤害",
                            List.of(
                                    new Effect(
                                            Action.ADD_NAMED_RELIC,
                                            0,
                                            "猪爆气"
                                    )
                            )
                    ),

                    new Option(
                            "拿起猪冰棍",
                            "战斗的前两回合开始时额外获得 1 点能量",
                            List.of(
                                    new Effect(
                                            Action.ADD_NAMED_RELIC,
                                            0,
                                            "猪冰棍"
                                    )
                            )
                    ),

                    new Option(
                            "拿起猪疾速",
                            "战斗开始时获得 1 点敏捷；"
                                    + "篝火的「休息」改为「练起来」，不回血，"
                                    + "改为使本遗物提供的敏捷 +1",
                            List.of(
                                    new Effect(
                                            Action.ADD_NAMED_RELIC,
                                            0,
                                            "猪疾速"
                                    )
                            )
                    )
            ),

            "encounter/xuefeng.png"
    );


    // =========================================================
    // 7. 随机挑选事件
    // =========================================================

    public static EventDef pick(Player player) {

        if (!xuefengDone(player)
                && Math.random() < XUEFENG_CHANCE) {

            player.xuefengSeen = true;

            return XUEFENG;
        }

        return NORMAL_POOL.get(
                (int) (Math.random() * NORMAL_POOL.size())
        );
    }


    private static boolean xuefengDone(Player player) {

        return player != null
                && (
                player.xuefengSeen
                        || RelicFun.hasXuefengRelic(player)
        );
    }


    // =========================================================
    // 8. 全部事件
    // =========================================================

    public static List<EventDef> pool() {

        List<EventDef> all = new ArrayList<>(NORMAL_POOL);

        all.add(XUEFENG);

        return all;
    }


    // =========================================================
    // 9. 根据名字寻找指定遗物
    // =========================================================

    public static Relic relicNamed(String name) {

        return Relic.findByName(name);
    }
}