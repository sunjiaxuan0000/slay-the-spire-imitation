package com.example.demo.character;

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
}
