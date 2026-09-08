package com.example.demo;

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

    public static final int ROWS = 10;    // 一共 10 层（0~9）
    public static final int MAX_COLS = 5; // 每层最多 5 列

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
        for (int row = 0; row < ROWS; row++) {
            int count;
            if (row == 0 || row == ROWS - 1) {
                count = 1;                 // 起点层 / BOSS 层都只有 1 个
            } else {
                count = 1 + rnd.nextInt(4); // 中间层 1~4 个
            }

            // 从 0..MAX_COLS-1 里挑 count 个不同的列号
            List<Integer> cols = new ArrayList<>();
            for (int c = 0; c < MAX_COLS; c++) cols.add(c);
            Collections.shuffle(cols, rnd);
            cols = cols.subList(0, count);
            Collections.sort(cols);

            // 填类型并放入地图
            for (int c : cols) {
                NodeType t = typeFor(row, rnd);
                map.floors.get(row).add(new MapNode(row, c, t));
            }
        }

        // ---- 第 2 步：相邻两层连边 ----
        for (int row = 0; row < ROWS - 1; row++) {
            connectRows(map.floors.get(row), map.floors.get(row + 1));
        }
        return map;
    }

    /** 给 row 层的节点定类型（起点/BOSS 固定，其它按进度）。 */
    private static NodeType typeFor(int row, Random rnd) {
        if (row == 0) return NodeType.START;
        if (row == ROWS - 1) return NodeType.BOSS;

        int restRow = (int) Math.round((ROWS - 1) * 0.45);   // 大约一半进度处的休息点
        int treasureRow = (int) Math.round((ROWS - 1) * 0.8); // 接近塔顶的宝箱
        if (row == restRow) return NodeType.REST;
        if (row == treasureRow) return NodeType.TREASURE;
        if (row > restRow) {                                  // 休息点之后可能出现精英
            return rnd.nextInt(100) < 35 ? NodeType.ELITE : NodeType.MONSTER;
        }
        return NodeType.MONSTER;
    }

    /**
     * 连接相邻两层（保证不交叉）。
     *
     * 原理：下层 A 和上层 B 各自按列号排好序，连线让“排名”单调对应——
     * 左边的下层节点只连向左边的上层节点区域，右边的连向右边的。
     * 只要两条线的端点顺序不颠倒，线段就不会交叉。
     *
     * 分两种情况：
     *   下层节点 >= 上层节点：从下层出发铺“主轨”，保证每个下层都能往上走；
     *   上层节点更多：从上层出发认领来源，下层节点会“扇形”连出多个上层。
     */
    private static void connectRows(List<MapNode> A, List<MapNode> B) {
        int n = A.size(), m = B.size();
        if (n == 1) {
            // 只有一个下层节点：从同一点扇出去连所有上层，不会交叉
            for (MapNode b : B) connect(A.get(0), b);
            return;
        }

        if (n >= m) {
            // 下层多：按排名单调铺轨（左边连左边、右边连右边）
            for (int i = 0; i < n; i++) {
                int j = Math.round(i * (m - 1f) / (n - 1));
                connect(A.get(i), B.get(j));
                // 再顺手连一条“副轨”，让路线有一点分叉（两端顺序依然单调，不会交叉）
                if (i < n - 1) {
                    int j2 = Math.round((i + 1) * (m - 1f) / (n - 1));
                    connect(A.get(i), B.get(j2));
                }
            }
        } else {
            // 上层多：每个上层认领一个下层来源（排名单调），保证每个上层都可达
            for (int j = 0; j < m; j++) {
                int i = Math.round(j * (n - 1f) / (m - 1));
                connect(A.get(i), B.get(j));
            }
        }
    }

    private static void connect(MapNode a, MapNode b) {
        if (!a.next.contains(b)) {
            a.next.add(b);
            b.prev.add(a);
        }
    }
}
