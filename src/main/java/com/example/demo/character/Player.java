package com.example.demo.character;

import com.example.demo.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家的局内状态：名字、生命值（80/80）、牌组。
 * 一局游戏（选完角色 → 击败 BOSS 或放弃）期间全程带着它。
 */
public class Player {

    /** 角色名 —— 选人页卡片、战斗 HUD、地图 HUD 都读这一个，别再各写一份 */
    public static final String CHARACTER_NAME = "铁甲战猪";

    /**
     * 角色的起始最大生命。
     *
     * <p>选人页左侧介绍面板要显示「生命 80/80」，这里必须是那个唯一的 80 ——
     * 否则以后调数值时，面板会静静地写着旧数字。</p>
     */
    public static final int BASE_MAX_HP = 80;

    /** 选人页左侧介绍面板里的角色简介 */
    public static final String CHARACTER_DESC =
            "铁甲战猪是一个红色面具半遮面的猪形战士。他是个热心肠，"
                    + "常常会向处在饥渴之中的恶魔捐献猪血，由此获得了恶魔的剑。";

    /**
     * 角色的初始遗物。
     *
     * <p>选人页左侧介绍面板会把它（图标 + 名字 + 描述）显示在简介下面，
     * 开局时也是靠这一条加进牌组的 —— 只此一份，别在两处各写一个遗物，
     * 否则面板会显示着和实际开局不一样的遗物。</p>
     */
    public static final Relic STARTER_RELIC = Relic.BURNING_BLOOD;

    public int maxHp = BASE_MAX_HP;

    public int hp = maxHp;
    public final List<Card> deck = new ArrayList<>();
    public final List<Relic> relics = new ArrayList<>(); // 本局获得的遗物
    public boolean restedAtCampfire = false; // 篝火休息后标记
    public int leaveNoteBattles = 0; // 请假条：剩余生效战斗场次

    /**
     * 混沌：本局地图变异标记（起点遗物「混沌」拾取后置 true）。
     *
     * <p>置位后，非固定层节点在 {@link com.example.demo.view.MapView} 里统一画成
     * 「事件」图标，进入时由
     * {@code HelloApplication.handleArrive} 等概率改判成
     * 怪物 / 精英 / 事件 / 火堆 / 宝箱。整局有效，中途不会自己复位。</p>
     */
    public boolean chaos = false;

    public Player() {
        deck.addAll(starterDeck()); // 起始牌组
        relics.add(STARTER_RELIC); // 铁甲战猪固有初始遗物：燃烧之血（战斗结束回 6 血的被动，见 RelicFun.onBattleEnd）
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
