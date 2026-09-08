package com.example.demo;

import javafx.animation.AnimationTimer;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 贪吃蛇游戏。
 * 整块游戏区是一个网格：COLS 列 x ROWS 行，每格 TILE 像素。
 */
public class SnakeGame extends Pane {

    // ---------- 网格参数 ----------
    public static final int COLS = 30;
    public static final int ROWS = 20;
    public static final int TILE = 24;

    // ---------- 颜色 ----------
    private static final Color BG      = Color.rgb(22, 33, 26);   // 背景
    private static final Color HEAD    = Color.rgb(217, 249, 157);// 蛇头
    private static final Color BODY_A  = Color.rgb(101, 163, 13); // 身体(深)
    private static final Color BODY_B  = Color.rgb(132, 204, 22); // 身体(浅)
    private static final Color FOOD    = Color.rgb(239, 68, 68);  // 食物
    private static final Color TEXT_COLOR = Color.rgb(229, 231, 235);

    // ---------- 速度：每隔一段时间走一格 ----------
    private static final long BASE_TICK_NANOS = 70_000_000L; // 0.14 秒
    private static final long MIN_TICK_NANOS  =  50_000_000L; // 最快 0.06 秒
    private static final long SPEED_UP_NANOS  =   4_000_000L; // 每吃一个快一点

    // ---------- 方向 ----------
    public enum Dir { UP, DOWN, LEFT, RIGHT }

    private final Random random = new Random();

    private final List<Integer> body = new ArrayList<>();       // 蛇身各格：值 = row * COLS + col
    private final List<Rectangle> bodyNodes = new ArrayList<>();// 和 body 一一对应的可见方块

    private final Rectangle foodNode = new Rectangle(TILE, TILE);
    private final Text scoreText = new Text("分数：0");
    private final Label msgLabel = new Label();   // 暂停 / 结束提示

    private Dir dir = Dir.RIGHT;
    private Dir pendingDir = null;   // 本次 tick 前按键设的方向（还没生效）
    private int food = -1;
    private int score = 0;

    private boolean running = true;  // false = 游戏结束
    private boolean paused = false;
    private long tickNanos = BASE_TICK_NANOS;
    private long lastTick = 0;

    private final AnimationTimer timer;

    public SnakeGame() {
        setPrefSize(COLS * TILE, ROWS * TILE);
        setStyle("-fx-background-color: #16211a;");

        // 食物方块
        foodNode.setArcWidth(8);
        foodNode.setArcHeight(8);
        foodNode.setFill(FOOD);
        getChildren().add(foodNode);

        // 右上角分数
        scoreText.setFill(TEXT_COLOR);
        scoreText.setFont(Font.font(16));
        scoreText.setTranslateX(10);
        scoreText.setTranslateY(24);
        getChildren().add(scoreText);

        // 居中的提示文字（暂停/游戏结束），默认隐藏
        msgLabel.setTextFill(Color.WHITE);
        msgLabel.setFont(Font.font(22));
        msgLabel.setVisible(false);
        msgLabel.layoutXProperty().bind(
                widthProperty().subtract(msgLabel.widthProperty()).divide(2));
        msgLabel.layoutYProperty().bind(
                heightProperty().subtract(msgLabel.heightProperty()).divide(2));
        getChildren().add(msgLabel);

        reset();

        // 游戏主循环：每帧检查是否到“走一格”的时间
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!running || paused) {
                    lastTick = 0;          // 暂停/结束后让计时归零，避免恢复时连走好几格
                    return;
                }
                if (lastTick == 0) lastTick = now;
                if (now - lastTick >= tickNanos) {
                    lastTick = now;
                    tick();
                }
            }
        };
        timer.start();
    }

    // ================= 对外接口（由 HelloApplication 的键盘事件调用） =================

    /** 玩家按方向键/WASD：设一个还没生效的方向（防止原地掉头）。 */
    public void setDirection(Dir d) {
        if (!running || paused) return;
        Dir eff = (pendingDir != null) ? pendingDir : dir;
        if (d == eff || d == opposite(eff)) return;  // 同方向或反方向都忽略
        pendingDir = d;
    }

    /** 空格：暂停 / 继续。 */
    public void togglePause() {
        if (!running) return;
        paused = !paused;
        if (paused) {
            showMsg("已暂停\n按 空格 继续");
        } else {
            hideMsg();
        }
    }

    /** R：重新开始。 */
    public void restart() {
        reset();
    }

    /** 停掉游戏循环（切回主菜单时调用，避免后台空转）。 */
    public void stopGame() {
        timer.stop();
    }

    // ================= 游戏逻辑 =================

    /** 走一格：更新蛇头位置、吃食物、撞墙/撞自己判断。 */
    private void tick() {
        if (pendingDir != null) {
            dir = pendingDir;
            pendingDir = null;
        }

        int head = body.get(0);
        int headRow = head / COLS;
        int headCol = head % COLS;

        int newRow = headRow, newCol = headCol;
        switch (dir) {
            case UP    -> newRow--;
            case DOWN  -> newRow++;
            case LEFT  -> newCol--;
            case RIGHT -> newCol++;
        }

        // 撞墙
        if (newRow < 0 || newRow >= ROWS || newCol < 0 || newCol >= COLS) {
            gameOver();
            return;
        }

        int newHead = newRow * COLS + newCol;
        boolean eating = (newHead == food);

        // 撞自己（不吃食物时尾巴会移走，所以最后一个格子不算）
        int checkLen = eating ? body.size() : body.size() - 1;
        for (int i = 0; i < checkLen; i++) {
            if (body.get(i) == newHead) {
                gameOver();
                return;
            }
        }

        body.add(0, newHead);
        if (eating) {
            score += 10;
            scoreText.setText("分数：" + score);
            addBodyNode();
            if (tickNanos > MIN_TICK_NANOS) tickNanos -= SPEED_UP_NANOS; // 越吃越快
            if (!spawnFood()) {        // 整张地图都被蛇占满
                running = false;
                showMsg("你赢了！\n按 R 重新开始");
            }
        } else {
            body.remove(body.size() - 1);
        }

        renderBody();
    }

    private void gameOver() {
        running = false;
        showMsg("游戏结束 得分：" + score + "\n按 R 重新开始");
    }

    /** 找一块空格子放食物；没有空格返回 false（蛇已占满全图）。 */
    private boolean spawnFood() {
        if (body.size() >= (COLS-1) * (ROWS-1)) return false;
        int cell;
        do {
            cell = random.nextInt(COLS * ROWS);
        } while (body.contains(cell)||cell%COLS==0||cell%COLS==COLS-1||cell/COLS==0||cell/COLS==ROWS-1);
        food = cell;
        foodNode.setX((food %COLS) * TILE);
        foodNode.setY((food / COLS) * TILE);
        return true;
    }

    // ================= 画面刷新 =================

    private void renderBody() {
        for (int i = 0; i < bodyNodes.size(); i++) {
            int cell = body.get(i);
            Rectangle seg = bodyNodes.get(i);
            seg.setX((cell % COLS) * TILE);
            seg.setY((cell / COLS) * TILE);
            // 蛇头亮一点，身体交替深浅
            seg.setFill(i == 0 ? HEAD : (i % 2 == 0 ? BODY_A : BODY_B));
        }
    }

    // ================= 初始化 =================

    /** 回到开局状态：蛇在中间、长 3 格、向右走。 */
    private void reset() {
        // 清掉旧的身体方块
        for (Rectangle r : bodyNodes) getChildren().remove(r);
        bodyNodes.clear();
        body.clear();

        int centerRow = ROWS / 2;
        int centerCol = COLS / 2;
        body.add(centerRow * COLS + centerCol);       // 蛇头
        body.add(centerRow * COLS + (centerCol - 1)); // 身体
        body.add(centerRow * COLS + (centerCol - 2)); // 尾巴

        for (int i = 0; i < body.size(); i++) {
            addBodyNode();
        }

        dir = Dir.RIGHT;
        pendingDir = null;
        score = 0;
        scoreText.setText("分数：0");
        tickNanos = BASE_TICK_NANOS;
        running = true;
        paused = false;
        hideMsg();
        renderBody();
        spawnFood();
    }

    /** 在蛇尾追加一个可见方块（节点数始终和 body 一致）。 */
    private void addBodyNode() {
        Rectangle seg = new Rectangle(TILE - 1, TILE - 1);
        seg.setArcWidth(4);
        seg.setArcHeight(4);
        bodyNodes.add(seg);
        getChildren().add(seg);
        scoreText.toFront();   // 分数始终显示在最上层
    }

    private void showMsg(String text) {
        msgLabel.setText(text);
        msgLabel.setVisible(true);
        msgLabel.toFront();
    }

    private void hideMsg() {
        msgLabel.setVisible(false);
    }

    private static Dir opposite(Dir d) {
        return switch (d) {
            case UP    -> Dir.DOWN;
            case DOWN  -> Dir.UP;
            case LEFT  -> Dir.RIGHT;
            case RIGHT -> Dir.LEFT;
        };
    }
}
