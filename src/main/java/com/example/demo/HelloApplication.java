package com.example.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.util.Optional;

public class HelloApplication extends Application {

    private static final double W = SnakeGame.COLS * SnakeGame.TILE;
    private static final double H = SnakeGame.ROWS * SnakeGame.TILE;

    @Override
    public void start(Stage stage) {
        stage.setTitle("我的游戏");
        stage.setScene(buildMenuScene(stage));
        stage.show();

        // 点击窗口右上角关闭按钮(X)时，弹出退出确认（菜单和游戏里都生效）
        stage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("退出确认");
            alert.setHeaderText(null);
            alert.setContentText("确定要退出程序吗？");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                // 点击“取消”或直接关掉对话框 → 阻止窗口关闭
                event.consume();
            }
            // 点击“确定” → 什么都不用做，窗口按默认方式关闭、程序退出
        });
    }

    // ================= 场景切换 =================

    /** 主菜单场景：一启动先看到这里，不直接进游戏。 */
    private Scene buildMenuScene(Stage stage) {
        MainMenu menu = new MainMenu(
                () -> startSnake(stage), // 点“贪吃蛇”→ 切到游戏场景
                () -> stage.close()      // 点“退出游戏”→ 关闭窗口（触发上面的退出确认）
        );
        return new Scene(menu, W, H);
    }

    /** 贪吃蛇场景：点菜单里的“贪吃蛇”后进入。 */
    private void startSnake(Stage stage) {
        SnakeGame game = new SnakeGame();
        Scene gameScene = new Scene(game, W, H);

        // 键盘控制
        gameScene.setOnKeyPressed(e -> {
            KeyCode c = e.getCode();
            switch (c) {
                case UP, W    -> game.setDirection(SnakeGame.Dir.UP);
                case DOWN, S  -> game.setDirection(SnakeGame.Dir.DOWN);
                case LEFT, A  -> game.setDirection(SnakeGame.Dir.LEFT);
                case RIGHT, D -> game.setDirection(SnakeGame.Dir.RIGHT);
                case SPACE    -> game.togglePause();
                case R        -> game.restart();
                case ESCAPE   -> {                     // 按 Esc 回主菜单
                    game.stopGame();                   // 先停掉游戏循环
                    stage.setScene(buildMenuScene(stage));
                }
                default       -> { }
            }
        });

        stage.setScene(gameScene);
    }
}
