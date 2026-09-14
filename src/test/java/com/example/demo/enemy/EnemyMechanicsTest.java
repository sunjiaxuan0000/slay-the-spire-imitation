package com.example.demo.enemy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enemy 体系的纯逻辑单元测试（不启动 JavaFX）。
 * 覆盖：轮盘循环、变体数值、仪式时序、蓄势自爆、锁血、二阶段、护甲衰减、闪避等机制。
 */
class EnemyMechanicsTest {

    // ================= 基类默认行为 =================

    @Test
    @DisplayName("基类默认：无闪避/无持久护甲/不削上限/无反伤/无仪式")
    void baseDefaults() {
        Enemy e = Slime.base();
        assertFalse(e.hasDodge());
        assertFalse(e.dodge());
        assertFalse(e.isBlockPersistent());
        assertFalse(e.cutsMaxHpOnAttack());
        assertEquals(0.0, e.getReflectRate());
        assertEquals(0, e.getRitualPower());
    }

    @Test
    @DisplayName("默认 BUFF：力量 += value；敏捷加成格挡")
    void defaultBuffAndDexterity() {
        Enemy e = Slime.base();
        e.applyBuff(new Enemy.Step(Enemy.Intent.BUFF, 3));
        assertEquals(3, e.power);
        e.gainDexterity(6);
        assertEquals(11, e.blockGain(5)); // 5 + 6
    }

    // ================= 史莱姆 =================

    @Nested
    @DisplayName("史莱姆")
    class SlimeTests {

        @Test
        @DisplayName("基础史莱姆：36 血，攻击8/防御5/虚弱2，立绘210，攻击塞1黏液")
        void baseStats() {
            Slime s = Slime.base();
            assertEquals("史莱姆", s.name);
            assertEquals(36, s.maxHp);
            assertEquals(36, s.hp);
            assertFalse(s.isBoss);
            assertEquals(210, s.getPortraitSize());
            assertEquals("史莱姆", s.getPortraitName());
            assertEquals(1, s.getSlimeOnAttack());

            assertEquals(Enemy.Intent.ATTACK, s.current().intent);
            assertEquals(8, s.current().value);
            s.advance();
            assertEquals(Enemy.Intent.DEFEND, s.current().intent);
            assertEquals(5, s.current().value);
            s.advance();
            assertEquals(Enemy.Intent.WEAKEN, s.current().intent);
            assertEquals(2, s.current().value);
        }

        @Test
        @DisplayName("轮盘三步循环，第三步后回到攻击")
        void wheelCycles() {
            Slime s = Slime.base();
            s.advance(); s.advance(); // 到虚弱
            s.advance();               // 回到起点
            assertEquals(Enemy.Intent.ATTACK, s.current().intent);
            assertEquals(8, s.current().value);
        }

        @Test
        @DisplayName("大史莱姆：68 血，攻击16/防御14，复用史莱姆立绘但贴图250")
        void bigStats() {
            Slime s = Slime.big();
            assertEquals("大史莱姆", s.name);
            assertEquals(68, s.maxHp);
            assertEquals(16, s.current().value);
            s.advance();
            assertEquals(14, s.current().value);
            assertEquals("史莱姆", s.getPortraitName());
            assertEquals(250, s.getPortraitSize());
        }

        @Test
        @DisplayName("攻击意图描述包含基础伤害+力量")
        void intentTextAttack() {
            Slime s = Slime.base();
            assertTrue(s.intentText().contains("攻击 8"));
            s.power = 2;
            assertTrue(s.intentText().contains("攻击 10"));
        }
    }

    // ================= 邪教猪 / 仪式时序 =================

    @Nested
    @DisplayName("邪教猪")
    class CultistTests {

        @Test
        @DisplayName("基础邪教猪：45 血，首回合仪式2，之后攻击6循环")
        void baseRitualWheel() {
            CultistPig p = CultistPig.base();
            assertEquals(45, p.maxHp);
            assertEquals(Enemy.Intent.RITUAL, p.current().intent);
            assertEquals(2, p.current().value);
            p.advance();
            assertEquals(Enemy.Intent.ATTACK, p.current().intent);
            assertEquals(6, p.current().value);
            p.advance();
            assertEquals(Enemy.Intent.ATTACK, p.current().intent); // 永久攻击循环
        }

        @Test
        @DisplayName("老兵邪教猪：60 血、攻击8，复用邪教猪立绘、贴图170")
        void veteranStats() {
            CultistPig p = CultistPig.veteran();
            assertEquals("老兵邪教猪", p.name);
            assertEquals(60, p.maxHp);
            assertEquals("邪教猪", p.getPortraitName());
            assertEquals(170, p.getPortraitSize());
            p.advance();
            assertEquals(8, p.current().value);
        }

        @Test
        @DisplayName("仪式时序：激活当回合0力量，下回合跳过，之后每回合+2")
        void ritualTiming() {
            CultistPig p = CultistPig.base();
            // BattleView 在 RITUAL 意图执行时调用 setRitualPower
            p.setRitualPower(2);
            assertEquals(0, p.power);
            assertTrue(p.isRitualJustActivated());

            // 仪式后的第一个玩家回合（敌人回合开始 onTurnStart）：跳过
            p.onTurnStart();
            assertEquals(0, p.power);
            assertFalse(p.isRitualJustActivated());

            // 再之后每回合 +2
            p.onTurnStart();
            assertEquals(2, p.power);
            p.onTurnStart();
            assertEquals(4, p.power);
        }
    }

    // ================= 神风猪：蓄势/自爆/锁血 =================

    @Nested
    @DisplayName("神风猪")
    class BoomPigTests {

        @Test
        @DisplayName("56 血，轮盘 攻击10/蓄势1/蓄势1，每层自爆16")
        void planAndPerStackDamage() {
            BoomPig b = new BoomPig();
            assertEquals(56, b.maxHp);
            assertEquals(Enemy.Intent.ATTACK, b.current().intent);
            assertEquals(10, b.current().value);
            b.advance();
            assertEquals(Enemy.Intent.CHARGE, b.current().intent);
            assertEquals(1, b.current().value);
            b.advance();
            assertEquals(1, b.current().value);
            assertEquals(16, b.getChargeDamagePerStack());
        }

        @Test
        @DisplayName("自爆伤害 = 蓄势层数 × 16；0 层为 0")
        void explodeDamageByStacks() {
            BoomPig b = new BoomPig();
            assertEquals(0, b.explodeDamage());
            b.addChargeStacks(1);
            assertEquals(16, b.explodeDamage());
            b.addChargeStacks(2);
            assertEquals(48, b.explodeDamage());
        }

        @Test
        @DisplayName("锁血：HP归0首次拦截成功，current 强制变为 EXPLODE；再次拦截失败")
        void deathLockFlow() {
            BoomPig b = new BoomPig();
            b.addChargeStacks(2); // 32 点自爆伤害
            assertFalse(b.isDeathLocked());

            assertTrue(b.triggerDeathLock());
            assertTrue(b.isDeathLocked());
            assertEquals(Enemy.Intent.EXPLODE, b.current().intent);
            assertEquals(32, b.current().value);
            assertTrue(b.intentText().contains("自爆 32"));

            assertFalse(b.triggerDeathLock(), "已锁过血，不能再拦截第二次");
        }
    }

    // ================= 卫士猪 =================

    @Test
    @DisplayName("卫士猪：80血精英，30%反伤，攻击20/反伤2/防御20/攻击18")
    void guardPigStats() {
        GuardPig g = new GuardPig();
        assertEquals("卫士猪", g.name);
        assertEquals(80, g.maxHp);
        assertTrue(g.isElite);
        assertFalse(g.isBoss);
        assertEquals(0.3, g.getReflectRate(), 1e-9);
        int[] values = {20, 2, 20, 18};
        Enemy.Intent[] intents = {
                Enemy.Intent.ATTACK, Enemy.Intent.REFLECT,
                Enemy.Intent.DEFEND, Enemy.Intent.ATTACK
        };
        for (int i = 0; i < 4; i++) {
            assertEquals(intents[i], g.current().intent);
            assertEquals(values[i], g.current().value);
            g.advance();
        }
        assertEquals(Enemy.Intent.ATTACK, g.current().intent); // 四步循环
    }

    // ================= 闪电猪：闪避（统计） =================

    @Nested
    @DisplayName("闪电猪")
    class FlashPigTests {

        @Test
        @DisplayName("70血精英，拥有闪避能力")
        void stats() {
            FlashPig f = new FlashPig();
            assertEquals("闪电猪", f.name);
            assertEquals(70, f.maxHp);
            assertTrue(f.isElite);
            assertTrue(f.hasDodge());
            assertEquals(40, f.dodgeChanceUi());
        }

        @Test
        @DisplayName("闪避率统计约为 40%（20000 次，容差 ±5%）")
        void dodgeRateStatistical() {
            FlashPig f = new FlashPig();
            int n = 20000;
            int dodged = 0;
            for (int i = 0; i < n; i++) {
                if (f.dodge()) dodged++;
            }
            double rate = (double) dodged / n;
            assertTrue(rate > 0.35 && rate < 0.45,
                    "闪避率应约为 0.4，实际 " + rate);
        }
    }

    // ================= 巨猪骑士（BOSS 护甲机制） =================

    @Nested
    @DisplayName("巨猪骑士")
    class GiantBoarKnightTests {

        @Test
        @DisplayName("15血/500护甲，BOSS，护甲持久，攻击削血上限，立绘300")
        void baseStats() {
            GiantBoarKnight g = new GiantBoarKnight();
            assertEquals("巨猪骑士", g.name);
            assertEquals(15, g.maxHp);
            assertEquals(500, g.block);
            assertTrue(g.isBoss);
            assertTrue(g.isBlockPersistent());
            assertTrue(g.cutsMaxHpOnAttack());
            assertEquals(300, g.getPortraitSize());
        }

        @Test
        @DisplayName("回合开始自损当前护甲 4%（向下取整，不减成负数）")
        void armorDecay() {
            GiantBoarKnight g = new GiantBoarKnight();
            g.onTurnStart();
            assertEquals(480, g.block);       // 500 - 20
            g.onTurnStart();
            assertEquals(461, g.block);       // 480 - floor(19.2) = 461
            g.block = 1;
            g.onTurnStart();
            assertEquals(1, g.block);         // floor(0.04) = 0，保持 1
        }

        @Test
        @DisplayName("攻击公式：破甲前 5+损甲×3%，破甲后 10+损甲×2%（护甲≤250 破甲）")
        void attackFormula() {
            GiantBoarKnight g = new GiantBoarKnight();
            Enemy.Step attack = new Enemy.Step(Enemy.Intent.ATTACK, 0);
            assertEquals(5, g.baseAttackDamage(attack));   // 损甲 0

            g.block = 400;                                  // 损甲 100，未破甲
            assertFalse(g.isArmorBroken());
            assertEquals(8, g.baseAttackDamage(attack));   // 5 + 3

            g.block = 251;                                  // 损甲 249，仍 >250 未破甲
            assertEquals(12, g.baseAttackDamage(attack));  // 5 + floor(7.47)

            g.block = 250;                                  // 恰好 50%，进入破甲
            assertTrue(g.isArmorBroken());
            assertEquals(15, g.baseAttackDamage(attack));  // 10 + 5

            g.block = 0;                                    // 损甲 500
            assertEquals(20, g.baseAttackDamage(attack));  // 10 + 10
        }
    }

    // ================= 猪龙鱼公爵：血量二阶段 =================

    @Nested
    @DisplayName("猪龙鱼公爵")
    class DukeTests {

        @Test
        @DisplayName("200血BOSS，一阶段轮盘 BUFF3/攻击13/虚弱3/防御10/攻击16")
        void phaseOne() {
            DukePorcodraco d = new DukePorcodraco();
            assertEquals(200, d.maxHp);
            assertTrue(d.isBoss);
            assertEquals(Enemy.Intent.BUFF, d.current().intent);
            d.checkPhaseTransition();
            assertFalse(d.isSecondPhase, "满血不转阶段");
        }

        @Test
        @DisplayName("血量高于50%不转阶段；降至50%（含）转阶段并重置为攻击18轮盘")
        void phaseByHp() {
            DukePorcodraco d = new DukePorcodraco();
            d.advance(); // 一阶段走到攻击13
            d.hp = 101;
            d.checkPhaseTransition();
            assertFalse(d.isSecondPhase);
            assertEquals(13, d.current().value);

            d.hp = 100;
            d.checkPhaseTransition();
            assertTrue(d.isSecondPhase);
            assertEquals(Enemy.Intent.ATTACK, d.current().intent);
            assertEquals(18, d.current().value); // 指针重置，二阶段首步
        }
    }

    // ================= 猪？：回合数二阶段 =================

    @Nested
    @DisplayName("猪？")
    class MysteryPigTests {

        @Test
        @DisplayName("100血普通怪（非BOSS），一阶段 攻击12/防御20/吐黏液3/防御30")
        void phaseOneStats() {
            MysteryPig m = new MysteryPig();
            assertEquals("猪？", m.name);
            assertEquals(100, m.maxHp);
            assertFalse(m.isBoss);
            assertEquals(Enemy.Intent.ATTACK, m.current().intent);
            m.advance();
            assertEquals(Enemy.Intent.DEFEND, m.current().intent);
            m.advance();
            assertEquals(Enemy.Intent.SPIT, m.current().intent);
            assertEquals(3, m.current().value);
        }

        @Test
        @DisplayName("血量降到1也不转阶段；第5回合后转阶段，轮盘变为攻击30")
        void phaseByTurnNotHp() {
            MysteryPig m = new MysteryPig();
            m.hp = 1;
            m.checkPhaseTransition();
            assertFalse(m.isSecondPhase, "猪？不按血量转阶段");

            // 前 4 回合均不触发
            for (int i = 0; i < 4; i++) {
                m.advance();
                m.checkPhaseTransition();
                assertFalse(m.isSecondPhase, "第 " + (i + 1) + " 回合不应转阶段");
            }
            // 第 5 次行动（第 5 回合）后
            m.advance();
            m.checkPhaseTransition();
            assertTrue(m.isSecondPhase);
            assertEquals(Enemy.Intent.ATTACK, m.current().intent);
            assertEquals(30, m.current().value);
        }
    }

    // ================= 海兵猪 =================

    @Test
    @DisplayName("海兵猪：45血，防御20/潮湿1/攻击12/强化3，立绘180")
    void seaSoldierStats() {
        SeaSoldierPig s = new SeaSoldierPig();
        assertEquals("海兵猪", s.name);
        assertEquals(45, s.maxHp);
        assertEquals(180, s.getPortraitSize());
        assertEquals(Enemy.Intent.DEFEND, s.current().intent);
        assertEquals(20, s.current().value);
        s.advance();
        assertEquals(Enemy.Intent.WET, s.current().intent);
        s.advance();
        assertEquals(12, s.current().value);
        s.advance();
        assertEquals(Enemy.Intent.BUFF, s.current().intent);
    }

    // ================= 混沌猪：种子随机与轮盘循环 =================

    @Nested
    @DisplayName("混沌猪")
    class ChaosPigTests {

        @Test
        @DisplayName("104血精英，30%反伤；轮盘前两步一次后从防御12处循环")
        void wheelLoop() {
            ChaosPig c = new ChaosPig();
            assertEquals(104, c.maxHp);
            assertTrue(c.isElite);
            assertEquals(0.3, c.getReflectRate(), 1e-9);

            List<Enemy.Intent> seq = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                seq.add(c.current().intent);
                c.advance();
            }
            // BUFF, ATTACK, 然后进入循环段 DEFEND, BUFF, ATTACK, DEFEND, BUFF
            assertEquals(List.of(
                    Enemy.Intent.BUFF, Enemy.Intent.ATTACK,
                    Enemy.Intent.DEFEND, Enemy.Intent.BUFF, Enemy.Intent.ATTACK,
                    Enemy.Intent.DEFEND, Enemy.Intent.BUFF
            ), seq);
        }

        @Test
        @DisplayName("相同战斗种子 → 增益抽签序列完全一致（读档不可刷）")
        void seedDeterminism() {
            ChaosPig a = new ChaosPig();
            ChaosPig b = new ChaosPig();
            a.setBattleSeed(987654321L);
            b.setBattleSeed(987654321L);

            List<ChaosPig.ChaosBuff> seqA = new ArrayList<>();
            List<ChaosPig.ChaosBuff> seqB = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                if (a.current().intent == Enemy.Intent.BUFF) seqA.add(a.pendingBuff());
                if (b.current().intent == Enemy.Intent.BUFF) seqB.add(b.pendingBuff());
                a.advance();
                b.advance();
            }
            assertFalse(seqA.isEmpty());
            assertEquals(seqA, seqB);
        }

        @Test
        @DisplayName("未获得闪避增益前 hasDodge/dodge 恒为 false")
        void noDodgeBeforeBuff() {
            ChaosPig c = new ChaosPig();
            c.setBattleSeed(1L);
            assertFalse(c.dodgeGranted());
            assertFalse(c.hasDodge());
            assertFalse(c.dodge());
        }
    }
}
