package com.example.demo.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 杀戮尖塔式的地图模型 + 生成算法。
 *
 * 地图是一张只能“从下往上走”的分层路线图：
 *   floors.get(0)      = 最下面（起点层）
 *   floors.get(rows-1) = 最上面（BOSS 层）
 * 每个节点记下 next（上一层能去哪）/ prev（下一层从哪来）。
 *
 * 生成规则：
 *   1. 起点层 1 个起点、BOSS 层 1 个 BOSS、中间层随机 1~4 个节点；
 *   2. 相邻两层连边：每个下层节点连 1~2 条，并保证上层每个节点至少有一条入边
 *      （否则那个节点永远走不到）；
 *   3. 按进度填类型：前期普通怪、约 45% 处休息点、约 80% 处宝箱、后期出现精英。
 */
public class GameMap {

    /** 节点类型 */
    public enum NodeType {
        START("起点", "起"),
        MONSTER("怪物", "怪"),
        ELITE("精英", "精"),
        REST("休息", "休"),
        TREASURE("宝箱", "箱"),
        EVENT("事件", "？"),
        BOSS("BOSS", "王");

        final String label;   // 完整名字
        final String glyph;   // 圆圈里显示的字

        NodeType(String label, String glyph) {
            this.label = label;
            this.glyph = glyph;
        }
    }

    /** 一个地图节点 */
    public static class MapNode {
        public final int row;        // 第几层（0 = 最底层）
        public final int col;        // 第几列（0~MAX_COLS-1）
        public final NodeType type;
        public final List<MapNode> next = new ArrayList<>(); // 上一层可去的节点
        public final List<MapNode> prev = new ArrayList<>(); // 下一层连上来的节点

        MapNode(int row, int col, NodeType type) {
            this.row = row;
            this.col = col;
            this.type = type;
        }

        @Override
        public String toString() {
            return type.label + "@(" + row + "," + col + ")";
        }
    }

    // ================= 地图尺寸与固定层 =================
    public static final int ROWS = 17;       // 一共 17 层（row 0 = 起点 … row 16 = BOSS）
    public static final int MAX_COLS = 5;    // 每层最多 5 列
    public static final int CENTER_COL = MAX_COLS / 2; // 中列(起点/BOSS 用)
    public static final int TREASURE_ROW = 8; // 第 9 层 = 固定宝箱层（row 编号从 0 起）
    public static final int REST_ROW = ROWS - 2; // BOSS(17层) 前一层，第 16 层 = 固定篝火层

    public final List<List<MapNode>> floors = new ArrayList<>(); // floors.get(row)
    public MapNode current = null;        // 玩家现在在哪（null = 还没出发）

    private GameMap() {
        for (int i = 0; i < ROWS; i++) {
            floors.add(new ArrayList<>());
        }
    }

    // ================= 生成 =================

    /** 随机生成一张新地图。 */
    public static GameMap generate() {
        return generate(new Random().nextLong());
    }

    /** 用固定种子生成（同一种子 = 同一张图，方便调试/复制每日挑战）。 */
    public static GameMap generate(long seed) {
        Random rnd = new Random(seed);
        GameMap map = new GameMap();

        // ---- 第 1 步：每层摆节点 ----
        // 类型约束：不能“连续两个休息”或“连续两个精英”
        //   prevRest/prevElite = 上一行是否出现过；curX = 本行已经出现过(同层不再放第二个)
        boolean prevRest = false;
        boolean prevElite = false;
        int prevStart = 0; // 上一行连续列的起始列（用于“随机游走”，避免直上直下）

        for (int row = 0; row < ROWS; row++) {
            if (row == 0) {
                map.floors.get(row).add(new MapNode(row, CENTER_COL, NodeType.START));
                prevStart = CENTER_COL;
                continue;
            }
            if (row == ROWS - 1) {
                map.floors.get(row).add(new MapNode(row, CENTER_COL, NodeType.BOSS));
                continue;
            }

            // 全图统一密度：每层 4~5 个房间
            int count = 3 + rnd.nextInt(3);

            // 列号：以“随机游走”的方式选连续列（相对上一行最多偏 2 列）
            // → 相邻两层错开，不会形成一整条竖直直线
            int maxStart = MAX_COLS - count;
            int start = clamp(prevStart + (rnd.nextInt(5) - 2), 0, maxStart);
            prevStart = start;

            boolean curRest = false;
            boolean curElite = false;
            // 固定篝火层的下一层不允许出现篝火（避免“休息→休息”直连）
            boolean forceNoRest = (row == REST_ROW - 1);
            for (int c = start; c < start + count; c++) {
                NodeType t = typeFor(row, rnd,
                        forceNoRest || prevRest || curRest, prevElite || curElite);
                if (t == NodeType.REST) curRest = true;
                if (t == NodeType.ELITE) curElite = true;
                map.floors.get(row).add(new MapNode(row, c, t));
            }
            prevRest = curRest;
            prevElite = curElite;
        }

        // ---- 第 2 步：相邻两层连边（每层随机整体斜移，路线不再笔直） ----
        for (int row = 0; row < ROWS - 1; row++) {
            connectRows(map.floors.get(row), map.floors.get(row + 1), rnd);
        }
        return map;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** 休息(非固定层)最早出现的层：山脚前 4 层不刷篝火，避免过早休息 */
    private static final int MIN_REST_ROW = 5;

    /** 给 row 层定类型。banRest/banElite 用于“不能连续两个休息/精英”。 */
    private static NodeType typeFor(int row, Random rnd, boolean banRest, boolean banElite) {
        if (row == TREASURE_ROW) return NodeType.TREASURE; // 第 9 层：固定宝箱层
        if (row == REST_ROW) return NodeType.REST;         // BOSS 前一层：固定篝火层
        if (row == 1) return NodeType.MONSTER;             // 起点后第一层只出普通怪（不出事件/休息）

        int roll = rnd.nextInt(100);

        // 休息：只在过了山脚段出现，且不能被禁用（防连续）
        if (row >= MIN_REST_ROW && roll < 15 && !banRest) return NodeType.REST;

        // 事件：频率提高 —— 早段(还没休息) 25%，之后 22%
        if (row < MIN_REST_ROW) {
            if (roll < 45) return NodeType.EVENT;
            return NodeType.MONSTER; // 山脚只出怪/事件
        }
        if (roll < 30) return NodeType.EVENT;

        if (row < 4) return NodeType.MONSTER;  // 宝箱之前纯普通怪

        // 精英概率随楼层逐渐提高
        int eliteChance = (row+1)*6; // 20% → 45%
        eliteChance = Math.min(50, eliteChance);
        if (rnd.nextInt(100) >= eliteChance || banElite) return NodeType.MONSTER;
        return NodeType.ELITE;
    }

    /**
     * 连接相邻两层（以“直线主轨”为主，少量互连；数学上保证 0 交叉）。
     *
     * 原理：下层 A、上层 B 各自按列排序；
     *   “下层第 i 个 → 上层第 round(i*(m-1)/(n-1)) 个”映射单调不减
     *   → 两端顺序永不颠倒 → 线段不可能交叉。
     *
     * · 主轨：按上面排名单调一一连接（直为主）；
     * · 互连：约 60% 的下层节点再加一条 rank(i+1) 的边（单调安全）；
     * · 画面上的“不呆板”交给 MapView 的列位抖动处理，拓扑保持干净。
     */
    private static void connectRows(List<MapNode> A, List<MapNode> B, Random rnd) {
        int n = A.size(), m = B.size();

        if (n == 1) {
            for (MapNode b : B) connect(A.get(0), b); // 起点扇出
            return;
        }
        if (m == 1) {
            for (MapNode a : A) connect(a, B.get(0)); // BOSS 收拢
            return;
        }

        if (n >= m) {
            // 主轨 + 较多互连（只加“向前多跨一格”的安全边，保持单调 → 不交叉）
            for (int i = 0; i < n; i++) {
                int j = rankB(i, n, m);
                connect(A.get(i), B.get(j));
                if (i < n - 1 && rnd.nextInt(100) < 60) {
                    int j2 = rankB(i + 1, n, m);
                    connect(A.get(i), B.get(j2));
                }
            }
        } else {
            // 上层比下层多：从上层出发按排名认领来源（下层自然扇出，多路可选）
            for (int j = 0; j < m; j++) {
                connect(A.get(rankA(j, n, m)), B.get(j));
            }
        }

        // 保险：理论上映射已全覆盖，这里只防极端遗漏（用同一条单调规则补回）
        for (int j = 0; j < m; j++) {
            if (!B.get(j).prev.isEmpty()) continue;
            for (int i = 0; i < n; i++) {
                if (rankB(i, n, m) == j) { connect(A.get(i), B.get(j)); break; }
            }
        }
        for (int i = 0; i < n; i++) {
            if (!A.get(i).next.isEmpty()) continue;
            for (int j = 0; j < m; j++) {
                if (rankA(j, n, m) == i) { connect(A.get(i), B.get(j)); break; }
            }
        }
    }

    /** 下层第 i 个节点的“排名搭档”在上层里的下标 */
    private static int rankB(int i, int n, int m) {
        return Math.round(i * (m - 1f) / (n - 1));
    }

    /** 上层第 j 个节点的“排名搭档”在下层里的下标 */
    private static int rankA(int j, int n, int m) {
        return Math.round(j * (n - 1f) / (m - 1));
    }

    private static void connect(MapNode a, MapNode b) {
        if (!a.next.contains(b)) {
            a.next.add(b);
            b.prev.add(a);
        }
    }
}
