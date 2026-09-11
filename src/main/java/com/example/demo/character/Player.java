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

    public int hp = maxHp;
    public final List<Card> deck = new ArrayList<>();
    public final List<Relic> relics = new ArrayList<>(); // 本局获得的遗物
    public boolean restedAtCampfire = false; // 篝火休息后标记
    public int leaveNoteBattles = 0; // 请假条：剩余生效战斗场次

    public Player() {
        deck.addAll(starterDeck()); // 起始牌组
        relics.add(Relic.BURNING_BLOOD); // 战士固有初始遗物：燃烧之血（战斗结束回 6 血的被动，见 RelicFun.onBattleEnd）
    }

    /** 获得遗物（同名不重复拿，避免效果叠加） */
    public void addRelic(Relic r) {
        if (r == null) return;
        for (Relic have : relics) {
            if (have.name.equals(r.name)) return;
        }
        relics.add(r);
        // 遗物获得时的即时效果（保温杯、请假条等）委托给 RelicFun
        RelicFun.onRelicObtained(this, r, null);
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

    /** 削减最大生命值（巨猪骑士的攻击），当前生命同步夹到新上限；上限可减为 0 并判定死亡 */
    public void reduceMaxHp(int amount) {
        if (amount <= 0) return;
        maxHp = Math.max(0, maxHp - amount);
        hp = Math.max(0, Math.min(hp, maxHp));
    }
}
