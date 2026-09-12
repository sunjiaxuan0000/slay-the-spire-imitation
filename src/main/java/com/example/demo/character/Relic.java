package com.example.demo.character;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * 遗物：记录名字和描述，并持有全部遗物池数据。
 * 效果逻辑在 RelicFun 中实现。
 */
public  class Relic {
    public final String name;
    public final String desc;
    /** 遗物图标的资源路径（相对 /com/example/demo/）；为 null 时用名字首字的文字图标 */
    public final String imagePath;

    public Relic(String name, String desc) {
        this(name, desc, null);
    }

    public Relic(String name, String desc, String imagePath) {
        this.name = name;
        this.desc = desc;
        this.imagePath = imagePath;
    }

    /** 铁甲战猪固有初始遗物：燃烧之血（开局即拥有，不进入任何抽取池） */
    public static final Relic BURNING_BLOOD =
            new Relic("燃烧之血", "每场战斗结束后恢复 6 点生命", "relic/BurningBlood.png");

    // ================= 遗物池数据 =================

    private static final List<Relic> STARTER_RELICS = List.of(
            new Relic("青铜怀表", "获得时升级牌组里的一张牌", "relic/huaibiao.png"),
            new Relic("请假条", "接下来的三场战斗怪物血量变为 1", "relic/qingjia.png"),
            new Relic("保温杯", "最大生命值增加 8 点", "relic/cup.png"),
            new Relic("破镜", "删除当前卡组里的一张牌", "relic/jingzi.png"),
            // 混沌：本局地图变异 —— 效果见 RelicFun.onRelicObtained（置 player.chaos）
            //       和三连卡牌奖励（RoomView.giveChaosCardRewards）
            new Relic("混沌",
                    "拾取时获得三次卡牌奖励，非固定节点全变事件图标，进入随机遭遇怪物/精英/事件/火堆/宝箱",
                    "relic/chaos.jpg")
    );

    private static final List<Relic> ELITE_RELICS = List.of(
            // —— 普通遗物（各 7%）——
            new Relic("红头骨", "当生命值 ≤ 50% 时，获得额外 3 点力量", "relic/RedSkull.png"),
            new Relic("猫", "每场战斗开始时获得 10 点格挡", "relic/mao.png"),
            new Relic("孙子兵法", "若一回合内未打出攻击牌，下回合开始时获得 1 点额外能量", "relic/ArtofWar.png"),
            new Relic("发条靴", "造成 ≤ 5 的未被格挡伤害时，提升为 8", "relic/TheBoot.png"),
            new Relic("赤牛", "每场战斗第一次攻击造成 8 点额外伤害", "relic/chiniu.png"),
            new Relic("金刚杵", "每场战斗开始时获得 1 点力量", "relic/Vajra.png"),
            new Relic("小血瓶", "每场战斗开始时恢复 2 点生命", "relic/Blood_vial.png"),
            new Relic("草莓", "最大生命值提升 7 点", "relic/Strawberry.png"),
            new Relic("百年积木", "每场战斗第一次失去生命值时抽 3 张牌", "relic/CentennialPuzzle.png"),
            new Relic("奥利哈钢", "回合结束时若无格挡，获得 6 点格挡", "relic/Orichalcum.png"),
            new Relic("古茶具套装", "篝火休息后下一场战斗开始时获得 2 点额外能量", "relic/Tea_set.png"),
            // —— 罕见遗物（各 5%）——
            new Relic("荔枝", "最大生命值提升 13 点", "relic/Pear.png"),
            new Relic("精致折扇", "一回合打出 3 张攻击牌时获得 4 点格挡", "relic/OrnamentalFan.png"),
            new Relic("开信刀", "一回合打出 3 张技能牌时对敌人造成 5 点伤害", "relic/LetterOpener.png"),
            new Relic("带骨肉", "战斗结束时若生命值 < 50%，恢复 12 点生命", "relic/Meat.png"),
            // —— 稀有遗物（3%）——
            new Relic("鸟面翁", "每打出一张技能牌恢复 2 点生命", "relic/Bird_urn.png")
    );

    /** 与 ELITE_RELICS 一一对应的权重（百分比） */
    private static final List<Integer> ELITE_WEIGHTS = List.of(
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,   // 普通 ×11
            5, 5, 5, 5,                           // 罕见 ×4
            3                                      // 稀有 ×1
    );

    private static final List<Relic> EVENT_RELICS = List.of(
            new Relic("英雄宝典", "每场战斗开始时增加一张不消耗能量的能力牌", "relic/Enchiridion.png"),
            new Relic("老牧师", "每场战斗结束后最大生命值增加 1", "relic/mushi.jpg"),
            new Relic("忘情牛肉面", "每场战斗开始时获得 3 点力量，仅第一回合有效", "relic/mian.png"),
            new Relic("taffy", "每回合结束时生命值高于 50% 额外获得 5 点格挡", "relic/taffy.jpg"),
            new Relic("牛来", "每回合开始时对敌人造成 3 点伤害", "relic/niulai.png"),
            new Relic("奶龙", "第二回合开始时获得 12 点格挡", "relic/nailong.png")
    );

    /** Boss 遗物池：击败 Boss 后获得 */
    private static final List<Relic> BOSS_RELICS = List.of(
            new Relic("奴隶贩子颈环", "在 Boss 战和精英战中，每回合开始时获得 1 点额外能量", "relic/Collar.png"),
            new Relic("空鸟笼", "获取时可以移除牌组里的两张牌", "relic/Cage.png"),
            new Relic("召唤铃铛", "连续三次精英遗物机会（每个都可拾取或丢弃），但牌组里会多一张「伤口」", "relic/Bell.png")
    );

    public static List<Relic> starterRelics() {
        return new ArrayList<>(STARTER_RELICS);
    }

    public static List<Relic> eliteRelics() {
        return new ArrayList<>(ELITE_RELICS);
    }

    public static List<Integer> eliteWeights() {
        return new ArrayList<>(ELITE_WEIGHTS);
    }

    public static List<Relic> eventRelics() {
        return new ArrayList<>(EVENT_RELICS);
    }

    public static List<Relic> bossRelics() {
        return new ArrayList<>(BOSS_RELICS);
    }

    /** 全部遗物（起点 + 精英 + 事件 + Boss），开发者模式面板用它列出所有可加/可删的遗物 */
    public static List<Relic> allRelics() {
        List<Relic> all = new ArrayList<>();
        all.addAll(STARTER_RELICS);
        all.addAll(ELITE_RELICS);
        all.addAll(EVENT_RELICS);
        all.addAll(BOSS_RELICS);
        return all;
    }


    /** 加载遗物图标图片；未配置路径或加载失败时返回 null（调用方退回文字图标） */
    public Image loadImage() {
        if (imagePath == null) return null;
        var in = Relic.class.getResourceAsStream("/com/example/demo/" + imagePath);
        if (in == null) return null;
        Image img = new Image(in);
        return img.isError() ? null : img;
    }

    /**
     * 只画遗物图标本身：有图就用图，没图退回"名字首字"的紫色方块。
     *
     * <p>不含点击事件、不含 Tooltip，方便各处按自己的尺寸复用：
     * HUD 遗物条 30px、详情页 96px、开发者面板 26px …
     */
    public StackPane buildIconGraphic(double size) {
        StackPane icon = new StackPane();
        icon.setPrefSize(size, size);
        icon.setMaxSize(size, size);
        double radius = size * 0.22;   // 圆角跟着尺寸走，大小图标看起来才一致

        Image img = loadImage();
        if (img != null) {
            icon.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); "
                    + "-fx-background-radius: " + radius + ";");
            ImageView iv = new ImageView(img);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            icon.getChildren().add(iv);
        } else {
            icon.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: " + radius + ";");
            Label g = new Label(name.substring(0, 1));
            g.setTextFill(Color.WHITE);
            g.setFont(Font.font(size * 0.46));
            g.setStyle("-fx-font-weight: bold;");
            icon.getChildren().add(g);
        }
        return icon;
    }

    public VBox buildPage(Runnable onClose) {
        StackPane icon = buildIconGraphic(96);

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
        StackPane icon = buildIconGraphic(30);
        icon.setCursor(javafx.scene.Cursor.HAND);
        Tooltip.install(icon, new Tooltip(name + "\n" + desc));
        icon.setOnMouseClicked(e -> onClick.accept(this));
        return icon;
    }
}