package com.example.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.EnumMap;
import java.util.Map;

/**
 * 局内顶部 UI（HUD）：角色生命 + 牌组一览。
 * 放在每个局内场景的顶部，生命变化后调用 refresh() 刷新。
 */
public class RunHud extends HBox {

    private static final double BAR_WIDTH = 170; // 血条宽度

    private final Player player;

    private final Label hpText;
    private final Region hpFill;
    private final HBox deckBox;

    public RunHud(Player player) {
        this.player = player;

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(22);
        setPadding(new Insets(8, 16, 8, 16));
        setStyle("-fx-background-color: #111827;");
        setMaxHeight(USE_PREF_SIZE);

        // ---------- 左边：角色名 ----------
        Label name = new Label(Player.CHARACTER_NAME);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(20));
        name.setStyle("-fx-font-weight: bold;");

        // ---------- 中间：生命值（数字 + 血条） ----------
        hpText = new Label();
        hpText.setTextFill(Color.WHITE);
        hpText.setFont(Font.font(16));

        // 血条底槽（深色圆角条），HBox 会让填充块靠左、不拉伸
        HBox barBg = new HBox();
        barBg.setPrefSize(BAR_WIDTH, 14);
        barBg.setMaxSize(BAR_WIDTH, 14);
        barBg.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 7;");

        // 血条填充（宽度随当前血量比例变化）
        hpFill = new Pane();
        hpFill.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 7;");
        barBg.getChildren().add(hpFill);

        HBox hpArea = new HBox(8);
        hpArea.setAlignment(Pos.CENTER_LEFT);
        hpArea.getChildren().addAll(hpText, barBg);

        // ---------- 右边：牌组一览 ----------
        Label deckTitle = new Label("牌组 · " + player.deck.size() + " 张");
        deckTitle.setTextFill(Color.rgb(148, 163, 184));
        deckTitle.setFont(Font.font(14));

        deckBox = new HBox(8);
        deckBox.setAlignment(Pos.CENTER_LEFT);

        HBox deckArea = new HBox(8);
        deckArea.setAlignment(Pos.CENTER_LEFT);
        deckArea.getChildren().addAll(deckTitle, deckBox);

        getChildren().addAll(name, hpArea, deckArea);

        refresh();
    }

    /** 按当前血量/牌组刷新显示（血量变了、牌组变了都要调用）。 */
    public void refresh() {
        // 生命数字
        hpText.setText(player.hp() + " / " + player.maxHp);

        // 血条宽度 = 总宽 × 血量比例
        double ratio = (double) player.hp() / player.maxHp;
        hpFill.setPrefWidth(Math.max(0, BAR_WIDTH * ratio));
        // 血量低（<40%）时血条变红，提醒危险
        hpFill.setStyle("-fx-background-color: " + (ratio < 0.4 ? "#ef4444" : "#22c55e")
                + "; -fx-background-radius: 7;");

        // 牌组按种类统计：打击×5 防御×4 痛击×1
        deckBox.getChildren().clear();
        Map<Card.Kind, Integer> counts = new EnumMap<>(Card.Kind.class);
        for (Card c : player.deck) {
            counts.merge(c.kind, 1, Integer::sum);
        }
        for (Card.Kind kind : Card.Kind.values()) {
            int n = counts.getOrDefault(kind, 0);
            if (n == 0) continue;
            Label chip = new Label(kind.label + " ×" + n);
            chip.setTextFill(Color.rgb(226, 232, 240));
            chip.setFont(Font.font(13));
            chip.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10; "
                    + "-fx-padding: 3 10 3 10;");
            deckBox.getChildren().add(chip);
        }
    }
}
