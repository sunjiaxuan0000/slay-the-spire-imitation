package com.example.demo.character;

import java.util.ArrayList;
import java.util.List;

/**
 * 遗物：暂时只记录名字和描述。
 * 以后加效果时，在战斗/休息/地图对应的地方检查 player 的 relics 即可。
 */
public class Relic {
    public final String name;
    public final String desc;

    public Relic(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    /**
     * 全部遗物（起点三选一 / 宝箱随机 / 开发者模式面板都用这一份，避免到处硬编码）。
     * 每调用一次都返回新对象，调用方可以随便改这个列表。
     */
    public static List<Relic> pool() {
        return new ArrayList<>(List.of(
                new Relic("青铜怀表", "战斗开始时获得 2 点格挡"),
                new Relic("请假条", "每回合多抽 1 张牌"),
                new Relic("保温杯", "每场战斗开始时恢复 10 点生命")
        ));
    }
}
