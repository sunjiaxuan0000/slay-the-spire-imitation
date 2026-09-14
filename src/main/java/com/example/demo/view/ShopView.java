package com.example.demo.view;

import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;
import com.example.demo.character.Player;
import com.example.demo.character.Relic;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 商店页面（只负责展示，不负责真正的购买业务逻辑）：
 *   顶部 = 标题“商店” + 当前金币；
 *   中间 = 卡牌商品区 + 遗物商品区（每个商品显示名称/描述/价格 + “购买”按钮）；
 *   底部 = “离开商店”按钮。
 * 点“购买”只触发回调，扣金币/加牌/加遗物由 HelloApplication 处理；
 * 购买成功后外部调用 markSold() 把商品置灰、refreshGold() 刷新金币显示。
 */
public class ShopView extends BorderPane {

    private final Player player;
    private final Consumer<Card> onBuyCard;
    private final Consumer<Relic> onBuyRelic;
    private final Runnable onLeave;

    private final Label goldLabel = new Label();
    // 记录每个商品对应的“购买”按钮，购买成功后外部 markSold() 用它置灰
    private final Map<Card, Button> cardBuyButtons = new HashMap<>();
    private final Map<Relic, Button> relicBuyButtons = new HashMap<>();

    /**
     * @param player     当前玩家（只读它的金币用于显示）
     * @param cardGoods  本店出售的卡牌
     * @param relicGoods 本店出售的遗物
     * @param onBuyCard  点某张卡牌的“购买”时触发（外部负责扣钱、加牌、markSold）
     * @param onBuyRelic 点某个遗物的“购买”时触发（外部负责扣钱、加遗物、markSold）
     * @param onLeave    点“离开商店”时触发（外部负责回到地图）
     */
    public ShopView(Player player,
                    List<Card> cardGoods,
                    List<Relic> relicGoods,
                    Consumer<Card> onBuyCard,
                    Consumer<Relic> onBuyRelic,
                    Runnable onLeave) {
        this.player = player;
        this.onBuyCard = onBuyCard;
        this.onBuyRelic = onBuyRelic;
        this.onLeave = onLeave;

        setStyle("-fx-background-color: linear-gradient(to bottom, #1e1b4b, #0b1020);");
        setTop(header());
        setCenter(goodsArea(cardGoods, relicGoods));
        setBottom(footer());
        refreshGold();
    }

    // ================= 各区域 =================

    /** 顶部：标题 + 当前金币 */
    private VBox header() {
        Label title = new Label("商 店");
        title.setTextFill(Color.rgb(251, 191, 36));
        title.setFont(Font.font(34));
        title.setStyle("-fx-font-weight: bold;");

        goldLabel.setTextFill(Color.rgb(253, 224, 71));
        goldLabel.setFont(Font.font(18));
        goldLabel.setStyle("-fx-font-weight: bold;");

        VBox header = new VBox(6, title, goldLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(14, 0, 6, 0));
        return header;
    }

    /** 中间：卡牌商品区 + 遗物商品区 */
    private VBox goodsArea(List<Card> cardGoods, List<Relic> relicGoods) {
        HBox cardRow = new HBox(16);
        cardRow.setAlignment(Pos.CENTER);
        cardRow.setPadding(new Insets(6, 14, 10, 14));
        for (Card c : cardGoods) cardRow.getChildren().add(cardItem(c));

        HBox relicRow = new HBox(16);
        relicRow.setAlignment(Pos.CENTER);
        relicRow.setPadding(new Insets(6, 14, 10, 14));
        for (Relic r : relicGoods) relicRow.getChildren().add(relicItem(r));

        VBox box = new VBox(6,
                sectionTitle("卡牌商品"), cardRow,
                sectionTitle("遗物商品"), relicRow);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    /** 单个卡牌商品：卡面 + 名称/描述/价格 + 购买按钮 */
    private VBox cardItem(Card c) {
        Button buy = buyButton("购买");
        buy.setOnAction(e -> onBuyCard.accept(c));
        cardBuyButtons.put(c, buy);

        VBox item = new VBox(4,
                CardFaceView.buildAt(c, 105), // 和奖励弹层同款卡面
                nameLabel(c.kind.label),
                descLabel(c.kind.desc, 112),
                priceLabel(cardPrice(c)),
                buy);
        item.setAlignment(Pos.CENTER);
        item.setPadding(new Insets(8));
        item.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-background-radius: 12;");
        return item;
    }

    /** 单个遗物商品：图标 + 名称/描述/价格 + 购买按钮 */
    private VBox relicItem(Relic r) {
        StackPane icon = new StackPane();
        icon.setPrefSize(72, 72);
        icon.setMaxSize(72, 72);
        icon.setStyle("-fx-background-color: linear-gradient(to bottom right, #a78bfa, #6d28d9); "
                + "-fx-background-radius: 12;");
        Label glyph = new Label(r.name.substring(0, 1));
        glyph.setTextFill(Color.WHITE);
        glyph.setFont(Font.font(30));
        glyph.setStyle("-fx-font-weight: bold;");
        icon.getChildren().add(glyph);
        Tooltip.install(icon, new Tooltip(r.desc));

        Button buy = buyButton("购买");
        buy.setOnAction(e -> onBuyRelic.accept(r));
        relicBuyButtons.put(r, buy);

        VBox item = new VBox(4,
                icon,
                nameLabel(r.name),
                descLabel(r.desc, 130),
                priceLabel(relicPrice()),
                buy);
        item.setAlignment(Pos.CENTER);
        item.setPadding(new Insets(8));
        item.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-background-radius: 12;");
        return item;
    }

    /** 底部：离开商店按钮 */
    private HBox footer() {
        Button leave = new Button("离开商店");
        leave.setFont(Font.font(16));
        leave.setStyle("-fx-base: #334155; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        leave.setOnAction(e -> onLeave.run());

        HBox footer = new HBox(leave);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(8, 0, 16, 0));
        return footer;
    }

    // ================= 给外部调用的方法 =================

    /** 刷新金币显示（外部扣完钱后调用；本视图不自己改金币） */
    public void refreshGold() {
        goldLabel.setText("当前金币：" + player.gold);
    }

    /** 购买成功后由外部调用：把该卡牌商品置为“已售”。 */
    public void markSold(Card c) {
        Button b = cardBuyButtons.get(c);
        if (b != null) {
            b.setText("已售");
            b.setDisable(true);
        }
    }

    /** 购买成功后由外部调用：把该遗物商品置为“已售”。 */
    public void markSold(Relic r) {
        Button b = relicBuyButtons.get(r);
        if (b != null) {
            b.setText("已售");
            b.setDisable(true);
        }
    }

    /** 卡牌售价：按稀有度定（weight 4=白 50 / 3=蓝 75 / 1=金 150）。HelloApplication 扣钱时用同一价格。 */
    public static int cardPrice(Card c) {
        return switch (c.kind.weight) {
            case 3  -> 75;
            case 1  -> 150;
            default -> 50;
        };
    }

    /** 遗物售价：暂时固定。 */
    public static int relicPrice() {
        return 150;
    }

    // ================= 小控件 =================

    private static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.rgb(148, 163, 184));
        l.setFont(Font.font(18));
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private static Label nameLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.WHITE);
        l.setFont(Font.font(14));
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private static Label descLabel(String text, double maxWidth) {
        Label l = new Label(text);
        l.setTextFill(Color.rgb(203, 213, 225));
        l.setFont(Font.font(11));
        l.setWrapText(true);
        l.setMaxWidth(maxWidth);
        l.setTextAlignment(TextAlignment.CENTER);
        return l;
    }

    private static Label priceLabel(int price) {
        Label l = new Label("价格：" + price + " 金币");
        l.setTextFill(Color.rgb(253, 224, 71));
        l.setFont(Font.font(13));
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private static Button buyButton(String text) {
        Button b = new Button(text);
        b.setFont(Font.font(13));
        b.setStyle("-fx-base: #b45309; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        return b;
    }
}
