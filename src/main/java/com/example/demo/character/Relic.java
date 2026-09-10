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

public class Relic {
    public final String name;
    public final String desc;

    public Relic(String name, String desc) {
        this.name = name;
        this.desc = desc;
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