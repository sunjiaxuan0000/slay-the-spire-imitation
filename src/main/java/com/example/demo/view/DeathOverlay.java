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

    /** 副标题（「你被 xxx 击败，本局结束」那一行）；{@link #forEvent} 会改写它 */
    private Label bodyLabel;

    /**
     * 战斗死亡。
     *
     * @param enemyName 击败玩家的怪物名称
     * @param onReturn  点击“返回主菜单”时的回调
     */
    public DeathOverlay(String enemyName, Runnable onReturn) {
        buildDeathOverlay(enemyName, onReturn);
        setVisible(false);
    }

    /**
     * 事件死亡：玩家在事件里把血扣光。
     *
     * <p>版面和战斗死亡<b>完全一样</b>（红色「你倒下了…」+ 副标题 + 「返回主菜单」），
     * 只把副标题换成「你死于 xxx」—— 事件里没有敌人，「你被 xxx 击败」说不通。</p>
     *
     * <p>用静态工厂而不是重载构造，是为了不和 {@code (String, Runnable)} 撞签名。</p>
     *
     * <p>返回的对象已经 {@code setVisible(true)} —— 事件里没有「倒地」动画，
     * 构造出来就是要立刻显示的（战斗那条路是自己等动画播完再 setVisible）。</p>
     *
     * @param eventName 把玩家扣死的事件名（如「诅咒祭坛」）
     */
    public static DeathOverlay forEvent(String eventName, Runnable onReturn) {
        DeathOverlay overlay = new DeathOverlay(eventName, onReturn);
        overlay.bodyLabel.setText("你死于「" + eventName + "」，本局结束");
        overlay.setVisible(true);
        return overlay;
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

        bodyLabel = new Label("你被 " + enemyName + " 击败，本局结束");
        bodyLabel.setTextFill(Color.rgb(226, 232, 240));
        bodyLabel.setFont(Font.font(16));

        Button back = new Button("返回主菜单");
        back.setFont(Font.font(16));
        back.setPrefSize(170, 42);
        back.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
        back.setOnAction(e -> onReturn.run());

        box.getChildren().addAll(title, bodyLabel, back);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(box, Pos.CENTER);
        getChildren().addAll(dim, box);
    }
}
