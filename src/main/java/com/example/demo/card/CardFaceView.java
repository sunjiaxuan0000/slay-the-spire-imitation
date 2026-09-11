package com.example.demo.card;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多层贴图卡面（由旧版 CardFaceView 重构而来，适配新分包 com.example.demo.card）。
 *
 * 层次（自底向上）：
 *   1) 卡面美术：art/&lt;Kind 名&gt;.png —— 垫在最底下，靠基础图的透明区域"漏"出来（没图就跳过这层）
 *   2) 基础卡面：ability.png(能力) / attack.png(攻击) / skill.png(技能)
 *   3) 稀有度飘带：normal.png(weight=3 白) / rare.png(2 蓝) / gold.png(1 金)，类型文字写在飘带中间
 *   4) 卡名：卡上方
 *   5) 描述：卡下方
 *   6) 能量徽章 power.png：左上角，上面写能量
 *
 * ★ 想调某层贴图位置：改"布局参数"区常量即可（坐标系 = 卡面 150×210）。
 * ★ 想调卡面美术的位置：改"卡面美术"区的 ART_BOX_* / ART_DX / ART_DY 常量，
 *   或者直接跑 {@link ArtCalibrator} 拖滑块，拖好了把打印出来的常量粘回来。
 */
public class CardFaceView {

    // ================= 布局参数（卡面 150 × 210 坐标系）=================
    public static final double CARD_W = 150;
    public static final double CARD_H = 210;

    private static final double EMBLEM_CX = CARD_W / 2; // 稀有度飘带中心 x（水平居中）
    private static final double EMBLEM_CY = 104;       // 稀有度飘带中心 y（卡中部）
    private static final double EMBLEM_W = 108;        // 飘带宽
    private static final double EMBLEM_H = 64;         // 飘带高

    private static final double BADGE_X = 6;           // power 徽章左上角 x（卡左上角）
    private static final double BADGE_Y = 4;
    private static final double BADGE_SIZE = 42;       // 徽章边长

    // ---- 卡名 / 描述底板（浅色压底，把文字从花哨的底图里托出来）----
    /**
     * 基础卡面纵向放大到的高度（横向仍是 CARD_W，不做缩放）。
     *
     * 原图 756×1126，实际非透明内容只到 y≈1048（约 93%），底部约 7% 是透明留白。
     * 按 150×210 铺满时粉色底边只到 card y≈194，而描述底板底端在 y=202，
     * 于是底板会落进那块透明留白里 —— 看起来就是"描述跑出了卡面边框"。
     *
     * 纵向放大到 230（≈1.095×）并把内容顶边对齐卡面顶端，粉色底边正好铺到 y=210，
     * 描述底板就被完整包在边框内。
     *
     * 横向**故意不动**：左右两侧的粉色边框在源图里只有约 18px 宽（≈3.6 卡面像素），
     * 一旦等比放大会被裁边切掉，所以这里只拉纵向。
     */
    private static final double BASE_FACE_H = 230;
    private static final double BASE_FACE_Y = -4;      // 顶边略微上提，盖住源图顶部 1~2% 的透明留白
    private static final double NAME_PLATE_X = 50;     // 特意让开左上角的能量徽章
    private static final double NAME_PLATE_Y = 5;
    private static final double NAME_PLATE_W = 94;
    private static final double NAME_PLATE_H = 30;
    private static final double DESC_PLATE_X = 8;
    private static final double DESC_PLATE_Y = 134;
    private static final double DESC_PLATE_W = 134;
    private static final double DESC_PLATE_H = 68;
    private static final double PLATE_ARC = 10;        // 底板圆角
    private static final double DESC_PAD = 6;          // 描述文字内边距

    // ---- 配色：暖米白底板 + 深咖文字，柔和不刺眼，对比也够 ----
    private static final Color PLATE_FILL = Color.rgb(252, 245, 230, 0.95);
    private static final Color PLATE_STROKE = Color.rgb(120, 98, 70, 0.45);
    private static final Color NAME_TEXT = Color.rgb(74, 54, 36);
    private static final Color DESC_TEXT = Color.rgb(88, 72, 56);
    private static final double NAME_FONT = 17;
    private static final double DESC_FONT = 12.5;

    /**
     * 文字的十六进制色，配合 inline {@code -fx-text-fill} 使用。
     *
     * ★ 为什么要用 inline style 而不是只靠 {@code setTextFill()}：
     * JavaFX 里 CSS 的优先级高于代码 setter，modena 样式表对 .label 有
     * {@code -fx-text-fill: -fx-text-background-color}，而 {@code -fx-text-background-color}
     * 是由祖先的 {@code -fx-background} 推导的"背景阶梯色"。
     * 牌堆浏览页 / 牌组页的 ScrollPane 设了 {@code -fx-background: #0f172a}（深色），
     * 于是阶梯色推成白色，把我们 setTextFill 的深咖色整个盖掉 ——
     * 卡面一到那个弹层里就变成白字。
     * inline style 优先级最高，可以钉死颜色，不再受祖先背景影响。
     */
    private static final String NAME_TEXT_CSS = "#4a3624";
    private static final String DESC_TEXT_CSS = "#584838";
    private static final String ON_ART_TEXT_CSS = "#ffffff"; // 飘带/徽章上的字，压在深色图案上
    // ======================================================

    // ================= 卡面美术（填进基础图中间的"透明框"）=================
    // 想换位置/大小：改下面这组常量，或直接跑 card/ArtCalibrator 拖滑块调。

    /**
     * 美术图目录（classpath 绝对路径）。
     * <p>文件名 = {@link Card.Kind} 的枚举名 + {@code .png}，例如
     * {@code STRIKE.png} / {@code DEFEND.png} / {@code HAMMER.png}。
     * <p>放这里：{@code demo/src/main/resources/com/example/demo/art/}
     * <p>缺图的卡会自动跳过美术层，卡面回到原来的样子，不会报错。
     */
    private static final String ART_DIR = "/com/example/demo/art/";

    /** 总开关：false 就完全不画美术层（回到纯基础卡面） */
    private static final boolean ART_ENABLED = true;

    /**
     * 每种基础图里"透明框"的位置与尺寸（卡面 150×210 坐标，{@code {x, y, w, h}}）。
     *
     * <p>这四个数是<b>实测</b>出来的，直接对应三张基础图中间的镂空区域：
     * <ul>
     *   <li>attack 框 126×80、skill 框 130×83（都是圆角矩形）</li>
     *   <li>ability 框 96×83（<b>是正圆</b>）</li>
     * </ul>
     * 注意：美术层垫在基础图<b>下面</b>，所以形状不用自己裁 ——
     * 基础图哪里透明，美术就从哪里露出来，上面那三种形状（含那个正圆）天然就对了。
     *
     * ★ 要整体微调请用 {@link #ART_DX} / {@link #ART_DY} / {@link #ART_SCALE}，
     *   不要改这里的基准值 —— 这样三种类型能保持一致的相对关系。
     */
    private static final double[] ART_BOX_ATTACK = { 13, 15, 126, 80 };
    private static final double[] ART_BOX_SKILL  = { 13, 15, 130, 83 };
    private static final double[] ART_BOX_POWER  = { 27, 15,  96, 83 };

    /** 全局微调：整体平移（右/下为正）与整体缩放（以框中心为基准） */
    private static final double ART_DX = 0;
    private static final double ART_DY = 0;
    private static final double ART_SCALE = 1.0;

    /**
     * 美术怎么塞进框：
     * <ul>
     *   <li>{@code COVER}：等比放大到盖满整个框（推荐）。多出来的部分压在基础图底下，看不见。</li>
     *   <li>{@code CONTAIN}：等比缩小到完整装进框，四边可能留空（留空处会透出游戏背景）。</li>
     *   <li>{@code STRETCH}：直接拉伸到框的尺寸，图与框比例不一致时会变形。</li>
     * </ul>
     */
    private static final ArtFit ART_FIT = ArtFit.COVER;

    /** 美术在框内的填充方式 */
    public enum ArtFit { CONTAIN, COVER, STRETCH }

    /** 已加载成功的美术图 */
    private static final Map<Card.Kind, Image> ART_CACHE = new ConcurrentHashMap<>();
    /** 确认过没有文件、不用反复找的卡种 */
    private static final Set<Card.Kind> ART_MISSING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 校准工具专用：置 true 时正式美术层不画，
     * 好让 {@link ArtCalibrator} 用它自己那套滑块参数来预览。
     * 游戏里永远是 false，别动它。
     */
    static boolean artLayerSuppressed = false;
    // =======================================================================

    private static Image img(String name) {
        // 用“/com/example/demo/icons/…”绝对路径：本类已移到 card 子包，
        // 相对路径会错误地找 card/icons/…
        return imgAt("/com/example/demo/icons/" + name);
    }

    /** 按 classpath 绝对路径加载图片；找不到返回 null（不抛异常） */
    private static Image imgAt(String absPath) {
        var in = CardFaceView.class.getResourceAsStream(absPath);
        if (in == null) return null;
        Image im = new Image(in);
        return im.isError() ? null : im;
    }

    /**
     * 取某张卡的美术图：{@code art/<Kind 名>.png}。
     * <p>没放图就返回 {@code null}（结果会被缓存，不会每次去翻 classpath）。
     * <p>校准工具 {@link ArtCalibrator} 也走这个方法。
     */
    public static Image artOf(Card.Kind kind) {
        if (kind == null || ART_MISSING.contains(kind)) return null;
        Image cached = ART_CACHE.get(kind);
        if (cached != null) return cached;
        Image loaded = imgAt(ART_DIR + kind.name() + ".png");
        if (loaded == null) {
            ART_MISSING.add(kind);
            return null;
        }
        ART_CACHE.put(kind, loaded);
        return loaded;
    }

    /** 该卡种的美术图"应该"放在哪（给校准工具和排错用，只是个字符串） */
    public static String artPathFor(Card.Kind kind) {
        return "src/main/resources" + ART_DIR + (kind == null ? "<Kind>" : kind.name()) + ".png";
    }

    /** 某类型的默认框 {@code {x, y, w, h}}（副本，改了不影响常量） */
    public static double[] defaultArtBox(Card.Type type) {
        return artBox(type);
    }

    private static double[] artBox(Card.Type type) {
        double[] base = switch (type) {
            case ATTACK -> ART_BOX_ATTACK;
            case SKILL  -> ART_BOX_SKILL;
            case POWER  -> ART_BOX_POWER;
            case STATUS -> ART_BOX_SKILL; // 状态牌借技能面
        };
        return base.clone();
    }

    /**
     * 卡面美术层：把 {@code art/<Kind 名>.png} 按框摆好。
     *
     * <p><b>这一层垫在基础图下面</b>（是 {@code root} 的第一个子节点），
     * 所以基础图中间那块透明区域就成了天然遮罩 ——
     * 攻击/技能的圆角矩形、能力牌那个正圆，全都是基础图自己的形状，
     * 这里不需要做任何裁剪。
     *
     * <p>没图 / 关掉了就返回 {@code null}，调用方跳过这一层。
     */
    private static ImageView artLayer(Card c) {
        if (!ART_ENABLED || artLayerSuppressed) return null;
        Image art = artOf(c.kind);
        if (art == null) return null;

        double[] box = artBox(c.kind.type);

        // 以框中心为基准做整体缩放 + 平移
        double bw = box[2] * ART_SCALE;
        double bh = box[3] * ART_SCALE;
        if (bw <= 0 || bh <= 0) return null;
        double cx = box[0] + box[2] / 2 + ART_DX;
        double cy = box[1] + box[3] / 2 + ART_DY;

        double iw = art.getWidth(), ih = art.getHeight();
        if (iw <= 0 || ih <= 0) return null;

        double dw, dh;
        switch (ART_FIT) {
            case STRETCH -> {
                dw = bw;
                dh = bh;
            }
            case CONTAIN -> {
                double s = Math.min(bw / iw, bh / ih);
                dw = iw * s;
                dh = ih * s;
            }
            default -> { // COVER：等比放大到盖满整框，多出来的部分压在基础图底下看不见
                double s = Math.max(bw / iw, bh / ih);
                dw = iw * s;
                dh = ih * s;
            }
        }

        ImageView view = new ImageView(art);
        view.setFitWidth(dw);
        view.setFitHeight(dh);
        view.setPreserveRatio(false);       // 宽高已经算好了，别让它再改
        view.setLayoutX(cx - dw / 2);       // 以框中心居中（COVER 时多余部分均分到两边）
        view.setLayoutY(cy - dh / 2);
        view.setMouseTransparent(true);     // 美术只是好看，别抢卡片的点击
        return view;
    }

    private static final Image FACE_ATTACK = img("attack.png");
    private static final Image FACE_SKILL = img("skill.png");
    private static final Image FACE_ABILITY = img("ability.png");
    private static final Image RIBBON_COMMON = img("normal.png"); // 3 白
    private static final Image RIBBON_RARE = img("rare.png");     // 2 蓝
    private static final Image RIBBON_GOLD = img("gold.png");     // 1 金
    private static final Image POWER = img("power.png");

    /** 生成一张分层贴图卡面（150×210） */
    public static Pane build(Card c) {
        return buildAt(c, CARD_W);
    }

    /** 按目标宽度等比生成卡面并外套固定尺寸容器（堆/组/列表浏览用），高度 = 宽 × 1.4 */
    public static StackPane buildAt(Card c, double width) {
        return buildAt(c, width, c.kind.desc);
    }

    /**
     * 按目标宽度等比生成卡面，描述文字使用 {@code descOverride}（战斗中用于显示含力量/易伤加成的实际数值）。
     */
    public static StackPane buildAt(Card c, double width, String descOverride) {
        Pane face = buildRaw(c, descOverride);
        double scale = width / CARD_W;
        face.setScaleX(scale);
        face.setScaleY(scale);
        StackPane wrap = new StackPane(face);
        wrap.setPrefSize(width, width * 1.4);
        wrap.setMaxSize(width, width * 1.4);
        wrap.setAlignment(javafx.geometry.Pos.CENTER);
        return wrap;
    }

    /** 浅色圆角底板：垫在卡名/描述下面，把文字从花哨的底图和飘带里托出来 */
    private static Rectangle plate(double x, double y, double w, double h) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setArcWidth(PLATE_ARC);
        r.setArcHeight(PLATE_ARC);
        r.setFill(PLATE_FILL);
        r.setStroke(PLATE_STROKE);
        r.setStrokeWidth(1.2);
        r.setMouseTransparent(true); // 底板只负责好看，别抢卡片的点击
        return r;
    }

    private static Pane buildRaw(Card c) {
        return buildRaw(c, c.kind.desc);
    }

    private static Pane buildRaw(Card c, String descOverride) {
        Pane root = new Pane();
        root.setPrefSize(CARD_W, CARD_H);
        root.setMaxSize(CARD_W, CARD_H);
        // 基础卡面纵向放大后底部会溢出卡面，用矩形裁剪把可视区限定在 150×210 内
        root.setClip(new Rectangle(0, 0, CARD_W, CARD_H));

        // ---------- 0) 卡面美术：垫在最底下，靠基础图的透明区域"漏"出来（没放图就跳过）----------
        ImageView art = artLayer(c);
        if (art != null) root.getChildren().add(art);

        // ---------- 1) 基础卡面（横向铺满 150，纵向放大到 BASE_FACE_H）----------
        Image base = switch (c.kind.type) {
            case ATTACK -> FACE_ATTACK;
            case SKILL  -> FACE_SKILL;
            case POWER  -> FACE_ABILITY;
            case STATUS -> FACE_SKILL; // 状态牌暂无专用面，先借技能面
        };
        ImageView baseView = new ImageView(base);
        double baseW = BASE_FACE_H * (base.getWidth() / base.getHeight()); // 按源图等比，不拉伸
        baseView.setFitWidth(baseW);
        baseView.setFitHeight(BASE_FACE_H);
        baseView.setLayoutX((CARD_W - baseW) / 2); // 居中：只裁掉源图左右那圈透明留白
        baseView.setLayoutY(BASE_FACE_Y);
        root.getChildren().add(baseView);

        // ---------- 2) 稀有度飘带（中间）+ 类型文字 ----------
        Image ribbon = switch (c.kind.weight) {
            case 3 -> RIBBON_COMMON; // 白卡
            case 2 -> RIBBON_RARE;   // 蓝卡
            case 1 -> RIBBON_GOLD;   // 金卡
            default -> RIBBON_COMMON;
        };
        ImageView ribbonView = new ImageView(ribbon);
        ribbonView.setFitWidth(EMBLEM_W);
        ribbonView.setFitHeight(EMBLEM_H);
        ribbonView.setLayoutX(EMBLEM_CX - EMBLEM_W / 2);
        ribbonView.setLayoutY(EMBLEM_CY - EMBLEM_H / 2.5);
        root.getChildren().add(ribbonView);

        String typeText = switch (c.kind.type) {
            case ATTACK -> "攻击";
            case SKILL  -> "技能";
            case POWER  -> "能力";
            case STATUS -> "状态";
        };
        Label typeLabel = new Label(typeText);
        typeLabel.setTextFill(Color.WHITE);
        typeLabel.setFont(Font.font(16));
        // 颜色走 inline style：见 NAME_TEXT_CSS 上面的说明（CSS 会盖过 setTextFill）
        typeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ON_ART_TEXT_CSS + ";");
        typeLabel.setLayoutX(EMBLEM_CX - 20);
        typeLabel.setLayoutY(EMBLEM_CY - 20);
        typeLabel.setPrefWidth(40);
        typeLabel.setAlignment(javafx.geometry.Pos.CENTER);
        root.getChildren().add(typeLabel);

        // ---------- 3) 卡名（上方）：浅色底板 + 深咖粗体 ----------
        root.getChildren().add(plate(NAME_PLATE_X, NAME_PLATE_Y, NAME_PLATE_W, NAME_PLATE_H));

        Label name = new Label(c.kind.label);
        name.setTextFill(NAME_TEXT);
        name.setFont(Font.font(NAME_FONT));
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: " + NAME_TEXT_CSS + ";");
        name.setLayoutX(NAME_PLATE_X);
        name.setLayoutY(NAME_PLATE_Y);
        name.setPrefWidth(NAME_PLATE_W);
        name.setPrefHeight(NAME_PLATE_H);
        name.setAlignment(Pos.CENTER);
        root.getChildren().add(name);

        // ---------- 4) 描述（下方）：浅色底板 + 深灰褐正文，整块垂直居中 ----------
        root.getChildren().add(plate(DESC_PLATE_X, DESC_PLATE_Y, DESC_PLATE_W, DESC_PLATE_H));

        Label desc = new Label(descOverride);
        desc.setTextFill(DESC_TEXT);
        desc.setFont(Font.font(DESC_FONT));
        desc.setStyle("-fx-text-fill: " + DESC_TEXT_CSS + ";");
        desc.setLayoutX(DESC_PLATE_X);
        desc.setLayoutY(DESC_PLATE_Y);
        desc.setPrefWidth(DESC_PLATE_W);
        desc.setPrefHeight(DESC_PLATE_H);
        desc.setWrapText(true);
        desc.setAlignment(Pos.CENTER);
        desc.setPadding(new Insets(DESC_PAD));
        root.getChildren().add(desc);

        // ---------- 5) power 徽章（左上角）+ 能量数字 ----------
        ImageView badge = new ImageView(POWER);
        badge.setFitWidth(BADGE_SIZE);
        badge.setFitHeight(BADGE_SIZE);
        badge.setLayoutX(BADGE_X);
        badge.setLayoutY(BADGE_Y);
        root.getChildren().add(badge);

        Label cost = new Label(c.cost >= 0 ? String.valueOf(c.cost) : "X");
        cost.setTextFill(Color.WHITE);
        cost.setFont(Font.font(20));
        cost.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ON_ART_TEXT_CSS + ";");
        cost.setEffect(new DropShadow(2, Color.BLACK));
        cost.setLayoutX(BADGE_X);
        cost.setLayoutY(BADGE_Y + (BADGE_SIZE - 24) / 2);
        cost.setPrefWidth(BADGE_SIZE);
        cost.setAlignment(javafx.geometry.Pos.CENTER);
        root.getChildren().add(cost);

        return root;
    }
}
