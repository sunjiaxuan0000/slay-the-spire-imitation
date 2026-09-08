package com.example.demo;

import java.util.List;

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
        NOTHING        // 无事发生
    }

    /** 一个选项 */
    public static class Option {
        public final String label;      // 选项名
        public final String effectDesc; // “选项实际效果”文字（显示在选项里）
        public final Action action;     // 真实效果种类
        public final int amount;        // 效果数值（HEAL/DAMAGE 用）

        public Option(String label, String effectDesc, Action action, int amount) {
            this.label = label;
            this.effectDesc = effectDesc;
            this.action = action;
            this.amount = amount;
        }
    }

    public final String name;
    public final String desc;
    public final List<Option> options;

    public EventDef(String name, String desc, List<Option> options) {
        this.name = name;
        this.desc = desc;
        this.options = options;
    }

    // ================= 事件池（进事件节点时随机挑一个） =================

    public static List<EventDef> pool() {
        return List.of(
                new EventDef("岔路口的雕像",
                        "一尊古老的石像立在路中间，底座刻着几行模糊的字："
                                + "“向它献上你的血，它将予你回应。”雕像的眼窝似乎亮了一下。",
                        List.of(
                                new Option("以血相献", "失去 8 点生命", Action.DAMAGE, 8),
                                new Option("轻叩石像", "无事发生，雕像纹丝不动", Action.NOTHING, 0),
                                new Option("转身离开", "没有冒险的必要", Action.NOTHING, 0)
                        )),

                new EventDef("废弃的营地",
                        "地上散落着焦黑的木柴与一顶破帐篷。有人曾在这里扎营，"
                                + "营火早已熄灭，但木头深处似乎还留着一点余温。",
                        List.of(
                                new Option("拨弄余烬", "回复 15 点生命", Action.HEAL, 15),
                                new Option("翻找遗物", "获得一件未持有的随机遗物", Action.ADD_RELIC, 0),
                                new Option("不作停留", "继续赶路", Action.NOTHING, 0)
                        )),

                new EventDef("迷雾中的书摊",
                        "一阵风吹过，路旁不知何时多了一个书摊。摊主蒙着面，"
                                + "声音沙哑：“挑一样吧，凡人——是磨刀石，还是暖炉？”",
                        List.of(
                                new Option("要一张卡牌", "随机一张卡牌加入牌组", Action.ADD_CARD, 0),
                                new Option("喝口热茶", "回复 10 点生命", Action.HEAL, 10),
                                new Option("无视摊主", "径直走开", Action.NOTHING, 0)
                        ))
        );
    }
}
