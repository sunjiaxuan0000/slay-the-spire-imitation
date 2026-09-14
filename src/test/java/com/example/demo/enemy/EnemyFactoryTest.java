package com.example.demo.enemy;

import com.example.demo.view.GameMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EnemyFactory 单元测试：
 * 1) 按楼层的怪池开放规则（前5层无高级池、猪？仅后5层）；
 * 2) 概率分布的统计抽样（容差随样本量设置）；
 * 3) named() 读档重建与精英/BOSS 的种子确定性。
 *
 * <p>注意：基础变体与高级变体是同一个类（{@code Slime.base()/big()} 都是 Slime，
 * {@code CultistPig.base()/veteran()} 都是 CultistPig），只能按 {@code name} 分池。</p>
 */
class EnemyFactoryTest {

    /** 基础池三种怪的名字 */
    private static final Set<String> BASE_POOL_NAMES = Set.of("史莱姆", "邪教猪", "海兵猪");

    private Map<String, Integer> sample(int row, int n) {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Enemy e = EnemyFactory.normal(row);
            counts.merge(e.name, 1, Integer::sum);
        }
        return counts;
    }

    private double ratio(Map<String, Integer> c, String name, int n) {
        return c.getOrDefault(name, 0) / (double) n;
    }

    /** 只统计高级池命中样本（按名字排除基础池三种），返回池内条件分布 */
    private Map<String, Integer> sampleAdvanced(int row, int n) {
        Map<String, Integer> counts = new HashMap<>();
        int got = 0;
        while (got < n) {
            Enemy e = EnemyFactory.normal(row);
            if (BASE_POOL_NAMES.contains(e.name)) continue;
            counts.merge(e.name, 1, Integer::sum);
            got++;
        }
        return counts;
    }

    // ================= 前 5 层：仅基础池 =================

    @Test
    @DisplayName("前5层(row 0~4)只刷基础池：史莱姆/邪教猪/海兵猪，且三种都能刷到")
    void earlyRowsOnlyBasePool() {
        for (int row = 0; row < 5; row++) {
            Map<String, Integer> c = sample(row, 1500);
            for (String name : c.keySet()) {
                assertTrue(BASE_POOL_NAMES.contains(name),
                        "row=" + row + " 出现了非基础池怪物 " + name);
            }
            assertTrue(c.getOrDefault("史莱姆", 0) > 0);
            assertTrue(c.getOrDefault("邪教猪", 0) > 0);
            assertTrue(c.getOrDefault("海兵猪", 0) > 0);
        }
    }

    @Test
    @DisplayName("基础池内部比例 史莱姆40%/邪教猪30%/海兵猪30%（容差±4.5%）")
    void earlyRowRatios() {
        int n = 6000;
        Map<String, Integer> c = sample(0, n);
        assertEquals(0.40, ratio(c, "史莱姆", n), 0.045);
        assertEquals(0.30, ratio(c, "邪教猪", n), 0.045);
        assertEquals(0.30, ratio(c, "海兵猪", n), 0.045);
    }

    // ================= 高级池开放节奏 =================

    @Test
    @DisplayName("第6层(row5)高级池占60%；末层(row16)占80%（容差±4%）")
    void advancedPoolRateByRow() {
        int n = 6000;

        Map<String, Integer> c5 = sample(5, n);
        double advanced5 = c5.entrySet().stream()
                .filter(e -> !BASE_POOL_NAMES.contains(e.getKey()))
                .mapToInt(Map.Entry::getValue).sum() / (double) n;
        assertEquals(0.60, advanced5, 0.04, "row5 高级池应约60%");

        Map<String, Integer> c16 = sample(16, n);
        double advanced16 = c16.entrySet().stream()
                .filter(e -> !BASE_POOL_NAMES.contains(e.getKey()))
                .mapToInt(Map.Entry::getValue).sum() / (double) n;
        assertEquals(0.80, advanced16, 0.04, "row16 高级池应约80%");
    }

    @Test
    @DisplayName("猪？只在后5层(row>=12)出现；row 5~11 永不出现")
    void mysteryPigOnlyLateRows() {
        for (int row = 5; row <= 11; row++) {
            Map<String, Integer> c = sample(row, 1200);
            assertEquals(0, c.getOrDefault("猪？", 0),
                    "row=" + row + " 不应出现猪？");
        }
        Map<String, Integer> late = sample(13, 6000);
        assertTrue(late.getOrDefault("猪？", 0) > 0,
                "row13 应能刷出猪？");
    }

    // ================= 高级池内部比例 =================

    @Test
    @DisplayName("row5~11 高级池内部：大史莱姆40%/老兵邪教猪30%/神风猪30%（容差±6%）")
    void midAdvancedPoolRatios() {
        int n = 5000;
        Map<String, Integer> c = sampleAdvanced(8, n);
        assertEquals(0.40, ratio(c, "大史莱姆", n), 0.06);
        assertEquals(0.30, ratio(c, "老兵邪教猪", n), 0.06);
        assertEquals(0.30, ratio(c, "神风猪", n), 0.06);
        assertEquals(0, c.getOrDefault("猪？", 0));
    }

    @Test
    @DisplayName("后5层高级池内部：大史莱姆20%/老兵邪教猪30%/神风猪10%/猪？40%（容差±5%）")
    void lateAdvancedPoolRatios() {
        int n = 6000;
        Map<String, Integer> c = sampleAdvanced(16, n);
        assertEquals(0.20, ratio(c, "大史莱姆", n), 0.05);
        assertEquals(0.30, ratio(c, "老兵邪教猪", n), 0.05);
        assertEquals(0.10, ratio(c, "神风猪", n), 0.05);
        assertEquals(0.40, ratio(c, "猪？", n), 0.05);
    }

    // ================= 变体身份 =================

    @Test
    @DisplayName("工厂产物身份正确：普通怪非精英非BOSS，精英带标记，BOSS带标记")
    void flagsBySource() {
        for (int i = 0; i < 50; i++) {
            Enemy n = EnemyFactory.normal(i % 17);
            assertFalse(n.isElite);
            assertFalse(n.isBoss);
            Enemy e = EnemyFactory.elite(i);
            assertTrue(e.isElite);
            assertFalse(e.isBoss);
            Enemy b = EnemyFactory.boss();
            assertTrue(b.isBoss);
        }
    }

    // ================= named() 读档重建 =================

    @Test
    @DisplayName("named() 按名字精确重建全部12种敌人")
    void namedRoundTrip() {
        assertEquals(Slime.class, EnemyFactory.named("史莱姆", 0, false).getClass());
        assertEquals(Slime.class, EnemyFactory.named("大史莱姆", 10, false).getClass());
        assertEquals(CultistPig.class, EnemyFactory.named("邪教猪", 0, false).getClass());
        assertEquals(CultistPig.class, EnemyFactory.named("老兵邪教猪", 10, false).getClass());
        assertEquals(BoomPig.class, EnemyFactory.named("神风猪", 10, false).getClass());
        assertEquals(SeaSoldierPig.class, EnemyFactory.named("海兵猪", 0, false).getClass());
        assertEquals(MysteryPig.class, EnemyFactory.named("猪？", 13, false).getClass());
        assertEquals(GuardPig.class, EnemyFactory.named("卫士猪", 0, false).getClass());
        assertEquals(FlashPig.class, EnemyFactory.named("闪电猪", 0, false).getClass());
        assertEquals(ChaosPig.class, EnemyFactory.named("混沌猪", 0, false).getClass());
        assertEquals(DukePorcodraco.class, EnemyFactory.named("猪龙鱼公爵", 16, true).getClass());
        assertEquals(GiantBoarKnight.class, EnemyFactory.named("巨猪骑士", 16, true).getClass());
    }

    @Test
    @DisplayName("named() 重建保留变体数值：大史莱姆68血/250贴图，老兵邪教猪60血/170贴图")
    void namedPreservesVariantStats() {
        Enemy big = EnemyFactory.named("大史莱姆", 10, false);
        assertEquals(68, big.maxHp);
        assertEquals("史莱姆", big.getPortraitName());
        assertEquals(250, big.getPortraitSize());

        Enemy vet = EnemyFactory.named("老兵邪教猪", 10, false);
        assertEquals(60, vet.maxHp);
        assertEquals("邪教猪", vet.getPortraitName());
        assertEquals(170, vet.getPortraitSize());
    }

    @Test
    @DisplayName("named() 名字为null/空/无法识别时退回随机生成，不返回null")
    void namedFallback() {
        assertNotNull(EnemyFactory.named(null, 0, false));
        assertNotNull(EnemyFactory.named("", 0, false));
        assertNotNull(EnemyFactory.named("不存在的怪", 0, false));
        Enemy fallbackBoss = EnemyFactory.named("旧档怪", 16, true);
        assertNotNull(fallbackBoss);
        assertTrue(fallbackBoss.isBoss);
    }

    // ================= 精英 / BOSS 确定性 =================

    @Nested
    @DisplayName("种子确定性")
    class SeedTests {

        @Test
        @DisplayName("同种子两次生成同一只精英（不可SL刷怪）")
        void eliteDeterministic() {
            for (long seed : new long[]{0L, 1L, 42L, -7L, 123456789L}) {
                assertEquals(EnemyFactory.elite(seed).getClass(),
                        EnemyFactory.elite(seed).getClass());
            }
        }

        @Test
        @DisplayName("精英三种（卫士猪/闪电猪/混沌猪）都可出现")
        void eliteCoversAllThree() {
            Set<Class<?>> seen = new HashSet<>();
            for (long seed = 0; seed < 1000; seed++) {
                seen.add(EnemyFactory.elite(seed).getClass());
            }
            assertEquals(Set.of(GuardPig.class, FlashPig.class, ChaosPig.class), seen);
        }

        @Test
        @DisplayName("指定BOSS种类必出对应BOSS；null退随机")
        void bossByKind() {
            assertTrue(EnemyFactory.boss(GameMap.BossKind.BOAR) instanceof GiantBoarKnight);
            assertTrue(EnemyFactory.boss(GameMap.BossKind.DUKE) instanceof DukePorcodraco);
            assertNotNull(EnemyFactory.boss(null));
        }
    }
}
