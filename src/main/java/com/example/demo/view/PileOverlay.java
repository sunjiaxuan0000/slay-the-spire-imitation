package com.example.demo.view;

import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 牌堆浏览弹层：点击抽牌堆/弃牌堆图标时弹出，按牌 id 排列展示堆内所有牌。
 * 半透明遮罩铺满画面，点遮罩或关闭按钮退出。
 */
public class PileOverlay extends StackPane {
    private final StackPane overlay = new StackPane();
    private final Label overlayTitle = new Label();
    private final HBox overlayCards = new HBox(8);

    public PileOverlay() {
        buildOverlay();
        getChildren().add(overlay);
        setVisible(false);
    }

    private void buildOverlay() {
        // 半透明遮罩，点它关闭
        Pane dim = new Pane();
        dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.72);");
        dim.setOnMouseClicked(e -> hide());

        // 中间卡片浏览区
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setMaxSize(900, 520);
        box.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 16; -fx-padding: 18;");

        overlayTitle.setTextFill(Color.WHITE);
        overlayTitle.setFont(Font.font(22));
        overlayTitle.setStyle("-fx-font-weight: bold;");

        Label sub = new Label("按牌 id 排列（点遮罩或按关闭按钮退出）");
        sub.setTextFill(Color.rgb(148, 163, 184));
        sub.setFont(Font.font(13));

        ScrollPane scroll = new ScrollPane(overlayCards);
        scroll.setPrefSize(860, 360);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #0f172a; -fx-background-color: #0f172a;");

        Button close = new Button("关闭");
        close.setPrefWidth(120);
        close.setPrefHeight(36);
        close.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-background-radius: 9; -fx-cursor: hand;");
        close.setOnAction(e -> hide());

        box.getChildren().addAll(overlayTitle, sub, scroll, close);
        StackPane.setAlignment(box, Pos.CENTER);
        box.setMaxWidth(900);
        box.setMaxHeight(520);
        overlay.getChildren().addAll(dim, box);
    }

    /** 打开弹层，标题显示堆名+张数，牌按 id 升序排列。 */
    public void showPile(String title, List<Card> pile) {
        overlayTitle.setText(title + "（" + pile.size() + " 张）");
        overlayCards.getChildren().clear();

        // 不按抽取顺序，按牌的 id 排序显示
        List<Card> sorted = new ArrayList<>(pile);
        sorted.sort(Comparator.comparingInt(c -> c.id));

        if (sorted.isEmpty()) {
            Label empty = new Label("（空的）");
            empty.setTextFill(Color.rgb(148, 163, 184));
            empty.setFont(Font.font(16));
            overlayCards.getChildren().add(empty);
        } else {
            for (Card c : sorted) {
                overlayCards.getChildren().add(CardFaceView.buildAt(c, 120)); // 分层贴图卡面
            }
        }
        setVisible(true);
    }

    /** 隐藏弹层。 */
    public void hide() {
        setVisible(false);
    }
}
