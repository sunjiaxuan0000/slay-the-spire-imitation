package com.example.demo.character;

import com.example.demo.card.Card;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Player 纯逻辑单元测试：初始状态、伤害/治疗边界、
 * 最大生命值增减（巨猪骑士削上限可减到 0）、遗物去重、初始牌组。
 */
class PlayerTest {

    @Test
    @DisplayName("初始状态：80/80血、100金币、燃烧之血、10张起始牌(5打击4防御1痛击)")
    void initialState() {
        Player p = new Player();
        assertEquals(Player.BASE_MAX_HP, p.maxHp);
        assertEquals(80, p.hp());
        assertEquals(Player.INITIAL_GOLD, p.gold);
        assertTrue(p.relics.contains(Relic.BURNING_BLOOD));

        assertEquals(10, p.deck.size());
        Map<Card.Kind, Long> counts = p.deck.stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c.kind, java.util.stream.Collectors.counting()));
        assertEquals(5L, counts.get(Card.Kind.STRIKE));
        assertEquals(4L, counts.get(Card.Kind.DEFEND));
        assertEquals(1L, counts.get(Card.Kind.BASH));
    }

    @Test
    @DisplayName("伤害不会扣成负数")
    void damageClampsToZero() {
        Player p = new Player();
        p.damage(30);
        assertEquals(50, p.hp());
        p.damage(1000);
        assertEquals(0, p.hp());
        p.damage(10); // 已是 0，再受伤仍为 0
        assertEquals(0, p.hp());
    }

    @Test
    @DisplayName("治疗不会超过上限")
    void healClampsToMax() {
        Player p = new Player();
        p.damage(50);
        p.heal(10);
        assertEquals(40, p.hp());
        p.heal(9999);
        assertEquals(80, p.hp());
    }

    @Test
    @DisplayName("削减上限：当前血同步夹到新上限，上限可减为0")
    void reduceMaxHpClampsHp() {
        Player p = new Player();
        p.reduceMaxHp(30);
        assertEquals(50, p.maxHp);
        assertEquals(50, p.hp());          // 满血时削上限，当前血一起降

        p.damage(40);                     // 剩 10/50
        p.reduceMaxHp(20);
        assertEquals(30, p.maxHp);
        assertEquals(10, p.hp());         // 当前血低于新上限，保持不变

        p.reduceMaxHp(40);                // 上限减到 0
        assertEquals(0, p.maxHp);
        assertEquals(0, p.hp());
    }

    @Test
    @DisplayName("reduceMaxHp 对非正数无效")
    void reduceMaxHpIgnoresNonPositive() {
        Player p = new Player();
        p.reduceMaxHp(0);
        p.reduceMaxHp(-10);
        assertEquals(80, p.maxHp);
        assertEquals(80, p.hp());
    }

    @Test
    @DisplayName("增加上限：上限与当前血同时增加")
    void increaseMaxHp() {
        Player p = new Player();
        p.increaseMaxHp(5);
        assertEquals(85, p.maxHp);
        assertEquals(85, p.hp());

        p.damage(50);                     // 35/85
        p.increaseMaxHp(5);
        assertEquals(90, p.maxHp);
        assertEquals(40, p.hp());         // 只加 5 血，不回满
    }

    @Test
    @DisplayName("同名遗物不会重复获得")
    void addRelicDeduplicates() {
        Player p = new Player();
        int before = p.relics.size();
        p.addRelic(Relic.BURNING_BLOOD);  // 初始已有
        assertEquals(before, p.relics.size());
        p.addRelic(null);                 // null 安全
        assertEquals(before, p.relics.size());
    }

    @Test
    @DisplayName("保温杯获得时立即+8最大生命")
    void thermosRelicEffect() {
        Player p = new Player();
        p.addRelic(Relic.findByName("保温杯"));
        assertEquals(88, p.maxHp);
        assertEquals(88, p.hp());
    }
}
