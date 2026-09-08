package com.example.demo;

import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 地图界面：把 GameMap 画出来，支持“点击可到达的节点往上走”。
 *
 * 画法：连线（Line）垫底，节点（彩色圆）盖在线上。
 * 规则：只能走到当前节点的 next（上层）里；没选到的节点变暗不可点；
 *       走上一个节点后触发 onArrive(类型)，由外面决定发生什么。
 *
 * 宽度自适应：内容宽度随窗口变化时，节点列和连线会整体重排（配合 ScrollPane 的 fitToWidth）。
 */
public class MapView extends Pane {

    private static final double TOP = 70;         // 塔顶（BOSS 层）离内容顶部的距离
    private static final double ROW_SPACING = 95; // 层与层之间的纵向间距（拉长、不挤）
    private static final double CONTENT_H = TOP + ROW_SPACING * (GameMap.ROWS - 1) + 80; // 内容总高 >> 窗口高
    private static final double NODE_SIZE = 46;   // 圆节点直径

    // 各类型节点颜色
    private static String colorOf(GameMap.NodeType t) {
        return switch (t) {
            case START   -> "#16a34a";
            case MONSTER -> "#dc2626";
            case ELITE   -> "#f59e0b";
            case REST    -> "#2563eb";
            case TREASURE-> "#b45309";
            case BOSS    -> "#7f1d1d";
        };
    }

    /** 一个可见节点：彩色圆 + 里面一个字的图标 */
    private static class NodeView extends StackPane {
        final GameMap.MapNode node;
        final Label glyph = new Label();

        NodeView(GameMap.MapNode node) {
            this.node = node;
            setPrefSize(NODE_SIZE, NODE_SIZE);
            setMaxSize(NODE_SIZE, NODE_SIZE);
            glyph.setText(node.type.glyph);
            glyph.setTextFill(Color.WHITE);
            glyph.setFont(Font.font(16));
            getChildren().add(glyph);
        }
    }

    /** 一条连线：记住两端是哪两个节点，窗口变宽时好重新算坐标 */
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
    private final Consumer<GameMap.NodeType> onArrive; // 走上节点后回调
    private final ScrollPane scroll;                   // 外层滚动容器（视图跟随用）
    private final boolean interactive;                 // true=可点击行走；false=纯查看(战斗中看地图)
    private final List<NodeView> views = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Label header;
    private final Label hint;

    public MapView(GameMap map, Consumer<GameMap.NodeType> onArrive, ScrollPane scroll,
                   boolean interactive) {
        this.map = map;
        this.onArrive = onArrive;
        this.scroll = scroll;
        this.interactive = interactive;

        setPrefHeight(CONTENT_H); // 高度固定很长，宽度交给 ScrollPane(fitToWidth) 决定
        setStyle("-fx-background-color: #0b1020;");
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

        // 窗口（内容）宽度一变 → 所有节点和连线按新宽度重排
        widthProperty().addListener(o -> relayout());
        relayout();

        refresh();
    }

    // ================= 坐标计算（随内容宽度自适应） =================

    /** 某列中心的 x 坐标：列在内容宽度里均匀铺开，左右留边距 */
    private double nodeX(int col) {
        double w = getWidth() > 0 ? getWidth() : 1280; // 还没布局时按默认宽度算
        double margin = Math.max(50, w * 0.06);        // 左右边距随宽度变化
        if (GameMap.MAX_COLS <= 1) return w / 2;
        return margin + col * ((w - 2 * margin) / (GameMap.MAX_COLS - 1));
    }

    /** 某行的 y 坐标（纵向固定，靠 ScrollPane 滚动查看） */
    private double nodeY(int row) {
        return TOP + (GameMap.ROWS - 1 - row) * ROW_SPACING;
    }

    /** 把每个节点圆和每条连线挪到正确位置（宽度变了就调用） */
    private void relayout() {
        for (NodeView v : views) {
            v.setLayoutX(nodeX(v.node.col));
            v.setLayoutY(nodeY(v.node.row));
        }
        for (Edge e : edges) {
            e.line.setStartX(nodeX(e.a.col) + NODE_SIZE / 2);
            e.line.setStartY(nodeY(e.a.row) + NODE_SIZE / 2);
            e.line.setEndX(nodeX(e.b.col) + NODE_SIZE / 2);
            e.line.setEndY(nodeY(e.b.row) + NODE_SIZE / 2);
        }
    }

    private void draw() {
        // 1) 先画连线（在节点下面）
        for (List<GameMap.MapNode> rowNodes : map.floors) {
            for (GameMap.MapNode a : rowNodes) {
                for (GameMap.MapNode b : a.next) { // a 下层 → b 上层
                    Line line = new Line(0, 0, 0, 0);
                    line.setStroke(Color.rgb(71, 85, 105, 0.9));
                    line.setStrokeWidth(2);
                    getChildren().add(line);
                    edges.add(new Edge(line, a, b));
                }
            }
        }

        // 2) 再画节点圆（盖在连线上）
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

    /** 一个节点现在能不能点？ */
    private boolean reachable(GameMap.MapNode n) {
        if (map.current == null) return n.row == 0;          // 没出发时只能点起点层
        return map.current.next.contains(n);                  // 只能往上走一层
    }

    private void refresh() {
        for (NodeView v : views) {
            GameMap.MapNode n = v.node;
            boolean ok = reachable(n);
            boolean isCurrent = (n == map.current);

            String style;
            if (isCurrent) {
                style = String.format("-fx-background-color:%s; -fx-background-radius:23; "
                        + "-fx-border-color:#fde047; -fx-border-width:3; -fx-border-radius:23;", colorOf(n.type));
            } else {
                style = String.format("-fx-background-color:%s; -fx-background-radius:23; "
                        + "-fx-border-color:%s; -fx-border-width:%d; -fx-border-radius:23;",
                        colorOf(n.type), ok ? "#f8fafc" : "transparent", ok ? 2 : 0);
            }
            v.setStyle(style);
            v.setOpacity(ok || !interactive ? 1.0 : 0.4); // 不可点的变暗（查看模式全部点亮）
            if (interactive) {
                v.setCursor(ok ? Cursor.HAND : Cursor.DEFAULT);
            }
        }

        // 顶部进度 + 底部提示
        if (!interactive) {
            header.setText("地图（查看模式）");
            hint.setText("滚轮滚动查看 · 点外部任意处或“关闭”退出");
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

    /** 点击节点：可以走才走，走完刷新并通知外面。 */
    private void click(GameMap.MapNode n) {
        if (!interactive) return;
        if (!reachable(n)) return;
        map.current = n;
        refresh();
        followCurrent(); // 视图跟随，保证当前节点和上面的路线都在屏幕中间附近
        onArrive.accept(n.type);
    }

    /** 滚动外层 ScrollPane，让当前节点大致出现在视口中间。 */
    private void followCurrent() {
        if (scroll == null || map.current == null) return;
        double contentH = getHeight();                       // 地图内容总高
        double viewH = scroll.getViewportBounds().getHeight(); // 可见窗口高
        double maxV = contentH - viewH;
        if (maxV <= 0) return;

        double cy = nodeY(map.current.row) + NODE_SIZE / 2; // 当前节点中心的 y
        double v = (cy - viewH / 2) / maxV;                 // 让中心出现在视口中部
        v = Math.max(0, Math.min(1, v));                    // 限制在 0~1
        scroll.setVvalue(v);
    }
}
