package com.example.demo.view;

import com.example.demo.character.Player;
import com.example.demo.character.Relic;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

/**
 * “起点房间”：在进入地图冒险前和 NPC 对话、选择初始遗物。
 *
 * 构图仿照战斗界面但去掉所有战斗 UI：
 *   左边 = 角色立绘（无血量/状态）
 *   右边 = NPC + 对话框
 *   中间 = 三个初始遗物选项（选一个）
 * 只有选中遗物后，“离开房间”按钮才会出现。
 */
public class RoomView extends StackPane {

    private static final String CARD_STYLE = "-fx-background-color: rgba(30, 41, 59, 0.95); "
            + "-fx-background-radius: 12; -fx-border-color: transparent; -fx-border-width: 2; "
            + "-fx-border-radius: 12; -fx-padding: 10 14 10 14;";
    private static final String CARD_STYLE_CHOSEN = "-fx-background-color: rgba(30, 41, 59, 0.95); "
            + "-fx-background-radius: 12; -fx-border-color: #fbbf24; -fx-border-width: 2; "
            + "-fx-border-radius: 12; -fx-padding: 10 14 10 14;";

    private final Player player;
    private final List<Relic> options;
    private final Runnable onLeave;

    private boolean chosen = false;
    private final Button leaveBtn = new Button("离开房间");
    private final Label hint = new Label("选择一个初始遗物后即可离开");

    public RoomView(Player player, List<Relic> options,
                    String npcName, String npcDialog, Runnable onLeave) {
        this.player = player;
        this.options = options;
        this.onLeave = onLeave;

        setStyle("-fx-background-color: linear-gradient(to bottom, #131a22, #1c2430);");

        // ---- 三列：角色 | 遗物选项 | NPC ----
        HBox columns = new HBox(40);
        columns.setAlignment(Pos.CENTER);
        columns.setPadding(new Insets(20, 40, 30, 40));

        // 1) 左边：角色立绘（只有立绘，没有血量/状态等战斗 UI）
        VBox left = new VBox(8);
        left.setAlignment(Pos.CENTER);
        left.setPrefWidth(280);
        StackPane portrait = portrait("战",
                "radial-gradient(center 35% 30%, radius 100%, #b45309, #451a03);");
        portrait.setPrefSize(230, 230);
        portrait.setMaxSize(230, 230);
        left.getChildren().add(portrait);

        // 2) 中间：三个初始遗物选项
        VBox middle = new VBox(14);
        middle.setAlignment(Pos.CENTER);
        middle.setPrefWidth(340);

        Label title = new Label("选择一个初始遗物");
        title.setTextFill(Color.rgb(226, 232, 240));
        title.setFont(Font.font(19));
        middle.getChildren().add(title);

        for (Relic r : options) {
            middle.getChildren().add(buildRelicOption(r));
        }

        hint.setTextFill(Color.rgb(148, 163, 184));
        hint.setFont(Font.font(13));
        middle.getChildren().add(hint);

        // 3) 右边：NPC + 对话框
        VBox right = new VBox(10);
        right.setAlignment(Pos.CENTER);
        right.setPrefWidth(360);

        // 对话框
        Label dialog = new Label("“" + npcDialog + "”");
        dialog.setTextFill(Color.rgb(241, 245, 249));
        dialog.setFont(Font.font(15));
        dialog.setWrapText(true);
        dialog.setMaxWidth(340);
        StackPane bubble = new StackPane(dialog);
        bubble.setPadding(new Insets(14));
        bubble.setStyle("-fx-background-color: rgba(51, 65, 85, 0.9); -fx-background-radius: 14;");

        // NPC 立绘
        StackPane npcPortrait = portrait("猪",
                "radial-gradient(center 35% 30%, radius 100%, #a3a3a3, #3f3f46);");
        npcPortrait.setPrefSize(160, 160);
        npcPortrait.setMaxSize(160, 160);

        Label npcNameLabel = new Label(npcName);
        npcNameLabel.setTextFill(Color.rgb(212, 212, 216));
        npcNameLabel.setFont(Font.font(18));
        npcNameLabel.setStyle("-fx-font-weight: bold;");

        right.getChildren().addAll(bubble, npcPortrait, npcNameLabel);
        columns.getChildren().addAll(left, middle, right);
        getChildren().add(columns);

        // ---- “离开房间”：选完遗物才出现 ----
        leaveBtn.setFont(Font.font(17));
        leaveBtn.setPrefSize(170, 46);
        leaveBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; "
                + "-fx-background-radius: 12; -fx-cursor: hand;");
        leaveBtn.setVisible(false);
        leaveBtn.setOnAction(e -> onLeave.run());
        StackPane.setAlignment(leaveBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(leaveBtn, new Insets(0, 34, 28, 0));
        getChildren().add(leaveBtn);

        // 左上角提示（Esc = 放弃本局回主菜单）
        Label esc = new Label("Esc 放弃本局回主菜单");
        esc.setTextFill(Color.rgb(100, 116, 139));
        esc.setFont(Font.font(13));
        StackPane.setAlignment(esc, Pos.TOP_LEFT);
        StackPane.setMargin(esc, new Insets(14, 0, 0, 18));
        getChildren().add(esc);
    }

    /** 单个遗物选项按钮 */
    private Button buildRelicOption(Relic r) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(320);

        Label name = new Label(r.name);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(17));
        name.setStyle("-fx-font-weight: bold;");

        Label desc = new Label(r.desc);
        desc.setTextFill(Color.rgb(203, 213, 225));
        desc.setFont(Font.font(13));
        desc.setWrapText(true);

        card.getChildren().addAll(name, desc);

        Button btn = new Button();
        btn.setGraphic(card);
        btn.setStyle(CARD_STYLE);
        btn.setCursor(Cursor.HAND);
        btn.setMaxWidth(340);
        btn.setOnAction(e -> choose(r, btn));
        return btn;
    }

    /** 选中一个遗物：加金框、禁用其它选项、出现离开按钮 */
    private void choose(Relic r, Button btn) {
        if (chosen) return;
        chosen = true;

        btn.setStyle(CARD_STYLE_CHOSEN);
        player.addRelic(r); // 遗物记进玩家状态
        hint.setText("已获得：" + r.name + " —— 可以离开了");
        hint.setTextFill(Color.rgb(251, 191, 36));
        leaveBtn.setVisible(true);
    }

    /** 圆形立绘占位（以后换成 ImageView） */
    private static StackPane portrait(String glyph, String gradient) {
        StackPane p = new StackPane();
        p.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 115;");
        Label g = new Label(glyph);
        g.setTextFill(Color.rgb(255, 255, 255, 0.85));
        g.setFont(Font.font(96));
        p.getChildren().add(g);
        return p;
    }
}
