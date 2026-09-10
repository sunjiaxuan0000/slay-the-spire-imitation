package com.example.demo.character;

import com.example.demo.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家的局内状态：名字、生命值（80/80）、牌组。
 * 一局游戏（选完角色 → 击败 BOSS 或放弃）期间全程带着它。
 */
public class Player {

    public static final String CHARACTER_NAME = "战士";
    public int maxHp = 80;

    private int hp = maxHp;
    public final List<Card> deck = new ArrayList<>();
    public final List<Relic> relics = new ArrayList<>(); // 本局获得的遗物
    public boolean restedAtCampfire = false; // 篝火休息后标记

    public Player() {
        deck.addAll(starterDeck()); // 起始牌组
    }

    /** 获得遗物（同名不重复拿，避免效果叠加） */
    public void addRelic(Relic r) {
        if (r == null) return;
        for (Relic have : relics) {
            if (have.name.equals(r.name)) return;
        }
        relics.add(r);
    }

    /** 起始牌组：10 张 = 5 打击 + 4 防御 + 1 痛击 */
    public static List<Card> starterDeck() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 5; i++) cards.add(Card.strike());
        for (int i = 0; i < 4; i++) cards.add(Card.defend());
        cards.add(Card.bash());
        return cards;
    }

    public int hp() { return hp; }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    public void damage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    /** 增加最大生命值，同时恢复等量生命 */
    public void increaseMaxHp(int amount) {
        maxHp += amount;
        hp += amount;
    }
}
