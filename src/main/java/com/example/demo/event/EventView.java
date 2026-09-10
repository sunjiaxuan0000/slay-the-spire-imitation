package com.example.demo.event;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.function.Consumer;

/**
 * 事件页面：
 *   背景 = 该事件的专属背景图（没有就 fallback 到渐变色占位，可后续放图）；
 *   右半部分上方 = 事件名称 + 事件描述；
 *   下面 = 事件选项（每个选项显示“选项名 + 实际效果”）。
 * 点一个选项 → 交给外面结算并回到地图。
 */
public class EventView extends StackPane {

    /** 事件专属背景图：放到 src/main/resources/com/example/demo/event_bg.png 即可生效 */
    private static final String BG_NAME = "event_bg.png";

    public EventView(EventDef ev, Consumer<EventDef.Option> onChoose) {
        // ---------- 背景 ----------
        Image bgImage = loadImage(BG_NAME);
        if (bgImage != null) {
            ImageView bg = new ImageView(bgImage);
            bg.setPreserveRatio(false);
            bg.fitWidthProperty().bind(widthProperty());
            bg.fitHeightProperty().bind(heightProperty());
            getChildren().add(bg);
            // 压暗一点让文字清楚
            Pane dim = new Pane();
            dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.35);");
            getChildren().add(dim);
        } else {
            Pane fallback = new Pane();
            fallback.setStyle("-fx-background-color: linear-gradient(to bottom, #0c4a6e, #0f172a);");
            getChildren().add(fallback);
            Label tip = new Label("事件背景占位 —— 可放入 event_bg.png 换成专属背景图");
            tip.setTextFill(Color.rgb(125, 211, 252, 0.55));
            tip.setFont(Font.font(14));
            StackPane.setAlignment(tip, Pos.BOTTOM_LEFT);
            StackPane.setMargin(tip, new Insets(0, 0, 14, 20));
            getChildren().add(tip);
        }

        // ---------- 右半部分：名称 + 描述 + 选项 ----------
        VBox panel = new VBox(12);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setMinWidth(560);
        panel.setPrefWidth(560);
        panel.setStyle("-fx-background-color: rgba(15, 23, 42, 0.94); -fx-background-radius: 16; "
                + "-fx-padding: 22 26 18 26;");

        Label title = new Label(ev.name);
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(28));
        title.setStyle("-fx-font-weight: bold;");

        Label desc = new Label(ev.desc);
        desc.setTextFill(Color.rgb(226, 232, 240));
        desc.setFont(Font.font(15));
        desc.setWrapText(true);
        desc.setMaxWidth(520);

        VBox options = new VBox(10);
        for (EventDef.Option o : ev.options) {
            options.getChildren().add(buildOption(o, onChoose));
        }

        panel.getChildren().addAll(title, desc, options);

        // 放在“右半部分的上方”
        StackPane.setAlignment(panel, Pos.TOP_RIGHT);
        StackPane.setMargin(panel, new Insets(60, 46, 0, 0));
        getChildren().add(panel);

        // Esc 提示
        Label esc = new Label("Esc 放弃本局回主菜单");
        esc.setTextFill(Color.rgb(148, 163, 184));
        esc.setFont(Font.font(13));
        StackPane.setAlignment(esc, Pos.BOTTOM_LEFT);
        StackPane.setMargin(esc, new Insets(0, 0, 14, 20));
        getChildren().add(esc);
    }

    /** 一个事件选项：选项名 + 实际效果说明 */
    private Button buildOption(EventDef.Option o, Consumer<EventDef.Option> onChoose) {
        Label label = new Label(o.label);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font(17));
        label.setStyle("-fx-font-weight: bold;");

        Label effect = new Label(o.effectDesc);
        effect.setTextFill(Color.rgb(203, 213, 225));
        effect.setFont(Font.font(13));
        effect.setWrapText(true);

        VBox text = new VBox(3);
        text.setAlignment(Pos.CENTER_LEFT);
        text.getChildren().addAll(label, effect);

        Button btn = new Button();
        btn.setGraphic(text);
        btn.setMaxWidth(520);
        btn.setMinWidth(520);
        btn.setStyle("-fx-background-color: rgba(51, 65, 85, 0.9); -fx-background-radius: 10; "
                + "-fx-cursor: hand; -fx-padding: 8 14 8 14;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(71, 85, 105, 0.95); "
                + "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 14 8 14;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: rgba(51, 65, 85, 0.9); "
                + "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 14 8 14;"));
        btn.setOnAction(e -> onChoose.accept(o));
        return btn;
    }

    private static Image loadImage(String name) {
        try {
            var in = EventView.class.getResourceAsStream("/com/example/demo/" + name);
            if (in == null) return null;
            return new Image(in);
        } catch (Exception ex) {
            return null;
        }
    }
}
