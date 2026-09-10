package com.example.demo.operator;

import com.example.demo.settings.GameSettings;

import javafx.scene.control.ToggleButton;
import javafx.scene.text.Font;

/**
 * 设置页里的「开发者模式」开关。
 * 开发者模式相关的 UI 都放在 operator 包，设置页只要 new 一个塞进去就行。
 *
 * 状态直接读写 {@link GameSettings#isDevMode()}（会自动存盘）。
 */
public class DevModeSwitch extends ToggleButton {

    public DevModeSwitch() {
        setSelected(GameSettings.isDevMode());
        applyStyle();
        setOnAction(e -> {
            GameSettings.setDevMode(isSelected());
            applyStyle();
        });
    }

    /** 开=绿色，关=灰色 */
    private void applyStyle() {
        boolean on = isSelected();
        setText(on ? "已开启" : "已关闭");
        setFont(Font.font(15));
        setPrefSize(110, 38);
        setStyle("-fx-background-color: " + (on ? "#16a34a" : "#475569")
                + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
    }
}
