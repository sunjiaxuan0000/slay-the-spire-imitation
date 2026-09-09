package com.example.demo.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 死亡提示页：玩家被怪物击败后弹出，提示本局结束并提供“返回主菜单”按钮。
 */
public class DeathOverlay extends StackPane {

    /**
     * @param enemyName 击败玩家的怪物名称
     * @param onReturn  点击“返回主菜单”时的回调
     */
    public DeathOverlay(String enemyName, Runnable onReturn) {
        buildDeathOverlay(enemyName, onReturn);
        setVisible(false);
    }

    private void buildDeathOverlay(String enemyName, Runnable onReturn) {
        Pane dim = new Pane();
        dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.45);");

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 16; -fx-padding: 24 40 20 40;");

        Label title = new Label("你倒下了…");
        title.setTextFill(Color.rgb(248, 113, 113));
        title.setFont(Font.font(30));
        title.setStyle("-fx-font-weight: bold;");

        Label body = new Label("你被 " + enemyName + " 击败，本局结束");
        body.setTextFill(Color.rgb(226, 232, 240));
        body.setFont(Font.font(16));

        Button back = new Button("返回主菜单");
        back.setFont(Font.font(16));
        back.setPrefSize(170, 42);
        back.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
        back.setOnAction(e -> onReturn.run());

        box.getChildren().addAll(title, body, back);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(box, Pos.CENTER);
        getChildren().addAll(dim, box);
    }
}
