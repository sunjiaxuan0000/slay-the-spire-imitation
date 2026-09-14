package com.example.demo.view;

import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;
import com.example.demo.character.Player;
import com.example.demo.character.Relic;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 商店界面
 *
 * 职责：
 * 1. 展示卡牌和遗物商品
 * 2. 展示当前金币
 * 3. 处理购买按钮的点击回调
 * 4. 根据外部业务逻辑同步“已售”和金币状态
 *
 * 注意：
 * - 扣金币不在这里完成
 * - 卡牌加入牌组不在这里完成
 * - 遗物加入玩家背包不在这里完成
 *
 * HelloApplication 负责真正的购买业务逻辑。
 */
public class ShopView extends BorderPane {

    // =========================================================
    // 样式常量
    // =========================================================

    /**
     * 整体背景
     */
    private static final String BG_STYLE =
            "-fx-background-color: radial-gradient(center 50% 12%, radius 95%, #23304f, #0b1020);";

    /**
     * 金币显示
     */
    private static final String GOLD_PILL_STYLE =
            "-fx-background-color: rgba(251, 191, 36, 0.12);" +
                    "-fx-border-color: rgba(251, 191, 36, 0.55);" +
                    "-fx-border-width: 1.5;" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-radius: 18;" +
                    "-fx-padding: 6 18 6 18;" +
                    "-fx-font-size: 20px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #fbbf24;";

    /**
     * 商品区域
     */
    private static final String GOODS_PANEL_STYLE =
            "-fx-background-color: rgba(15, 23, 42, 0.55);" +
                    "-fx-border-color: rgba(255, 255, 255, 0.06);" +
                    "-fx-border-width: 1;" +
                    "-fx-background-radius: 14;" +
                    "-fx-border-radius: 14;";

    /**
     * 商品卡片：正常状态
     */
    private static final String ITEM_NORMAL_STYLE =
            "-fx-background-color: rgba(255, 255, 255, 0.08);" +
                    "-fx-border-color: rgba(255, 255, 255, 0.10);" +
                    "-fx-border-width: 1;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-radius: 12;";

    /**
     * 商品卡片：鼠标悬停
     */
    private static final String ITEM_HOVER_STYLE =
            "-fx-background-color: rgba(255, 255, 255, 0.14);" +
                    "-fx-border-color: rgba(251, 191, 36, 0.55);" +
                    "-fx-border-width: 1;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-radius: 12;";

    /**
     * 小节标题
     */
    private static final String SECTION_TITLE_STYLE =
            "-fx-font-size: 20px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #e2e8f0;";

    /**
     * 商品名字
     */
    private static final String NAME_STYLE =
            "-fx-font-size: 16px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #f1f5f9;";

    /**
     * 商品描述
     */
    private static final String DESC_STYLE =
            "-fx-font-size: 13px;" +
                    "-fx-text-fill: #cbd5e1;";

    /**
     * 价格
     */
    private static final String PRICE_STYLE =
            "-fx-font-size: 15px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #fbbf24;";

    /**
     * 购买按钮：可以买
     */
    private static final String BTN_BUY_STYLE =
            "-fx-background-color: #16a34a;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 9;" +
                    "-fx-padding: 6 18 6 18;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;";

    /**
     * 购买按钮：买不起
     */
    private static final String BTN_CANT_AFFORD_STYLE =
            "-fx-background-color: #334155;" +
                    "-fx-text-fill: #94a3b8;" +
                    "-fx-background-radius: 9;" +
                    "-fx-padding: 6 18 6 18;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;";

    /**
     * 购买按钮：已经卖出
     */
    private static final String BTN_SOLD_STYLE =
            "-fx-background-color: #1f2937;" +
                    "-fx-text-fill: #64748b;" +
                    "-fx-background-radius: 9;" +
                    "-fx-padding: 6 18 6 18;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;";

    /**
     * 离开商店按钮
     */
    private static final String BTN_LEAVE_STYLE =
            "-fx-background-color: #1e293b;" +
                    "-fx-text-fill: #e2e8f0;" +
                    "-fx-border-color: #475569;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-radius: 10;" +
                    "-fx-padding: 9 30 9 30;" +
                    "-fx-font-size: 16px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;";

    /**
     * 底部操作区
     */
    private static final String FOOTER_STYLE =
            "-fx-border-color: rgba(255, 255, 255, 0.08) transparent transparent transparent;" +
                    "-fx-border-width: 1 0 0 0;";

    /**
     * 遗物 / 服务卡片的统一最小高度。
     *
     * <p>给的是「下限」不是「固定值」：描述短的时候三张卡一样高（按钮对齐），
     * 描述长（比如猪疾速那种三四行的）时卡片自己长高，文字不会被裁。</p>
     */
    public static final double RELIC_ITEM_MIN_H = 300;

    // =========================================================
    // 成员变量
    // =========================================================

    private final Player player;

    /**
     * 点击购买卡牌以后交给外部处理
     */
    private final Consumer<Card> onBuyCard;

    /**
     * 点击购买遗物以后交给外部处理
     */
    private final Consumer<Relic> onBuyRelic;

    /**
     * 点击「移除卡牌」服务以后交给外部处理（为 null 表示这家店不提供该服务）
     */
    private final Runnable onRemoveCard;

    /**
     * 点击离开商店以后交给外部处理
     */
    private final Runnable onLeave;

    /**
     * 金币显示
     */
    private final Label goldLabel = new Label();

    /**
     * 上一次显示的金币
     *
     * -1 表示刚打开商店
     */
    private int lastGold = -1;


    // =========================================================
    // 商品 → UI 控件的映射
    // =========================================================

    /**
     * 卡牌 → 购买按钮
     */
    private final Map<Card, Button> cardBuyButtons = new HashMap<>();

    /**
     * 遗物 → 购买按钮
     */
    private final Map<Relic, Button> relicBuyButtons = new HashMap<>();

    /**
     * 卡牌 → 商品外框
     */
    private final Map<Card, VBox> cardItems = new HashMap<>();

    /**
     * 遗物 → 商品外框
     */
    private final Map<Relic, VBox> relicItems = new HashMap<>();


    // =========================================================
    // 已售商品
    // =========================================================

    /**
     * 已经购买的卡牌
     */
    private final Set<Card> soldCards = new HashSet<>();

    /**
     * 已经购买的遗物
     */
    private final Set<Relic> soldRelics = new HashSet<>();

    /**
     * 「移除卡牌」服务的按钮（一家店只能买一次）
     */
    private Button removeCardButton;

    /**
     * 「移除卡牌」服务是否已用过
     */
    private boolean removeUsed = false;


    // =========================================================
    // 本店的实际标价（基准价 ±30 浮动）
    // =========================================================

    /**
     * 卡牌 → 本店标价。
     *
     * <p>价格在开店时<b>一次算好</b>存这里，之后只读不改 —— 不能每次
     * {@link #refreshGold()} 都现摇一次，否则买一张牌金币一刷新，
     * 其余商品的价格跟着跳，玩家永远不知道自己要点下去多少钱。</p>
     */
    private final Map<Card, Integer> cardPrices = new HashMap<>();

    /** 遗物 → 本店标价（同上，开店时定死） */
    private final Map<Relic, Integer> relicPrices = new HashMap<>();

    /**
     * 价格浮动幅度：基准价 {@code ±PRICE_SWING}。
     * 想改成「只便宜不贵」就把下面那个 {@code nextInt} 的区间换成 {@code [0, 2*PRICE_SWING]}。
     */
    public static final int PRICE_SWING = 30;


    // =========================================================
    // 构造方法
    // =========================================================

    public ShopView(
            Player player,
            List<Card> cardGoods,
            List<Relic> relicGoods,
            Consumer<Card> onBuyCard,
            Consumer<Relic> onBuyRelic,
            Runnable onLeave) {
        this(player, cardGoods, relicGoods, onBuyCard, onBuyRelic, null, onLeave);
    }

    /**
     * @param onRemoveCard 「移除卡牌」服务的回调；传 null 则这家店不显示该服务
     */
    public ShopView(
            Player player,
            List<Card> cardGoods,
            List<Relic> relicGoods,
            Consumer<Card> onBuyCard,
            Consumer<Relic> onBuyRelic,
            Runnable onRemoveCard,
            Runnable onLeave) {
        this(player, cardGoods, relicGoods, onBuyCard, onBuyRelic, onRemoveCard, onLeave,
                new Random().nextLong());
    }

    /**
     * @param priceSeed 价格浮动的随机种子。传「节点种子派生值」就能做到
     *                  「同一个商店节点重进，标价一模一样」，刷不出更便宜的金卡。
     */
    public ShopView(
            Player player,
            List<Card> cardGoods,
            List<Relic> relicGoods,
            Consumer<Card> onBuyCard,
            Consumer<Relic> onBuyRelic,
            Runnable onRemoveCard,
            Runnable onLeave,
            long priceSeed) {

        this.player = player;
        this.onBuyCard = onBuyCard;
        this.onBuyRelic = onBuyRelic;
        this.onRemoveCard = onRemoveCard;
        this.onLeave = onLeave;

        // ⚠ 必须先算价，再建 UI —— buildCardItem / buildRelicItem / refreshGold 都读这两张表
        Random rnd = new Random(priceSeed);
        for (Card c : cardGoods) {
            cardPrices.put(c, fluctuate(cardPrice(c), rnd));
        }
        for (Relic r : relicGoods) {
            relicPrices.put(r, fluctuate(relicPrice(), rnd));
        }

        setPadding(new Insets(20));

        setStyle(BG_STYLE);

        // 顶部
        setTop(buildHeader());

        // 商品区域（可滚动：商品总高度经常超过窗口，不滚的话底部「离开商店」会被挤没）
        setCenter(buildScrollableGoods(cardGoods, relicGoods));

        // 底部
        setBottom(buildFooter());

        // 第一次刷新金币和按钮状态
        refreshGold();
    }

    /** 基准价 ±{@link #PRICE_SWING}，最低不低于 1 金币 */
    private static int fluctuate(int base, Random rnd) {
        int delta = rnd.nextInt(PRICE_SWING * 2 + 1) - PRICE_SWING;
        return Math.max(1, base + delta);
    }

    /** 本店里这张牌的实际标价（没登记过就退回基准价） */
    public int priceOf(Card card) {
        return cardPrices.getOrDefault(card, cardPrice(card));
    }

    /** 本店里这件遗物的实际标价（没登记过就退回基准价） */
    public int priceOf(Relic relic) {
        return relicPrices.getOrDefault(relic, relicPrice());
    }


    // =========================================================
    // 顶部
    // =========================================================

    /**
     * 构建商店顶部
     */
    private VBox buildHeader() {

        Label title = new Label("商店");

        title.setStyle(
                "-fx-font-size: 34px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #fcd34d;"
        );

        Label subtitle = new Label(
                "欢迎光临 · 用金币换取前路的力量"
        );

        subtitle.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #94a3b8;"
        );

        VBox titleBox = new VBox(2, title, subtitle);

        titleBox.setAlignment(Pos.CENTER_LEFT);


        // 弹性空间
        Region spacer = new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );


        // 金币
        goldLabel.setStyle(GOLD_PILL_STYLE);


        HBox header = new HBox(20);

        header.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().addAll(
                titleBox,
                spacer,
                goldLabel
        );


        VBox box = new VBox(header);

        box.setPadding(
                new Insets(0, 0, 18, 0)
        );

        return box;
    }


    /**
     * 把商品区包一层可滚动容器。
     *
     * <p>「遗物 3 件 + 卡牌 4 张 + 服务 1 项」竖着排下来，高度常常超过窗口（尤其是
     * 上面还顶了一条 HUD 的时候）。不滚的话 {@link BorderPane} 会把 center 压缩，
     * 底部的「离开商店」按钮跟着被挤掉 —— 玩家就出不去了。</p>
     *
     * <p>滚动条隐藏（沿用地图页的做法）：滚轮 / 拖动照样能用，但界面干净。</p>
     */
    private ScrollPane buildScrollableGoods(List<Card> cardGoods, List<Relic> relicGoods) {

        ScrollPane scroll = new ScrollPane(buildGoodsArea(cardGoods, relicGoods));

        scroll.setFitToWidth(true);   // 商品区跟着窗口宽度走，FlowPane 才能正确换行

        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        scroll.setPannable(true);

        scroll.setMinSize(0, 0);      // 内容再高也不许把窗口撑大

        // 背景透明：露出 ShopView 自己的径向渐变，别糊一层灰
        scroll.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;"
        );

        return scroll;
    }


    // =========================================================
    // 商品区域
    // =========================================================

    /**
     * 构建商品区域
     *
     * 第一阶段：
     * - 卡牌直接复用 CardFaceView
     * - 遗物暂时保留美术占位窗口
     */
    private VBox buildGoodsArea(
            List<Card> cardGoods,
            List<Relic> relicGoods) {

        VBox root = new VBox(18);

        root.setPadding(new Insets(20));

        root.setStyle(GOODS_PANEL_STYLE);


        // =====================================================
        // 遗物
        // =====================================================

        VBox relicSection = new VBox(10);

        relicSection.getChildren().add(
                sectionTitle("遗物")
        );


        FlowPane relicPane = new FlowPane();

        relicPane.setHgap(18);
        relicPane.setVgap(18);
        relicPane.setAlignment(Pos.CENTER);


        for (Relic relic : relicGoods) {

            relicPane.getChildren().add(
                    buildRelicItem(relic)
            );
        }


        relicSection.getChildren().add(
                relicPane
        );


        // =====================================================
        // 卡牌
        // =====================================================

        VBox cardSection = new VBox(10);

        cardSection.getChildren().add(
                sectionTitle("卡牌")
        );


        FlowPane cardPane = new FlowPane();

        cardPane.setHgap(18);
        cardPane.setVgap(18);
        cardPane.setAlignment(Pos.CENTER);


        for (Card card : cardGoods) {

            cardPane.getChildren().add(
                    buildCardItem(card)
            );
        }


        cardSection.getChildren().add(
                cardPane
        );


        // =====================================================
        // 服务（移除卡牌）
        // =====================================================

        VBox serviceSection = new VBox(10);

        serviceSection.getChildren().add(
                sectionTitle("服务")
        );


        FlowPane servicePane = new FlowPane();

        servicePane.setHgap(18);
        servicePane.setVgap(18);
        servicePane.setAlignment(Pos.CENTER);


        if (onRemoveCard != null) {

            servicePane.getChildren().add(
                    buildRemoveCardItem()
            );
        }


        serviceSection.getChildren().add(
                servicePane
        );


        // =====================================================
        // 添加到商品区域
        // =====================================================

        if (onRemoveCard != null) {

            root.getChildren().addAll(
                    relicSection,
                    cardSection,
                    serviceSection
            );

        } else {

            root.getChildren().addAll(
                    relicSection,
                    cardSection
            );
        }

        return root;
    }


    // =========================================================
    // 服务项：移除卡牌
    // =========================================================

    /**
     * 「移除卡牌」服务：花钱从牌组里永久删掉一张牌（和原版商店的 removal service 一样）。
     *
     * <p>这里只负责「显示 + 把点击交给外部」—— 真正弹选牌界面、扣钱、删牌都在
     * {@code HelloApplication}，因为那些要碰 Player 和 Scene。</p>
     */
    private VBox buildRemoveCardItem() {

        VBox box = new VBox(8);

        box.setPadding(
                new Insets(12)
        );

        box.setPrefWidth(210);

        // 和遗物卡片同一套高度规则：给下限、不钉死，服务卡不会比旁边矮一截
        box.setMinHeight(RELIC_ITEM_MIN_H);

        box.setStyle(
                ITEM_NORMAL_STYLE
        );


        // 图标区（借遗物的美术框风格，中间放个「移除」字样）
        StackPane art = new StackPane();

        art.setPrefSize(180, 110);
        art.setMinSize(180, 110);
        art.setMaxSize(180, 110);

        art.setStyle(
                "-fx-background-color: rgba(239, 68, 68, 0.14);" +
                        "-fx-border-color: rgba(248, 113, 113, 0.5);" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;"
        );

        Label glyph = new Label("✂");

        glyph.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.75);" +
                        "-fx-font-size: 34px;"
        );

        art.getChildren().add(glyph);


        Label name = new Label("移除卡牌");

        name.setStyle(NAME_STYLE);


        Label desc = new Label("从你的牌组中永久移除一张卡牌");

        desc.setStyle(DESC_STYLE);
        desc.setWrapText(true);
        desc.setMaxWidth(180);
        desc.setMinHeight(Region.USE_PREF_SIZE);


        Label priceLabel = new Label(removeCardPrice() + " 金币");

        priceLabel.setStyle(PRICE_STYLE);


        removeCardButton = new Button("选择");

        removeCardButton.setStyle(BTN_BUY_STYLE);


        removeCardButton.setOnAction(e -> {

            if (onRemoveCard != null) {

                onRemoveCard.run();
            }
        });


        attachHoverEffect(
                box,
                () -> removeUsed
        );


        Region push = new Region();

        VBox.setVgrow(push, Priority.ALWAYS);


        box.getChildren().addAll(
                art,
                name,
                desc,
                push,
                priceLabel,
                removeCardButton
        );


        return box;
    }


    /**
     * 外部在真正完成移除之后调用：按钮变「已使用」，不再响应点击。
     */
    public void markRemoveUsed() {

        removeUsed = true;

        if (removeCardButton == null) return;

        removeCardButton.setText("已使用");

        removeCardButton.setStyle(
                BTN_SOLD_STYLE
        );

        removeCardButton.setDisable(true);
    }


    /**
     * 「移除卡牌」服务的价格。
     */
    public static int removeCardPrice() {

        return 75;
    }


    // =========================================================
    // 小节标题
    // =========================================================

    /**
     * 小节标题 + 分隔线
     */
    private VBox sectionTitle(String text) {

        Label title = new Label(text);

        title.setStyle(
                SECTION_TITLE_STYLE
        );


        Region line = new Region();

        line.setPrefHeight(1);

        line.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.10);"
        );


        return new VBox(
                6,
                title,
                line
        );
    }


    // =========================================================
    // 卡牌商品
    // =========================================================

    /**
     * 构建卡牌商品
     *
     * 核心设计：
     *
     * 商店不自己绘制卡牌。
     *
     * 直接调用：
     *
     * CardFaceView.buildAt(card, 120)
     *
     * 这样商店中的卡牌和战斗中的卡牌使用的是同一个
     * CardFaceView。
     *
     * 以后卡牌同学修改：
     * - 卡牌背景
     * - 卡牌图片
     * - 卡牌文字
     * - 卡牌布局
     *
     * 商店会自动跟着变化。
     */
    private VBox buildCardItem(Card card) {

        VBox box = new VBox(8);

        box.setAlignment(Pos.CENTER);

        box.setPadding(
                new Insets(10)
        );

        box.setPrefWidth(150);

        box.setStyle(
                ITEM_NORMAL_STYLE
        );


        // =====================================================
        // 卡牌美术
        // =====================================================

        /*
         * 直接复用战斗中的卡牌显示。
         *
         * 这里就是我们给“卡牌同学”留下的窗口。
         *
         * 商店只关心：
         *
         * Card → CardFaceView
         *
         * 不关心卡牌具体怎么画。
         */
        Node cardArt = CardFaceView.buildAt(
                card,
                120
        );


        // =====================================================
        // 价格
        // =====================================================

        int price = priceOf(card);

        Label priceLabel = new Label(
                price + " 金币"
        );

        priceLabel.setStyle(
                PRICE_STYLE
        );


        // =====================================================
        // 购买按钮
        // =====================================================

        Button buyButton = new Button("购买");

        buyButton.setStyle(
                BTN_BUY_STYLE
        );


        buyButton.setOnAction(e -> {

            if (onBuyCard != null) {

                /*
                 * 注意：
                 * 这里不扣金币。
                 *
                 * 这里只告诉 HelloApplication：
                 *
                 * “玩家点击了这张卡牌的购买按钮。”
                 */
                onBuyCard.accept(card);
            }
        });


        // =====================================================
        // 保存引用
        // =====================================================

        cardBuyButtons.put(
                card,
                buyButton
        );

        cardItems.put(
                card,
                box
        );


        // =====================================================
        // 鼠标悬停
        // =====================================================

        attachHoverEffect(
                box,
                () -> soldCards.contains(card)
        );


        // =====================================================
        // 添加内容
        // =====================================================

        box.getChildren().addAll(
                cardArt,
                priceLabel,
                buyButton
        );


        return box;
    }


    // =========================================================
    // 遗物商品
    // =========================================================

    /**
     * 构建遗物商品
     *
     * 第一阶段暂时不动遗物美术。
     *
     * buildRelicArt()
     * 就是给负责遗物的同学留下的接口。
     */
    private VBox buildRelicItem(Relic relic) {

        VBox box = new VBox(8);

        box.setPadding(
                new Insets(12)
        );

        box.setPrefWidth(210);

        /*
         * ⚠ 不写 setPrefHeight —— 那会把盒子高度钉死，VBox 只能压缩内部控件来塞进
         * 固定高度，而被压的正是唯一可伸缩的 desc（Label），于是长描述（猪疾速那种）
         * 直接被裁掉一行。
         *
         * 改成「给一个统一的最小高度 + 高度随内容长」：
         *   - minHeight 让三张卡看起来一样高（矮的描述下面留白，由 spacer 顶开）
         *   - 描述真超了就把盒子撑高，文字完整显示
         */
        box.setMinHeight(RELIC_ITEM_MIN_H);

        box.setStyle(
                ITEM_NORMAL_STYLE
        );


        // =====================================================
        // 遗物美术区域
        // =====================================================

        /*
         * 以后遗物同学做好图片之后，
         * 只需要修改 buildRelicArt()。
         *
         * 这里不需要动商店的购买逻辑。
         */
        Node art = buildRelicArt(relic);


        // =====================================================
        // 遗物名称
        // =====================================================

        Label name = new Label(
                relic.name
        );

        name.setStyle(
                NAME_STYLE
        );


        // =====================================================
        // 遗物描述
        // =====================================================

        Label desc = new Label(
                relic.desc
        );

        desc.setStyle(
                DESC_STYLE
        );

        desc.setWrapText(true);

        desc.setMaxWidth(180);

        // 描述本身不许被压缩：宁可把卡片撑高，也不能裁掉一行效果说明
        desc.setMinHeight(Region.USE_PREF_SIZE);


        // =====================================================
        // 价格
        // =====================================================

        int price = priceOf(relic);

        Label priceLabel = new Label(
                price + " 金币"
        );

        priceLabel.setStyle(
                PRICE_STYLE
        );


        // =====================================================
        // 购买按钮
        // =====================================================

        Button buyButton = new Button(
                "购买"
        );

        buyButton.setStyle(
                BTN_BUY_STYLE
        );


        buyButton.setOnAction(e -> {

            if (onBuyRelic != null) {

                onBuyRelic.accept(relic);
            }
        });


        // =====================================================
        // 保存引用
        // =====================================================

        relicBuyButtons.put(
                relic,
                buyButton
        );

        relicItems.put(
                relic,
                box
        );


        // =====================================================
        // 鼠标悬停
        // =====================================================

        attachHoverEffect(
                box,
                () -> soldRelics.contains(relic)
        );


        // =====================================================
        // 添加内容
        // =====================================================

        // 弹性空白：把「价格 + 购买」压到卡片底部，
        // 这样三张卡无论描述几行，按钮都在同一条水平线上
        Region push = new Region();

        VBox.setVgrow(push, Priority.ALWAYS);


        box.getChildren().addAll(
                art,
                name,
                desc,
                push,
                priceLabel,
                buyButton
        );


        return box;
    }


    // =========================================================
    // 底部
    // =========================================================

    /**
     * 商店底部
     */
    private HBox buildFooter() {

        Button leaveButton = new Button(
                "离开商店"
        );

        leaveButton.setStyle(
                BTN_LEAVE_STYLE
        );


        leaveButton.setOnAction(e -> {

            if (onLeave != null) {

                onLeave.run();
            }
        });


        HBox footer = new HBox(
                leaveButton
        );

        footer.setAlignment(
                Pos.CENTER
        );

        footer.setPadding(
                new Insets(16, 0, 0, 0)
        );

        footer.setStyle(
                FOOTER_STYLE
        );


        return footer;
    }


    // =========================================================
    // 刷新金币
    // =========================================================

    /**
     * 刷新金币显示，同时重新计算所有商品是否可以买。
     *
     * HelloApplication 扣完金币以后调用。
     */
    public void refreshGold() {

        int gold = player.gold;


        // =====================================================
        // 更新金币文字
        // =====================================================

        goldLabel.setText(
                "💰 当前金币：" + gold
        );


        // =====================================================
        // 金币变化动画
        // =====================================================

        if (lastGold >= 0 && gold < lastGold) {

            playGoldPulse();
        }

        lastGold = gold;


        // =====================================================
        // 更新卡牌购买按钮
        // =====================================================

        cardBuyButtons.forEach(
                (card, button) ->
                        updateBuyButton(
                                button,
                                priceOf(card),
                                soldCards.contains(card)
                        )
        );


        // =====================================================
        // 更新遗物购买按钮
        // =====================================================

        relicBuyButtons.forEach(
                (relic, button) ->
                        updateBuyButton(
                                button,
                                priceOf(relic),
                                soldRelics.contains(relic)
                        )
        );


        // =====================================================
        // 更新「移除卡牌」按钮
        // =====================================================

        if (removeCardButton != null) {

            updateBuyButton(
                    removeCardButton,
                    removeCardPrice(),
                    removeUsed
            );
        }
    }


    // =========================================================
    // 更新购买按钮
    // =========================================================

    /**
     * 根据价格判断按钮状态。
     */
    private void updateBuyButton(
            Button button,
            int price,
            boolean sold) {

        // 已售商品不再修改
        if (sold) {

            return;
        }


        boolean canAfford =
                player.gold >= price;


        button.setDisable(
                !canAfford
        );


        button.setStyle(
                canAfford
                        ? BTN_BUY_STYLE
                        : BTN_CANT_AFFORD_STYLE
        );
    }


    // =========================================================
    // 金币动画
    // =========================================================

    /**
     * 金币牌轻微放大后恢复。
     */
    private void playGoldPulse() {

        ScaleTransition pulse =
                new ScaleTransition(
                        Duration.millis(180),
                        goldLabel
                );


        pulse.setFromX(1.18);
        pulse.setFromY(1.18);

        pulse.setToX(1.0);
        pulse.setToY(1.0);


        pulse.playFromStart();
    }


    // =========================================================
    // 鼠标悬停
    // =========================================================

    /**
     * 商品卡片鼠标悬停效果。
     */
    private void attachHoverEffect(
            VBox box,
            BooleanSupplier isSold) {

        box.setOnMouseEntered(e -> {

            if (!isSold.getAsBoolean()) {

                box.setStyle(
                        ITEM_HOVER_STYLE
                );
            }
        });


        box.setOnMouseExited(e -> {

            if (!isSold.getAsBoolean()) {

                box.setStyle(
                        ITEM_NORMAL_STYLE
                );
            }
        });
    }


    // =========================================================
    // 卡牌购买成功
    // =========================================================

    /**
     * 卡牌购买成功以后调用。
     *
     * 外部业务逻辑完成：
     *
     * 1. 扣金币
     * 2. 加入牌组
     *
     * 然后调用：
     *
     * shopView.markSold(card);
     */
    public void markSold(Card card) {

        soldCards.add(card);


        Button button =
                cardBuyButtons.get(card);


        if (button != null) {

            button.setText(
                    "已售"
            );

            button.setDisable(
                    true
            );

            button.setStyle(
                    BTN_SOLD_STYLE
            );
        }


        VBox box =
                cardItems.get(card);


        if (box != null) {

            box.setStyle(
                    ITEM_NORMAL_STYLE
            );

            box.setOpacity(
                    0.55
            );
        }
    }


    // =========================================================
    // 遗物购买成功
    // =========================================================

    /**
     * 遗物购买成功以后调用。
     */
    public void markSold(Relic relic) {

        soldRelics.add(relic);


        Button button =
                relicBuyButtons.get(relic);


        if (button != null) {

            button.setText(
                    "已售"
            );

            button.setDisable(
                    true
            );

            button.setStyle(
                    BTN_SOLD_STYLE
            );
        }


        VBox box =
                relicItems.get(relic);


        if (box != null) {

            box.setStyle(
                    ITEM_NORMAL_STYLE
            );

            box.setOpacity(
                    0.55
            );
        }
    }


    // =========================================================
    // 卡牌价格
    // =========================================================

    /**
     * 卡牌价格：按<b>稀有度</b>定，越稀有越贵。
     *
     * <p>{@code Kind.weight} 的取值是 1=金卡 / 2=蓝卡 / 3=白卡（见
     * {@code Card.Kind} 的注释），所以：</p>
     *
     * <pre>
     *   weight = 1（金卡，最稀有）→ 150
     *   weight = 2（蓝卡）        → 100
     *   weight = 3（白卡，最常见）→ 50
     * </pre>
     *
     * <p>⚠ 别把「weight 数值大」当成「更稀有」—— 它越大越普通，
     * 之前把白卡定成 75、蓝卡落 default 定成 50，正好搞反了。</p>
     */
    public static int cardPrice(Card card) {

        return switch (card.kind.weight) {

            case 1 -> 150;   // 金卡

            case 2 -> 100;   // 蓝卡

            default -> 50;   // 白卡
        };
    }


    // =========================================================
    // 遗物价格
    // =========================================================

    /**
     * 遗物价格。
     */
    public static int relicPrice() {

        return 150;
    }


    // =========================================================
    // 遗物美术窗口
    // =========================================================

    /**
     * 遗物美术区域。
     *
     * ========================================================
     * 给遗物同学的接口
     * ========================================================
     *
     * 现在：
     *
     *     [ 遗物美术区域 ]
     *
     * 以后：
     *
     *     [ 遗物图片 ]
     *
     * 如果遗物同学已经有统一的 RelicView / RelicFaceView，
     * 以后只需要修改这个方法。
     *
     * 商店其他代码不用动。
     */
    private Node buildRelicArt(Relic relic) {

        StackPane art =
                new StackPane();


        art.setPrefSize(
                180,
                110
        );

        art.setMinSize(
                180,
                110
        );

        art.setMaxSize(
                180,
                110
        );


        art.setStyle(
                "-fx-background-color: rgba(124, 58, 237, 0.12);" +
                        "-fx-border-color: rgba(167, 139, 250, 0.45);" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;"
        );


        /*
         * 遗物图标统一走 Relic.buildIconGraphic() —— 那是全项目唯一的图标渲染入口
         * （HUD / 详情 / 开发者面板都用它），商店不能自己另画一套，
         * 否则读档后图标会退化成「名字首字」。
         *
         * 没配 imagePath 的遗物它自己会退回紫色圆 + 首字，这里什么都不用管。
         */
        art.getChildren().add(
                relic.buildIconGraphic(84)
        );


        return art;
    }
}