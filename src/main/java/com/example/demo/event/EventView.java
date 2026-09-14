package com.example.demo.event;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.function.Consumer;

/**
 * 事件页面：
 *   背景 = 事件自带的全屏大图（{@link EventDef#bgName}）
 *   右侧 = 事件名称 + 事件描述 + 事件选项
 *
 * <p><b>只有一张大图。</b>早期版本还有个「左侧小插图 + 四周 Alpha 淡出」的机制，
 * 现在全删了 —— 每个事件都像猪雪峰那样，用一整张背景图铺满页面，
 * 文字面板贴在右边，图的中间主体不会被挡住。</p>
 *
 * 点一个选项 → 交给外面结算并回到地图。
 */
public class EventView extends StackPane {

    /** 兜底背景图（事件没配 bgName 时用） */
    private static final String BG_NAME = "event_bg.png";

    /** 选项按钮固定尺寸：三个选项同宽同高 */
    private static final double OPTION_W = 520;
    private static final double OPTION_H = 88;

    /** 文字面板宽度（≥ OPTION_W + 左右 padding，否则选项会撑出面板） */
    private static final double PANEL_W = 580;

    public EventView(EventDef ev, Consumer<EventDef.Option> onChoose) {

        // =========================================================
        // 1. 全屏背景大图（事件自带 bgName 优先，否则退回通用 event_bg.png）
        // =========================================================

        Image bgImage = loadImage(ev.bgName != null ? ev.bgName : BG_NAME);

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
                    "事件背景缺失 —— 请放入 " + (ev.bgName != null ? ev.bgName : BG_NAME)
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
        // 2. 右侧面板：事件名称 + 描述 + 选项
        // =========================================================

        VBox panel = new VBox(12);

        panel.setAlignment(Pos.TOP_LEFT);

        panel.setMinWidth(PANEL_W);
        panel.setPrefWidth(PANEL_W);

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
        title.setMinHeight(Region.USE_PREF_SIZE);


        // ---------- 事件描述 ----------

        Label desc = new Label(ev.desc);

        desc.setTextFill(
                Color.rgb(226, 232, 240)
        );

        desc.setFont(Font.font(15));

        desc.setWrapText(true);

        desc.setMaxWidth(PANEL_W - 52);
        // ⚠ 描述必须完整显示：VBox 里的 Label 会被按剩余空间压缩高度，
        //    行一多（猪雪峰是三行）就会被裁掉尾巴。钉住 minHeight = 首选高度即可。
        desc.setMinHeight(Region.USE_PREF_SIZE);


        // ---------- 事件选项 ----------

        VBox options = new VBox(10);

        for (EventDef.Option o : ev.options) {

            options.getChildren().add(
                    buildOption(o, onChoose)
            );
        }


        // 把标题、描述、选项放进面板
        panel.getChildren().addAll(
                title,
                desc,
                options
        );


        // =========================================================
        // 3. 面板贴右侧、垂直居中（左边留出背景大图的主体）
        // =========================================================

        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(spacer, panel);

        content.setAlignment(Pos.CENTER_RIGHT);

        StackPane.setAlignment(content, Pos.CENTER);

        StackPane.setMargin(
                content,
                new Insets(24, 46, 24, 46)
        );

        getChildren().add(content);


        // =========================================================
        // 4. Esc 提示
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

    /** 一个事件选项：选项名 + 实际效果说明（三个选项固定同高，不因文字长短参差） */
    private Button buildOption(EventDef.Option o, Consumer<EventDef.Option> onChoose) {
        Label label = new Label(o.label);

        label.setTextFill(Color.WHITE);

        label.setFont(Font.font(17));
        label.setStyle("-fx-font-weight: bold;");
        label.setMinHeight(Region.USE_PREF_SIZE);

        Label effect = new Label(o.effectDesc);

        effect.setTextFill(
                Color.rgb(203, 213, 225)
        );

        effect.setFont(Font.font(13));

        effect.setWrapText(true);
        effect.setMaxWidth(OPTION_W - 28);
        effect.setMinHeight(Region.USE_PREF_SIZE);

        VBox text = new VBox(3, label, effect);
        text.setAlignment(Pos.CENTER_LEFT);

        Button btn = new Button();

        btn.setGraphic(text);
        // 固定高度：三个选项一样大（猪雪峰的三条说明长短不一，不钉住会高低不齐）
        btn.setPrefSize(OPTION_W, OPTION_H);
        btn.setMinSize(OPTION_W, OPTION_H);
        btn.setMaxSize(OPTION_W, OPTION_H);
        btn.setStyle("-fx-background-color: rgba(51, 65, 85, 0.9); -fx-background-radius: 10; "
                + "-fx-cursor: hand; -fx-padding: 8 14 8 14;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(71, 85, 105, 0.95); "
                + "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 14 8 14;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: rgba(51, 65, 85, 0.9); "
                + "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 14 8 14;"));
        btn.setOnAction(e -> onChoose.accept(o));
        return btn;
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
