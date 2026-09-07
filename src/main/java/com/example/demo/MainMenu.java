package com.example.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 游戏选择菜单。
 * 窗口打开后第一个看到的界面，点按钮才进入具体游戏。
 */
public class MainMenu extends VBox {

    /**
     * @param onStartGame 点“贪吃蛇”时要执行的代码（由 HelloApplication 传入）
     * @param onExit      点“退出游戏”时要执行的代码（由 HelloApplication 传入）
     */
    public MainMenu(Runnable onStartGame, Runnable onExit) {
        setAlignment(Pos.CENTER);
        setSpacing(18);
        setPrefSize(SnakeGame.COLS * SnakeGame.TILE, SnakeGame.ROWS * SnakeGame.TILE);
        setStyle("-fx-background-color: #0f172a;");

        // 标题
        Label title = new Label("游戏菜单");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(44));
        title.setStyle("-fx-font-weight: bold;");

        // 副标题
        Label subtitle = new Label("选择一个游戏开始");
        subtitle.setTextFill(Color.rgb(148, 163, 184));
        subtitle.setFont(Font.font(18));

        // “开始游戏”按钮
        Button btnSnake = new Button("贪吃蛇");
        btnSnake.setMinWidth(240);
        btnSnake.setMinHeight(56);
        btnSnake.setFont(Font.font(20));
        btnSnake.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; "
                + "-fx-background-radius: 12; -fx-cursor: hand;");
        btnSnake.setOnAction(e -> onStartGame.run());

        // “退出”按钮（会走 HelloApplication 里的退出确认弹窗）
        Button btnExit = new Button("退出游戏");
        btnExit.setMinWidth(240);
        btnExit.setMinHeight(44);
        btnExit.setFont(Font.font(16));
        btnExit.setStyle("-fx-background-color: #475569; -fx-text-fill: white; "
                + "-fx-background-radius: 10; -fx-cursor: hand;");
        btnExit.setOnAction(e -> onExit.run());

        // 以后想做第二个游戏，照 btnSnake 的样子再复制一个按钮、加一个回调即可
        getChildren().addAll(title, subtitle, btnSnake, btnExit);
        setPadding(new Insets(20));
    }
}
