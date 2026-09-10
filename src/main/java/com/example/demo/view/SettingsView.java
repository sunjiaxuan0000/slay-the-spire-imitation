package com.example.demo.view;

import com.example.demo.operator.DevModeSwitch;
import com.example.demo.settings.GameSettings;
import com.example.demo.sound.MusicFx;
import com.example.demo.sound.SoundFx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 设置页面：音乐音量、音效音量、开发者模式开关。
 *
 * 从主菜单的 set.png（设置）按钮进入，点“返回主菜单”或按 Esc 回去。
 * 改动会立刻生效，并自动存到用户目录的配置文件里（下次启动还记得）。
 */
public class SettingsView extends StackPane {

    private final Slider musicSlider;
    private final Slider sfxSlider;
    private final DevModeSwitch devToggle;
    private final Label musicValue = new Label();
    private final Label sfxValue = new Label();

    public SettingsView(Runnable onBack) {
        // ---- 背景：沿用主菜单那张图，压暗一点让面板更清楚 ----
        Image bgImage = new Image(SettingsView.class.getResourceAsStream("/com/example/demo/start_menu.png"));
        setBackground(cover(bgImage));

        Pane dim = new Pane();
        dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.72);");

        // ---- 标题 ----
        Label title = new Label("设置");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(34));
        title.setStyle("-fx-font-weight: bold;");

        Label sub = new Label("改动立刻生效并自动保存");
        sub.setTextFill(Color.rgb(148, 163, 184));
        sub.setFont(Font.font(13));

        // ---- 音乐音量 ----
        musicSlider = new Slider(0, 100, GameSettings.getMusicVolume() * 100);
        musicSlider.setPrefWidth(320);
        musicSlider.valueProperty().addListener((o, a, b) -> {
            MusicFx.setVolume(b.doubleValue() / 100.0); // 正在播的 BGM 实时跟着变
            musicValue.setText(pct(b.doubleValue()));
        });
        musicValue.setText(pct(musicSlider.getValue()));
        musicValue.setTextFill(Color.rgb(253, 224, 71));
        musicValue.setFont(Font.font(15));
        musicValue.setMinWidth(58);

        // ---- 音效音量 ----
        sfxSlider = new Slider(0, 100, GameSettings.getSfxVolume() * 100);
        sfxSlider.setPrefWidth(320);
        sfxSlider.valueProperty().addListener((o, a, b) -> {
            GameSettings.setSfxVolume(b.doubleValue() / 100.0);
            sfxValue.setText(pct(b.doubleValue()));
        });
        // 松手时放一声“点击”，当场试听音量大小
        sfxSlider.setOnMouseReleased(e -> SoundFx.play("click"));
        sfxValue.setText(pct(sfxSlider.getValue()));
        sfxValue.setTextFill(Color.rgb(253, 224, 71));
        sfxValue.setFont(Font.font(15));
        sfxValue.setMinWidth(58);

        // ---- 开发者模式（开关本体在 operator 包里） ----
        devToggle = new DevModeSwitch();

        Label devTitle = new Label("开发者模式");
        devTitle.setTextFill(Color.WHITE);
        devTitle.setFont(Font.font(17));

        Label devHint = new Label("开启后：地图上可以点任意节点直接进入；\n"
                + "HUD 上多一个「开」按钮，可自由修改牌组/手牌、增删遗物。");
        devHint.setTextFill(Color.rgb(148, 163, 184));
        devHint.setFont(Font.font(12));

        VBox devText = new VBox(3, devTitle, devHint);

        HBox devRow = new HBox(14, devText, spacer(), devToggle);
        devRow.setAlignment(Pos.CENTER_LEFT);

        // ---- 返回 ----
        Button back = new Button("返回主菜单");
        back.setFont(Font.font(16));
        back.setPrefSize(180, 44);
        back.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; "
                + "-fx-background-radius: 12; -fx-cursor: hand;");
        back.setOnAction(e -> onBack.run());

        Label tip = new Label("Esc 也可以返回");
        tip.setTextFill(Color.rgb(100, 116, 139));
        tip.setFont(Font.font(12));

        // ---- 组装面板 ----
        VBox panel = new VBox(16);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setPadding(new Insets(26, 34, 24, 34));
        panel.setStyle("-fx-background-color: rgba(30, 41, 59, 0.97); "
                + "-fx-background-radius: 18; -fx-border-color: #475569; "
                + "-fx-border-radius: 18; -fx-border-width: 1;");

        VBox head = new VBox(2, title, sub);
        head.setAlignment(Pos.CENTER);

        StackPane backBox = new StackPane(back);
        StackPane tipBox = new StackPane(tip);
        backBox.setAlignment(Pos.CENTER);
        tipBox.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(
                head,
                divider(),
                sliderRow("音乐音量", musicSlider, musicValue),
                sliderRow("音效音量", sfxSlider, sfxValue),
                divider(),
                devRow,
                divider(),
                backBox,
                tipBox
        );

        setStyle("-fx-background-color: #0b1020;");
        getChildren().addAll(dim, panel);
        StackPane.setAlignment(panel, Pos.CENTER);
    }

    /** 一行：名字 + 滑条 + 百分比 */
    private HBox sliderRow(String name, Slider slider, Label value) {
        Label label = new Label(name);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font(17));
        label.setMinWidth(80);

        HBox row = new HBox(14, label, slider, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, javafx.scene.layout.Priority.ALWAYS);
        return r;
    }

    private Region divider() {
        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxWidth(Double.MAX_VALUE);
        line.setStyle("-fx-background-color: #475569;");
        return line;
    }

    private static String pct(double v) {
        return (int) Math.round(v) + " %";
    }

    /** 背景图等比铺满（cover） */
    private static Background cover(Image image) {
        return new Background(new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1, 1, true, true, false, true)));
    }
}
