package com.example.demo.event;
import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.TranslateTransition;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * 事件页面：
 *   背景 = 事件页面背景图
 *   左侧 = 当前事件专属插图（约 55% 宽的「插图舞台」）
 *   右侧 = 石板/暗金风格事件面板（名称 + 分割线 + 描述 + 选项卡）
 *
 * 点一个选项 → 交给外面结算并回到地图。
 */
public class EventView extends StackPane {

    /** 事件页面背景图 */
    private static final String BG_NAME = "event_bg.png";

    /**
     * 插图四周淡出的宽度占图片宽/高的比例（0.22 = 每边 22% 的宽度逐渐变透明）。
     * 觉得淡出范围太大 → 调小（如 0.15）；太小 → 调大（如 0.30）。
     */
    private static final double IMAGE_FADE_FRACTION = 0.22;

    /**
     * 插图显示尺寸（保持比例缩放）。窗口 1600×900、去掉上下留白后高度约 830，
     * 660 在「图片尽量大」和「不挤压右侧面板」之间比较平衡。
     * 原图素材多为 600×600，放大 10% 左右的清晰度损耗可以接受。
     */
    private static final double IMAGE_DISPLAY_SIZE = 660;

    /** 左右比例：右侧面板固定宽，左侧插图区吃掉剩余宽度（约 55% 屏宽） */
    private static final double PANEL_W = 600;
    private static final double CONTENT_H_MARGIN = 44;
    private static final double CONTENT_SPACING = 36;

    /** 选项卡固定尺寸：三个选项同宽同高 */
    private static final double OPTION_W = 540;
    private static final double OPTION_H = 92;
    /**动画展示*/
    private ScaleTransition imageBreathingTransition;
    private TranslateTransition imageFloatTransition;//float//
    // ================= 配色（暗金 / 石板） =================

    private static final Color COL_TITLE      = Color.rgb(231, 213, 165); // 暗金米白（标题）
    private static final Color COL_DESC       = Color.rgb(216, 207, 186); // 暖灰米白（描述）
    private static final Color COL_HINT       = Color.rgb(158, 146, 122); // 暖灰（Esc 提示等）
    private static final Color COL_OPT_MAIN   = Color.rgb(236, 226, 200); // 选项主文字
    private static final Color COL_OPT_SUB    = Color.rgb(190, 178, 152); // 选项说明文字
    private static final Color COL_OPT_MAIN_H = Color.rgb(255, 244, 214); // 选项主文字（悬停）
    private static final Color COL_OPT_SUB_H  = Color.rgb(214, 200, 170); // 选项说明文字（悬停）

    // ================= 选项卡样式（集中管理） =================

    private static final String OPTION_STYLE_BASE =
            "-fx-background-insets: 0;"
                    + "-fx-background-radius: 6;"
                    + "-fx-border-radius: 6;"
                    + "-fx-border-width: 1;"
                    + "-fx-padding: 10 18 10 18;"
                    + "-fx-cursor: hand;";

    private static final String OPTION_BG_NORMAL =
            "-fx-background-color: linear-gradient(to bottom, rgba(54,44,28,0.92), rgba(30,24,16,0.95));";
    private static final String OPTION_BG_HOVER =
            "-fx-background-color: linear-gradient(to bottom, rgba(80,65,40,0.96), rgba(46,37,23,0.97));";
    private static final String OPTION_BORDER_NORMAL =
            "-fx-border-color: rgba(168,134,78,0.5);";
    private static final String OPTION_BORDER_HOVER =
            "-fx-border-color: rgba(224,180,104,0.95);";

    /** 选项卡阴影（普通 / 悬停各一档；静态实例可被多个按钮共用） */
    private static final DropShadow OPTION_SHADOW       = makeShadow(10, 4, 0, 0.45);
    private static final DropShadow OPTION_SHADOW_HOVER = makeShadow(16, 6, 0, 0.55);

    /** 第三轮动态动画预留：事件插图节点（没找到图片时为 null） */
    private ImageView eventImageView;

    public EventView(EventDef ev, Consumer<EventDef.Option> onChoose) {

        // =========================================================
        // 1. 背景（事件自带专属背景 EventDef.bgName 优先，否则退回通用 event_bg.png）
        // =========================================================

        Image bgImage = loadImage(ev.bgName != null ? ev.bgName : BG_NAME);
        if (bgImage != null) {

            ImageView bg = new ImageView(bgImage);

            bg.setPreserveRatio(false);
            bg.fitWidthProperty().bind(widthProperty());
            bg.fitHeightProperty().bind(heightProperty());

            getChildren().add(bg);

            // 压暗背景（暖色调），让前面的文字更加清楚
            Pane dim = new Pane();
            dim.setStyle(
                    "-fx-background-color: rgba(8, 5, 2, 0.42);"
            );

            getChildren().add(dim);

        } else {

            // 没有背景图时使用石板色渐变
            Pane fallback = new Pane();

            fallback.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #241c10, #100d09);"
            );

            getChildren().add(fallback);

            Label tip = new Label(
                    "事件背景占位 —— 可放入 event_bg.png 换成专属背景图"
            );

            tip.setTextFill(Color.rgb(158, 146, 122, 0.55));
            tip.setFont(Font.font(14));

            StackPane.setAlignment(tip, Pos.BOTTOM_LEFT);
            StackPane.setMargin(
                    tip,
                    new Insets(0, 0, 14, 20)
            );

            getChildren().add(tip);
        }

        //动画//

        // =========================================================
        // 2. 创建整个「图片 + 右侧面板」的主体（垂直居中）
        // =========================================================

        HBox content = new HBox(CONTENT_SPACING);

        content.setAlignment(Pos.CENTER);

        StackPane.setAlignment(
                content,
                Pos.CENTER
        );

        StackPane.setMargin(
                content,
                new Insets(0, CONTENT_H_MARGIN, 0, CONTENT_H_MARGIN)
        );


        // =========================================================
        // 3. 左侧：事件插图（占剩余宽度的「插图舞台」，图片两向居中）
        // =========================================================

        StackPane imageArea = new StackPane();

        imageArea.setAlignment(Pos.CENTER);

        // 吃掉右侧面板之外的剩余宽度（1600 宽下约 880 ≈ 55% 屏宽）
        imageArea.setMinWidth(640);
        HBox.setHgrow(imageArea, Priority.ALWAYS);

        // 高度跟随整页窗口（减去上下留白），让插图垂直居中
        imageArea.prefHeightProperty().bind(heightProperty().subtract(72));

        Image source = loadImage(ev.imageName);

        if (source != null) {

            // 生成「中心清晰、四周平滑淡出」的版本（真正的 Alpha 渐变，不是遮罩）
            Image fadedImage = createFadedImage(source);

            // 图片（变量保留为字段，方便第三轮在此 ImageView 上加动态效果）
            eventImageView = new ImageView(fadedImage);

            eventImageView.setFitWidth(IMAGE_DISPLAY_SIZE);
            eventImageView.setFitHeight(IMAGE_DISPLAY_SIZE);
            eventImageView.setPreserveRatio(true);

            imageArea.getChildren().add(eventImageView);
            initImageBreathingTransition(eventImageView);
            initImageFloatTransition(eventImageView);
        } else {

            // 如果图片没找到，显示提示
            Label imageTip = new Label(
                    "未找到事件图片：" + ev.imageName
            );

            imageTip.setTextFill(COL_HINT);

            imageTip.setFont(Font.font(14));

            imageArea.getChildren().add(imageTip);
        }


        // =========================================================
        // 4. 右侧：石板事件面板 = 事件名称 + 分割线 + 描述 + 选项
        // =========================================================

        VBox panel = new VBox(14);

        panel.setAlignment(Pos.TOP_CENTER);

        panel.setMinWidth(PANEL_W);
        panel.setPrefWidth(PANEL_W);
        panel.setMaxWidth(PANEL_W);

        // 暗色石板：上亮下暗的做旧渐变，别再用现代卡片风的半透明蓝灰
        panel.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.rgb(43, 34, 21)),
                        new Stop(0.45, Color.rgb(33, 26, 16)),
                        new Stop(1.0, Color.rgb(21, 17, 13))),
                new CornerRadii(10), Insets.EMPTY)));

        // 双层边框：外圈近黑厚边 + 内圈暗金细线（见 createEventPanelBorder）
        panel.setBorder(createEventPanelBorder());

        // 厚重的落地阴影
        panel.setEffect(createPanelShadow());

        // 面板高度贴内容，不跟左侧插图区一起撑满
        content.setFillHeight(false);


        // ---------- 事件标题 ----------

        Label title = new Label(spacedTitle(ev.name));

        title.setTextFill(COL_TITLE);
        title.setFont(Font.font(null, FontWeight.BOLD, 30));
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        title.setMinHeight(Region.USE_PREF_SIZE);


        title.setPadding(new Insets(2, 0, 4, 0));

        // ---------- 事件描述 ----------

        Label desc = new Label(ev.desc);

        desc.setTextFill(COL_DESC);

        desc.setFont(Font.font(16));

        desc.setLineSpacing(6);
        desc.setWrapText(true);

        desc.setMaxWidth(Double.MAX_VALUE);
        desc.setAlignment(Pos.CENTER_LEFT);
        // ⚠ 描述必须完整显示：VBox 里的 Label 会被按剩余空间压缩高度，
        //    行一多（猪雪峰是三行）就会被裁掉尾巴。钉住 minHeight = 首选高度即可。
        desc.setMinHeight(Region.USE_PREF_SIZE);
        desc.setPadding(new Insets(2, 4, 6, 4));


        // ---------- 事件选项 ----------

        VBox options = new VBox(10);

        options.setAlignment(Pos.CENTER);

        for (EventDef.Option o : ev.options) {

            options.getChildren().add(
                    buildOption(o, onChoose)
            );
        }

        VBox.setMargin(options, new Insets(6, 0, 0, 0));


        // 把标题、描述、选项放进右侧面板
        panel.getChildren().addAll(
                title,
                createDivider(),
                desc,
                options
        );


        // =========================================================
        // 5. 把左边图片 + 右边面板放进 HBox
        // =========================================================

        content.getChildren().addAll(
                imageArea,
                panel
        );

        // 最后把整个 content 放进 EventView
        getChildren().add(content);


        // =========================================================
        // 6. Esc 提示
        // =========================================================

        Label esc = new Label(
                "Esc 放弃本局回主菜单"
        );

        esc.setTextFill(COL_HINT);

        esc.setFont(Font.font(13));

        StackPane.setAlignment(
                esc,
                Pos.BOTTOM_LEFT
        );

        StackPane.setMargin(
                esc,
                new Insets(0, 0, 14, 20)
        );

        getChildren().add(esc);
    }

    /** 一个事件选项卡：选项名（主标题）+ 效果说明（副文字）；三个选项固定同高不参差 */
    private Button buildOption(EventDef.Option o, Consumer<EventDef.Option> onChoose) {
        Label label = new Label(o.label);

        label.setTextFill(COL_OPT_MAIN);

        label.setFont(Font.font(null, FontWeight.BOLD, 17));
        label.setMinHeight(Region.USE_PREF_SIZE);

        Label effect = new Label(o.effectDesc);

        effect.setTextFill(COL_OPT_SUB);

        effect.setFont(Font.font(13));

        effect.setWrapText(true);
        effect.setMaxWidth(OPTION_W - 40);
        effect.setMinHeight(Region.USE_PREF_SIZE);

        VBox text = new VBox(4, label, effect);
        text.setAlignment(Pos.CENTER_LEFT);

        Button btn = new Button();

        btn.setGraphic(text);
        // 固定尺寸：三个选项一样大（猪雪峰的三条说明长短不一，不钉住会高低不齐）
        btn.setPrefSize(OPTION_W, OPTION_H);
        btn.setMinSize(OPTION_W, OPTION_H);
        btn.setMaxSize(OPTION_W, OPTION_H);
        btn.setStyle(createOptionStyle(false));
        btn.setEffect(OPTION_SHADOW);

        // 悬停反馈：石板变亮 + 边框转暗金 + 阴影加深 + 文字提亮（都是即时样式切换，无动画）
        btn.setOnMouseEntered(e -> {
            btn.setStyle(createOptionStyle(true));
            btn.setEffect(OPTION_SHADOW_HOVER);
            label.setTextFill(COL_OPT_MAIN_H);
            effect.setTextFill(COL_OPT_SUB_H);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(createOptionStyle(false));
            btn.setEffect(OPTION_SHADOW);
            label.setTextFill(COL_OPT_MAIN);
            effect.setTextFill(COL_OPT_SUB);
        });
        btn.setOnAction(e -> onChoose.accept(o));
        return btn;
    }


    // ================= 面板 / 选项 / 分割线 的美术件 =================

    /** 选项卡整套内联样式：悬停时换亮底色 + 亮边框，其余（圆角/内距/光标）共用一份 */
    private static String createOptionStyle(boolean hovered) {
        return (hovered ? OPTION_BG_HOVER + OPTION_BORDER_HOVER
                        : OPTION_BG_NORMAL + OPTION_BORDER_NORMAL)
                + OPTION_STYLE_BASE;
    }
    private void initImageBreathingTransition(ImageView imageView) {

        imageBreathingTransition = new ScaleTransition(
                Duration.seconds(3),
                imageView
        );

        imageBreathingTransition.setFromX(1.0);
        imageBreathingTransition.setFromY(1.0);

        imageBreathingTransition.setToX(1.04);
        imageBreathingTransition.setToY(1.04);

        imageBreathingTransition.setCycleCount(Animation.INDEFINITE);
        imageBreathingTransition.setAutoReverse(true);

        imageBreathingTransition.play();
    }
    private void initImageFloatTransition(ImageView imageView) {
        imageFloatTransition = new TranslateTransition(
                Duration.seconds(5),
                imageView
        );
        imageFloatTransition.setFromX(-4);
        imageFloatTransition.setFromY(-4);
        imageFloatTransition.setToX(4);
        imageFloatTransition.setToY(4);
        imageFloatTransition.setCycleCount(Animation.INDEFINITE);
        imageFloatTransition.setAutoReverse(true);
        imageFloatTransition.play();
    }

    /**
     * 事件面板的边框：低调但有质感的三层描边。
     * 外圈近黑厚边压住底色，中圈暗金细线是「金属包边」，
     * 内圈一层极淡的暖色高光做出石板内缘被磨亮的做旧感。
     */
    private static Border createEventPanelBorder() {
        CornerRadii outerR = new CornerRadii(10);
        CornerRadii innerR = new CornerRadii(8);

        return new Border(
                new BorderStroke(Color.rgb(10, 8, 6, 0.9), BorderStrokeStyle.SOLID, outerR,
                        new BorderWidths(3), Insets.EMPTY),
                new BorderStroke(Color.rgb(190, 150, 82, 0.55), BorderStrokeStyle.SOLID, innerR,
                        new BorderWidths(1.4), new Insets(3)),
                new BorderStroke(Color.rgb(255, 236, 190, 0.07), BorderStrokeStyle.SOLID, innerR,
                        new BorderWidths(1), new Insets(6)));
    }

    /** 面板阴影：大范围低偏移的高斯投影，让石板「落地」 */
    private static DropShadow createPanelShadow() {
        return makeShadow(24, 10, 0.1, 0.62);
    }

    /** 生成一个高斯投影（只投黑影，不发光） */
    private static DropShadow makeShadow(double radius, double offsetY, double spread, double opacity) {
        DropShadow shadow = new DropShadow();
        shadow.setBlurType(BlurType.GAUSSIAN);
        shadow.setRadius(radius);
        shadow.setOffsetY(offsetY);
        shadow.setSpread(spread);
        shadow.setColor(Color.rgb(0, 0, 0, opacity));
        return shadow;
    }

    /**
     * 标题下的美术分割线：一条「中间稍亮、两侧逐渐变淡」的暗金线，
     * 正中叠一枚 45° 菱形装饰（setRotate 是静态旋转，不是动画）。
     */
    private static Node createDivider() {
        Region line = new Region();

        line.setPrefSize(280, 3);
        line.setMaxSize(280, 3);
        line.setStyle(
                "-fx-background-color: linear-gradient(to right,"
                        + " rgba(190,150,80,0) 0%, rgba(190,150,80,0.18) 20%,"
                        + " rgba(226,186,110,0.9) 50%,"
                        + " rgba(190,150,80,0.18) 80%, rgba(190,150,80,0) 100%);"
                        + "-fx-background-radius: 2;"
        );

        Region gem = new Region();

        gem.setPrefSize(7, 7);
        gem.setMaxSize(7, 7);
        gem.setRotate(45);
        gem.setStyle(
                "-fx-background-color: #d9b26a;"
                        + "-fx-border-color: rgba(255,238,200,0.85);"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 1;"
        );

        StackPane divider = new StackPane(line, gem);
        divider.setAlignment(Pos.CENTER);
        return divider;
    }

    /**
     * 标题字间留缝：「诅咒祭坛」→「诅 咒 祭 坛」。
     * 含字母/数字的标题原样返回，避免把英文或编号拆散。
     */
    private static String spacedTitle(String name) {
        if (name == null || name.isBlank() || name.matches(".*[a-zA-Z0-9].*")) {
            return name;
        }
        return String.join(" ", name.split(""));
    }


    // ================= 插图 Alpha 渐变 =================

    /**
     * 生成“四周平滑淡出”的图片（真正的 Alpha Mask）：
     * 新图与原图同尺寸，颜色一个字节都不改，
     * 只把每个像素的 Alpha 在原始值上乘一个 0~1 的渐变系数。
     *
     * 系数规则：
     *   ax = smoothstep(距左右边缘最近距离 / 淡出宽度)   → 边缘为 0，淡出带外为 1
     *   ay = smoothstep(距上下边缘最近距离 / 淡出宽度)
     *   最终系数 = ax × ay
     * 两个方向相乘，四角比边中点更早、更彻底地变透明，
     * 等值线是平滑曲线，不会出现四条生硬的直线渐变。
     */
    private static Image createFadedImage(Image source) {

        int w = (int) source.getWidth();
        int h = (int) source.getHeight();

        PixelReader reader = source.getPixelReader();
        if (reader == null || w <= 0 || h <= 0) {
            return source; // 读不到像素就退回原图，不影响显示
        }

        // 一次读出全部像素：BYTE_BGRA = 每像素 4 字节（蓝 绿 红 Alpha，未预乘）
        WritablePixelFormat<ByteBuffer> format = PixelFormat.getByteBgraInstance();
        byte[] pixels = new byte[w * h * 4];
        int stride = w * 4;
        reader.getPixels(0, 0, w, h, format, pixels, 0, stride);

        // 每条边的淡出宽度（像素）
        double marginX = Math.max(1, w * IMAGE_FADE_FRACTION);
        double marginY = Math.max(1, h * IMAGE_FADE_FRACTION);

        for (int y = 0; y < h; y++) {

            // 距上/下边的最近距离 → 归一化 → smoothstep（每行算一次）
            double ay = smooth01(Math.min(y, h - 1 - y) / marginY);

            for (int x = 0; x < w; x++) {

                double ax = smooth01(Math.min(x, w - 1 - x) / marginX);

                // 中心区域（两个方向都满了）直接跳过，保持原图不动
                if (ax >= 1.0 && ay >= 1.0) continue;

                int i = (y * w + x) * 4;
                int alpha = pixels[i + 3] & 0xFF;                    // 原始 Alpha
                pixels[i + 3] = (byte) Math.round(alpha * ax * ay);  // 只乘系数，RGB 不动
            }
        }

        WritableImage faded = new WritableImage(w, h);
        faded.getPixelWriter().setPixels(0, 0, w, h, format, pixels, 0, stride);
        return faded;
    }

    /**
     * smoothstep 平滑插值：把距离比例 t（0~1，超出会被截断）平滑映射到 [0,1]。
     * 两端斜率为 0，比线性渐变柔和，不会在“淡出带”和“中心区”交界处出现硬边。
     */
    private static double smooth01(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3.0 - 2.0 * c);
    }


    /**
     * 从 resources 中读取图片
     */
    private static Image loadImage(String name) {

        try {

            var in = EventView.class.getResourceAsStream(
                    "/com/example/demo/" + name
            );

            if (in == null) {
                return null;
            }

            return new Image(in);

        } catch (Exception ex) {

            return null;
        }
    }
}
