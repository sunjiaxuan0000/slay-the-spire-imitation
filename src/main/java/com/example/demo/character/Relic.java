package com.example.demo.character;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.function.Consumer;

import java.util.ArrayList;
import java.util.List;

/**
 * 遗物：暂时只记录名字和描述。
 * 以后加效果时，在战斗/休息/地图对应的地方检查 player 的 relics 即可。
 */
public  class Relic {
    public final String name;
    public final String desc;

    public Relic(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    /**
     * 全部遗物（起点三选一 / 宝箱随机 / 开发者模式面板都用这一份，避免到处硬编码）。
     * 每调用一次都返回新对象，调用方可以随便改这个列表。
     */
    public static List<Relic> pool() {
        return new ArrayList<>(List.of(
                new Relic("青铜怀表", "战斗开始时获得 2 点格挡"),
                new Relic("请假条", "每回合多抽 1 张牌"),
                new Relic("保温杯", "每场战斗开始时恢复 10 点生命")
        ));
    }

    public VBox buildPage(Runnable onClose) {
        StackPane icon = new StackPane();
        icon.setPrefSize(96, 96);
        icon.setMaxSize(96, 96);
        icon.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 18;");
        Label g = new Label(name.substring(0, 1));
        g.setTextFill(Color.WHITE);
        g.setFont(Font.font(44));
        g.setStyle("-fx-font-weight: bold;");
        icon.getChildren().add(g);

        Label nameLabel = new Label(name);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font(26));
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label descLabel = new Label(desc);
        descLabel.setTextFill(Color.rgb(203, 213, 225));
        descLabel.setFont(Font.font(16));
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(360);

        Button close = new Button("关闭");
        close.setFont(Font.font(14));
        close.setPrefSize(110, 34);
        close.setStyle("-fx-background-color: #475569; -fx-text-fill: white; "
                + "-fx-background-radius: 9; -fx-cursor: hand;");
        close.setOnAction(e -> onClose.run());

        VBox panel = new VBox(12);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 16; -fx-padding: 22 30 16 30;");
        panel.getChildren().addAll(icon, nameLabel, descLabel, close);
        return panel;
    }

    public StackPane buildIcon(Consumer<Relic> onClick) {
        StackPane icon = new StackPane();
        icon.setPrefSize(30, 30);
        icon.setMaxSize(30, 30);
        icon.setCursor(javafx.scene.Cursor.HAND);
        icon.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8;");

        Label g = new Label(name.substring(0, 1));
        g.setTextFill(Color.WHITE);
        g.setFont(Font.font(14));
        g.setStyle("-fx-font-weight: bold;");
        icon.getChildren().add(g);

        Tooltip.install(icon, new Tooltip(name + "\n" + desc));
        icon.setOnMouseClicked(e -> onClick.accept(this));
        return icon;
    }
}