package com.example.demo.event;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * 事件页面：
 *   背景 = 事件页面背景图
 *   左侧 = 当前事件专属插图
 *   右侧 = 事件名称 + 事件描述 + 事件选项
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
     * 插图显示尺寸（保持比例缩放）。原图素材多为 600×600，
     * 超过 600 相当于放大原图，会开始变糊，想再大就换个更大的思路：调窗口或换高清素材。
     */
    private static final double IMAGE_DISPLAY_SIZE = 600;

    public EventView(EventDef ev, Consumer<EventDef.Option> onChoose) {

        // =========================================================
        // 1. 背景
        // =========================================================

        Image bgImage = loadImage(BG_NAME);

        if (bgImage != null) {

            ImageView bg = new ImageView(bgImage);

            bg.setPreserveRatio(false);
            bg.fitWidthProperty().bind(widthProperty());
            bg.fitHeightProperty().bind(heightProperty());

            getChildren().add(bg);

            // 压暗背景，让前面的文字更加清楚
            Pane dim = new Pane();
            dim.setStyle(
                    "-fx-background-color: rgba(2, 6, 23, 0.35);"
            );

            getChildren().add(dim);

        } else {

            // 没有背景图时使用渐变色
            Pane fallback = new Pane();

            fallback.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #0c4a6e, #0f172a);"
            );

            getChildren().add(fallback);

            Label tip = new Label(
                    "事件背景占位 —— 可放入 event_bg.png 换成专属背景图"
            );

            tip.setTextFill(Color.rgb(125, 211, 252, 0.55));
            tip.setFont(Font.font(14));

            StackPane.setAlignment(tip, Pos.BOTTOM_LEFT);
            StackPane.setMargin(
                    tip,
                    new Insets(0, 0, 14, 20)
            );

            getChildren().add(tip);
        }


        // =========================================================
        // 2. 创建整个“图片 + 右侧面板”的主体
        // =========================================================

        HBox content = new HBox(40);

        content.setAlignment(Pos.TOP_CENTER);

        StackPane.setAlignment(
                content,
                Pos.TOP_CENTER
        );

        StackPane.setMargin(
                content,
                new Insets(60, 46, 0, 46)
        );


        // =========================================================
        // 3. 左侧：事件插图
        // =========================================================

        StackPane imageArea = new StackPane();

        imageArea.setAlignment(Pos.CENTER);

        // 给左侧图片区域一个比较大的宽度
        imageArea.setPrefWidth(900);
        imageArea.setMinWidth(700);

        // 高度跟随整页窗口（减去上下留白），让插图大致垂直居中
        imageArea.prefHeightProperty().bind(heightProperty().subtract(120));

        Image source = loadImage(ev.imageName);

        if (source != null) {

            // 生成“中心清晰、四周平滑淡出”的版本（真正的 Alpha 渐变，不是遮罩）
            Image fadedImage = createFadedImage(source);

            // 图片
            ImageView eventImageView = new ImageView(fadedImage);

            eventImageView.setFitWidth(IMAGE_DISPLAY_SIZE);
            eventImageView.setFitHeight(IMAGE_DISPLAY_SIZE);
            eventImageView.setPreserveRatio(true);

            imageArea.getChildren().add(eventImageView);

        } else {

            // 如果图片没找到，显示提示
            Label imageTip = new Label(
                    "未找到事件图片：" + ev.imageName
            );

            imageTip.setTextFill(
                    Color.rgb(148, 163, 184)
            );

            imageTip.setFont(Font.font(14));

            imageArea.getChildren().add(imageTip);
        }


        // =========================================================
        // 4. 右侧：事件名称 + 描述 + 选项
        // =========================================================

        VBox panel = new VBox(12);

        panel.setAlignment(Pos.TOP_LEFT);

        panel.setMinWidth(560);
        panel.setPrefWidth(560);

        panel.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.94);"
                        + "-fx-background-radius: 16;"
                        + "-fx-padding: 22 26 18 26;"
        );


        // ---------- 事件标题 ----------

        Label title = new Label(ev.name);

        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(28));
        title.setStyle("-fx-font-weight: bold;");


        // ---------- 事件描述 ----------

        Label desc = new Label(ev.desc);

        desc.setTextFill(
                Color.rgb(226, 232, 240)
        );

        desc.setFont(Font.font(15));

        desc.setWrapText(true);

        desc.setMaxWidth(520);


        // ---------- 事件选项 ----------

        VBox options = new VBox(10);

        for (EventDef.Option o : ev.options) {

            options.getChildren().add(
                    buildOption(o, onChoose)
            );
        }


        // 把标题、描述、选项放进右侧面板
        panel.getChildren().addAll(
                title,
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

        esc.setTextFill(
                Color.rgb(148, 163, 184)
        );

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


    /**
     * 创建一个事件选项按钮
     *
     * 显示：
     *   选项名称
     *   实际效果说明
     */
    private Button buildOption(
            EventDef.Option o,
            Consumer<EventDef.Option> onChoose
    ) {

        // ---------- 选项名称 ----------

        Label label = new Label(o.label);

        label.setTextFill(Color.WHITE);

        label.setFont(Font.font(17));

        label.setStyle(
                "-fx-font-weight: bold;"
        );


        // ---------- 效果说明 ----------

        Label effect = new Label(o.effectDesc);

        effect.setTextFill(
                Color.rgb(203, 213, 225)
        );

        effect.setFont(Font.font(13));

        effect.setWrapText(true);


        // ---------- 文字区域 ----------

        VBox text = new VBox(3);

        text.setAlignment(
                Pos.CENTER_LEFT
        );

        text.getChildren().addAll(
                label,
                effect
        );


        // ---------- 按钮 ----------

        Button btn = new Button();

        btn.setGraphic(text);

        btn.setMaxWidth(520);
        btn.setMinWidth(520);

        btn.setStyle(
                "-fx-background-color: rgba(51, 65, 85, 0.9);"
                        + "-fx-background-radius: 10;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 8 14 8 14;"
        );


        // 鼠标移入
        btn.setOnMouseEntered(e ->
                btn.setStyle(
                        "-fx-background-color: rgba(71, 85, 105, 0.95);"
                                + "-fx-background-radius: 10;"
                                + "-fx-cursor: hand;"
                                + "-fx-padding: 8 14 8 14;"
                )
        );


        // 鼠标移出
        btn.setOnMouseExited(e ->
                btn.setStyle(
                        "-fx-background-color: rgba(51, 65, 85, 0.9);"
                                + "-fx-background-radius: 10;"
                                + "-fx-cursor: hand;"
                                + "-fx-padding: 8 14 8 14;"
                )
        );


        // 点击按钮
        btn.setOnAction(
                e -> onChoose.accept(o)
        );

        return btn;
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