package com.example.demo;

import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 地图界面：把 GameMap 画出来，支持“点击可到达的节点往上走”。
 *
 * 画法：连线用 icons/arrow.png（沿节点方向拉伸旋转）垫底，
 *       节点用各类型图标（icons/*.png）盖在线上。
 * 规则：只能走到当前节点的 next（上层）里；没选到的节点变暗不可点；
 *       走上一个节点后触发 onArrive(类型)，由外面决定发生什么。
 *
 * 宽度自适应：内容宽度随窗口变化时，节点和箭头会整体重排（配合 ScrollPane 的 fitToWidth）。
 */
public class MapView extends Pane {

    private static final double ROW_SPACING = 95; // 层与层之间的纵向间距（拉长、不挤）
    private static final double NODE_SIZE = 46;   // 普通节点图标直径
    private static final double BIG_NODE_SIZE = 300; // 起点/BOSS 大图标直径（远大于普通节点）
    // BOSS 独立放在顶部留白带：顶部留一点边距，BOSS 图标下方与下一层留空隙，不再叠压
    private static final double BOSS_ZONE_TOP = 24;
    private static final double GRID_TOP = BOSS_ZONE_TOP + BIG_NODE_SIZE + 18; // 普通层起点
    private static final double CONTENT_H =
            GRID_TOP + (GameMap.ROWS - 2) * ROW_SPACING + BIG_NODE_SIZE + 60; // 底部为大起点图标留足空间

    // ================= 贴图 =================

    /** 图标资源（相对 resources/com/example/demo/） */
    private static final Map<GameMap.NodeType, String> ICON_FILES = new HashMap<>();
    static {
        ICON_FILES.put(GameMap.NodeType.MONSTER,  "icons/monster.png");
        ICON_FILES.put(GameMap.NodeType.ELITE,    "icons/elite.png");
        ICON_FILES.put(GameMap.NodeType.REST,     "icons/rest.png");
        ICON_FILES.put(GameMap.NodeType.TREASURE, "icons/chest.png");
        ICON_FILES.put(GameMap.NodeType.EVENT,    "icons/encounter.png");
        ICON_FILES.put(GameMap.NodeType.START,    "icons/deep.png");    // 起点大图标
        ICON_FILES.put(GameMap.NodeType.BOSS,     "icons/fishron.png"); // BOSS 大图标
    }

    /** 图标尺寸：起点/BOSS 用大图标，其余普通大小 */
    private static double nodeDiameter(GameMap.NodeType t) {
        return (t == GameMap.NodeType.START || t == GameMap.NodeType.BOSS)
                ? BIG_NODE_SIZE : NODE_SIZE;
    }

    private static Image loadImage(String path) {
        var in = MapView.class.getResourceAsStream(path);
        return in == null ? null : new Image(in);
    }

    private static String iconFile(GameMap.NodeType t) {
        return ICON_FILES.get(t);
    }

    // 各类型节点颜色（只有没图/老的起终点节点用）
    private static String colorOf(GameMap.NodeType t) {
        return switch (t) {
            case START   -> "#16a34a";
            case MONSTER -> "#dc2626";
            case ELITE   -> "#f59e0b";
            case REST    -> "#2563eb";
            case TREASURE-> "#b45309";
            case EVENT   -> "#0d9488";
            case BOSS    -> "#7f1d1d";
        };
    }

    // ================= 节点视图 =================

    /** 一个可见节点：图标图（或老式彩色圆） + 状态光环 */
    private static class NodeView extends StackPane {
        final GameMap.MapNode node;
        final Circle ring = new Circle();   // 当前/可到达光环

        NodeView(GameMap.MapNode node) {
            this.node = node;
            double d = nodeDiameter(node.type); // 大图标节点用大尺寸
            setPrefSize(d, d);
            setMaxSize(d, d);

            String icon = iconFile(node.type);
            if (icon != null) {
                ImageView img = new ImageView(loadImage(icon));
                img.setPreserveRatio(true);
                img.setFitWidth(d);
                img.setFitHeight(d);
                img.setMouseTransparent(true);
                getChildren().add(img);
            } else {
                // 兜底：彩色圆 + 字
                StackPane disc = new StackPane();
                disc.setPrefSize(d, d);
                disc.setMaxSize(d, d);
                disc.setStyle("-fx-background-color: " + colorOf(node.type)
                        + "; -fx-background-radius: " + (d / 2) + ";");
                Label glyph = new Label(node.type.glyph);
                glyph.setTextFill(Color.WHITE);
                glyph.setFont(Font.font(d / 3.0));
                glyph.setStyle("-fx-font-weight: bold;");
                disc.getChildren().add(glyph);
                getChildren().add(disc);
            }

            ring.setFill(null);
            ring.setStroke(null);
            ring.setRadius(d / 2 + 5); // 比图标大一圈的光环
            ring.setStrokeWidth(3);
            ring.setMouseTransparent(true);
            getChildren().add(ring);
        }
    }

    /** 一条连线：虚线 Line（保存两端节点，重排时更新端点） */
    private static class Edge {
        final Line line;
        final GameMap.MapNode a;
        final GameMap.MapNode b;

        Edge(Line line, GameMap.MapNode a, GameMap.MapNode b) {
            this.line = line;
            this.a = a;
            this.b = b;
        }
    }

    private final GameMap map;
    private final Consumer<GameMap.NodeType> onArrive;
    private final ScrollPane scroll;
    private final boolean interactive;
    private final List<NodeView> views = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Label header;
    private final Label hint;

    /** 每列的水平小抖动：只影响显示坐标（拓扑不变） */
    private final double[] colJitter = new double[GameMap.MAX_COLS];

    {
        java.util.Random jr = new java.util.Random(20240601L);
        for (int i = 0; i < colJitter.length; i++) {
            colJitter[i] = (jr.nextDouble() * 2 - 1) * 100;
        }
    }

    public MapView(GameMap map, Consumer<GameMap.NodeType> onArrive, ScrollPane scroll,
                   boolean interactive) {
        this.map = map;
        this.onArrive = onArrive;
        this.scroll = scroll;
        this.interactive = interactive;

        setPrefHeight(CONTENT_H);
        setBackground(makeMapBackground()); // map.png 拉伸铺满
        setCursor(Cursor.DEFAULT);

        header = new Label("");
        header.setTextFill(Color.WHITE);
        header.setFont(Font.font(20));
        header.layoutXProperty().bind(widthProperty().subtract(header.widthProperty()).divide(2));
        header.setLayoutY(16);
        getChildren().add(header);

        hint = new Label("");
        hint.setTextFill(Color.rgb(148, 163, 184));
        hint.setFont(Font.font(14));
        hint.layoutXProperty().bind(widthProperty().subtract(hint.widthProperty()).divide(2));
        hint.setLayoutY(CONTENT_H - 30);
        getChildren().add(hint);

        draw();

        widthProperty().addListener(o -> relayout());
        relayout();
        refresh();
    }

    // ================= 坐标 =================

    private double nodeX(int col) {
        double w = getWidth() > 0 ? getWidth() : 1280;
        double margin = Math.max(50, w * 0.06);
        if (GameMap.MAX_COLS <= 1) return w / 2;
        double lane = margin + col * ((w - 2 * margin) / (GameMap.MAX_COLS - 1));
        // 中央列（起点/BOSS 所在列）不抖动，保证大图标严格居中
        if (col != GameMap.CENTER_COL && col >= 0 && col < colJitter.length) {
            lane += colJitter[col];
        }
        return lane;
    }

    private double nodeY(int row) {
        if (row == GameMap.ROWS - 1) {
            return BOSS_ZONE_TOP; // BOSS 大图标抬高到顶部留白带
        }
        return GRID_TOP + (GameMap.ROWS - 2 - row) * ROW_SPACING; // 其余 0..15 层正常排布
    }

    private void relayout() {
        for (NodeView v : views) {
            v.setLayoutX(nodeX(v.node.col));
            v.setLayoutY(nodeY(v.node.row));
        }
        for (Edge e : edges) {
            double x1 = nodeX(e.a.col) + nodeDiameter(e.a.type) / 2;
            double y1 = nodeY(e.a.row) + nodeDiameter(e.a.type) / 2;
            double x2 = nodeX(e.b.col) + nodeDiameter(e.b.type) / 2;
            double y2 = nodeY(e.b.row) + nodeDiameter(e.b.type) / 2;
            e.line.setStartX(x1);
            e.line.setStartY(y1);
            e.line.setEndX(x2);
            e.line.setEndY(y2);
        }
    }

    private void draw() {
        // 1) 虚线连线（在节点下面）
        for (List<GameMap.MapNode> rowNodes : map.floors) {
            for (GameMap.MapNode a : rowNodes) {
                for (GameMap.MapNode b : a.next) { // a 下层 → b 上层
                    Line line = new Line(0, 0, 0, 0);
                    line.setStroke(Color.rgb(148, 163, 184, 0.9)); // 虚线的灰色
                    line.setStrokeWidth(3);
                    line.setStrokeLineCap(StrokeLineCap.ROUND);
                    line.getStrokeDashArray().addAll(4.0, 14.0);   // 虚线：10px 实 + 7px 空
                    line.setMouseTransparent(true);
                    getChildren().add(line);
                    edges.add(new Edge(line, a, b));
                }
            }
        }

        // 2) 节点图标（盖在连线上）
        for (List<GameMap.MapNode> rowNodes : map.floors) {
            for (GameMap.MapNode n : rowNodes) {
                NodeView v = new NodeView(n);
                if (interactive) {
                    v.setOnMouseClicked(e -> click(n));
                }
                getChildren().add(v);
                views.add(v);
            }
        }
    }

    // ================= 状态刷新 =================

    private boolean reachable(GameMap.MapNode n) {
        if (map.current == null) return n.row == 0;
        return map.current.next.contains(n);
    }

    private void refresh() {
        for (NodeView v : views) {
            GameMap.MapNode n = v.node;
            boolean ok = reachable(n);
            boolean isCurrent = (n == map.current);

            if (isCurrent) {
                v.ring.setStroke(Color.rgb(253, 224, 71, 0.95)); // 金圈 = 当前位置
                v.ring.setStrokeWidth(4);
            } else if (ok && interactive) {
                v.ring.setStroke(Color.rgb(248, 250, 252, 0.9)); // 白圈 = 可走
                v.ring.setStrokeWidth(2);
            } else {
                v.ring.setStroke(null); // 不可走 / 查看模式不加圈
            }
            v.setOpacity(ok || !interactive ? 1.0 : 0.45);
            if (interactive) {
                v.setCursor(ok ? Cursor.HAND : Cursor.DEFAULT);
            }
        }

        if (!interactive) {
            // 战斗内查看地图：不写“只读”字样，正常显示当前层信息
            if (map.current == null) {
                header.setText("地图");
            } else {
                header.setText("第 " + (map.current.row + 1) + " / " + GameMap.ROWS + " 层（"
                        + map.current.type.label + "）");
            }
            hint.setText("滚轮滚动浏览地图 · Esc 返回");
        } else if (map.current == null) {
            header.setText("地图 · 点击起点出发");
            hint.setText("滚轮上下浏览地图 · 白色光圈可走 · Esc 返回");
        } else {
            header.setText("地图 · 第 " + (map.current.row + 1) + " / " + GameMap.ROWS + " 层"
                    + "（" + map.current.type.label + "）");
            hint.setText(map.current.row == GameMap.ROWS - 1
                    ? "已抵达塔顶 · 滚轮可回看路线 · Esc 返回主菜单"
                    : "选择上方带白圈的节点继续 · Esc 返回主菜单");
        }
    }

    private void click(GameMap.MapNode n) {
        if (!interactive) return;
        if (!reachable(n)) return;
        map.current = n;
        refresh();
        followCurrent();
        onArrive.accept(n.type);
    }

    /** 把“第 row 层”滚动到屏幕中间（战斗/事件结束后回到地图、战斗内查看地图时用） */
    public void scrollToLayer(int row) {
        if (scroll == null) return;
        double contentH = getHeight();
        double viewH = scroll.getViewportBounds().getHeight();
        double maxV = contentH - viewH;
        if (maxV <= 0) return;

        double cy = nodeY(row) + NODE_SIZE / 2;
        double v = (cy - viewH / 2) / maxV;
        v = Math.max(0, Math.min(1, v));
        scroll.setVvalue(v);
    }

    private void followCurrent() {
        if (map.current != null) {
            scrollToLayer(map.current.row);
        }
    }

    /** 地图背景：map.png 拉伸铺满整个地图内容区 */
    private static Background makeMapBackground() {
        Image image = loadImage("map.png");
        BackgroundImage bi = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1, 1, true, true, false, false)
        );
        return new Background(bi);
    }
}
