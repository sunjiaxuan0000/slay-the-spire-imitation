package com.example.demo.card;

import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 卡牌视觉渲染逻辑（从 BattleView 抽取）。
 * 提供卡牌颜色映射、小卡牌、奖励按钮、卡牌图形等静态工具方法。
 */
public class CardView {

    /** 根据卡牌种类返回对应的十六进制颜色字符串。 */
    public static String cardColor(Card.Kind k) {
        return switch (k) {
            case STRIKE -> "#991b1b";
            case DEFEND -> "#1d4ed8";
            case BASH   -> "#b45309";
            case SWEEP  -> "#4338ca";
            case POMMEL -> "#92400e";
            case SHRUG  -> "#475569";
            case BLEED  -> "#881337";
            case HAMMER -> "#7f1d1d";
            case IMPREGNABLE -> "#1e3a5f";
            case DOUBLE_STRIKE -> "#c2410c";
            case KINDLE -> "#9a3412";
            case LIGHTNING -> "#ca8a04";
            case RAGE -> "#7e22ce";
            case OFFERING -> "#581c87";
        };
    }

    /** 构建一张小卡牌（用于牌堆浏览叠层）。 */
    public static VBox miniCard(Card c) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(88, 132);
        card.setStyle("-fx-background-color: " + cardColor(c.kind) + "; -fx-background-radius: 10;");

        Label id = new Label("#" + c.id);
        id.setTextFill(Color.rgb(255, 255, 255, 0.75));
        id.setFont(Font.font(11));

        Label name = new Label(c.kind.label);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(15));
        name.setStyle("-fx-font-weight: bold;");

        Label value = new Label(c.damage > 0 ? "伤害 " + c.damage : "格挡 " + c.block);
        value.setTextFill(Color.rgb(254, 243, 199));
        value.setFont(Font.font(12));

        card.getChildren().addAll(id, name, value);
        return card;
    }

    /** 构建一个可点击的奖励卡牌按钮，点击时通过 onChoose 回调传出被选中的卡牌。 */
    public static Button buildRewardButton(Card c, Consumer<Card> onChoose) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setPrefSize(150, 205);
        card.setStyle("-fx-background-color: " + cardColor(c.kind) + "; -fx-background-radius: 14;");

        Label cost = new Label("费用 " + c.cost);
        cost.setTextFill(Color.rgb(254, 243, 199));
        cost.setFont(Font.font(13));

        Label name = new Label(c.kind.label);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(19));
        name.setStyle("-fx-font-weight: bold;");

        Label value = new Label(
                c.damage > 0 && c.block > 0 ? "伤害 " + c.damage + "  格挡 " + c.block
                : c.damage > 0 ? "伤害 " + c.damage
                : c.block > 0 ? "格挡 " + c.block + (c.exhaust ? " 消耗" : "")
                : c.kind.desc);
        value.setTextFill(Color.WHITE);
        value.setFont(Font.font(15));

        Label desc = new Label(c.kind.desc);
        desc.setTextFill(Color.rgb(226, 232, 240));
        desc.setFont(Font.font(12));
        desc.setWrapText(true);

        card.getChildren().addAll(cost, name, value, desc);

        Button btn = new Button();
        btn.setGraphic(card);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
        btn.setOnAction(e -> onChoose.accept(c));
        return btn;
    }

    /** 构建卡牌的可视 VBox（不含 Button 包装与事件处理），由调用方自行包装并绑定事件。 */
    public static VBox buildCardGraphic(Card c) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(8));
        card.setPrefSize(116, 152);
        card.setStyle("-fx-background-color: " + cardColor(c.kind) + "; -fx-background-radius: 12;");

        Label cost = new Label(String.valueOf(c.cost));
        cost.setTextFill(Color.WHITE);
        cost.setFont(Font.font(15));
        cost.setStyle("-fx-font-weight: bold;");

        Label name = new Label(c.kind.label);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(17));
        name.setStyle("-fx-font-weight: bold;");

        String valueText;
        if (c.damage > 0 && c.block > 0) {
            valueText = "伤害 " + c.damage + "  格挡 " + c.block;
        } else if (c.damage > 0 && c.hits > 1) {
            valueText = "伤害 " + c.damage + "×" + c.hits;
        } else if (c.damage > 0) {
            valueText = c.kind.desc;
        } else if (c.block > 0) {
            valueText = "格挡 " + c.block;
            if (c.exhaust) valueText += " 消耗";
        } else {
            valueText = c.kind.desc;
        }
        Label value = new Label(valueText);
        value.setTextFill(Color.rgb(254, 243, 199));
        value.setFont(Font.font(13));

        card.getChildren().addAll(cost, name, value);
        return card;
    }
}
