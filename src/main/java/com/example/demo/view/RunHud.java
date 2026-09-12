package com.example.demo.view;

import com.example.demo.character.Player;
import com.example.demo.character.Relic;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.function.Consumer;

/**
 * 局内顶部 UI（HUD）：
 *   第一行：角色名 + 生命值(血条) … 右边 → [地图图标][牌组图标(右上角,只显示数量)]
 *   第二行：已拾取遗物的图标（悬停看名字/效果）
 * 点击牌组 / 地图 / 遗物图标 → 触发外层回调，
 * 由外层弹出“和抽牌堆一样”的整页大窗口（半透明遮罩 + 居中大面板）。
 */
public class RunHud extends VBox {

    private static final double BAR_WIDTH = 190; // 血条宽度

    private final Player player;

    private final Label hpText;
    private final Region hpFill;
    private final StackPane deckIcon = new StackPane();
    private final Label deckBadge = new Label();
    private final HBox relicRow = new HBox(10);
    private final HBox topRow = new HBox(18);

    public RunHud(Player player,
                  Consumer<Relic> onRelicClick,
                  Runnable onDeckClick,
                  Runnable onMapClick,
                  boolean showMapIcon) {
        this.player = player;

        // ---------- 第一行 ----------
        HBox row1 = topRow;
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.setPadding(new Insets(8, 18, 8, 18));
        row1.setStyle("-fx-background-color: #111827;");

        Label name = new Label(Player.CHARACTER_NAME);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(20));
        name.setStyle("-fx-font-weight: bold;");

        hpText = new Label();
        hpText.setTextFill(Color.WHITE);
        hpText.setFont(Font.font(16));

        HBox barBg = new HBox();
        barBg.setPrefSize(BAR_WIDTH, 14);
        barBg.setMaxSize(BAR_WIDTH, 14);
        barBg.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 7;");
        hpFill = new Region();
        hpFill.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 7;");
        barBg.getChildren().add(hpFill);

        HBox hpArea = new HBox(8);
        hpArea.setAlignment(Pos.CENTER_LEFT);
        hpArea.getChildren().addAll(hpText, barBg);

        // 弹性空隙 → 把右侧图标推到右上角
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        row1.getChildren().addAll(name, hpArea, spacer);

        // 地图图标（只在地图页之外的场景显示，例如战斗中用于“查看地图”）
        if (showMapIcon) {
            StackPane mapIcon = iconCard("图",
                    "linear-gradient(to bottom right, #86efac, #15803d);", "#052e16");
            Tooltip.install(mapIcon, new Tooltip("查看地图（战斗中也可查看）"));
            mapIcon.setOnMouseClicked(e -> onMapClick.run());
            row1.getChildren().add(mapIcon);
        }

        // 牌组图标（右上角）
        deckIcon.setCursor(javafx.scene.Cursor.HAND);
        deckIcon.setStyle("-fx-background-color: linear-gradient(to bottom right, #e5e7eb, #9ca3af); "
                + "-fx-background-radius: 8;");
        deckIcon.setPrefSize(46, 58);
        deckIcon.setMaxSize(46, 58);
        Label glyph = new Label("牌");
        glyph.setTextFill(Color.rgb(74, 54, 36));
        glyph.setFont(Font.font(17));
        glyph.setStyle("-fx-font-weight: bold;");
        deckIcon.getChildren().add(glyph);
        attachBadge(deckIcon, deckBadge);
        deckIcon.setOnMouseClicked(e -> onDeckClick.run());

        row1.getChildren().add(deckIcon);
        getChildren().add(row1);

        // ---------- 第二行：遗物 ----------
        relicRow.setAlignment(Pos.CENTER_LEFT);
        relicRow.setPadding(new Insets(6, 18, 6, 18));
        relicRow.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85);");
        getChildren().add(relicRow);

        // 保存点击回调供遗物图标用
        this.onRelicClick = onRelicClick;

        refresh();
    }

    private final Consumer<Relic> onRelicClick;

    /**
     * 开发者模式：在 HUD 上挂一个橙色的「开」按钮（在牌组图标左边），点开开发者面板。
     * 只有设置里开了开发者模式时，外层才会调用这个方法。
     */
    public void addDevButton(Runnable onClick) {
        StackPane dev = iconCard("开",
                "linear-gradient(to bottom right, #fde68a, #f59e0b);", "#78350f");
        Tooltip.install(dev, new Tooltip("开发者模式：修改牌组 / 手牌 / 遗物"));
        dev.setOnMouseClicked(e -> onClick.run());

        int idx = topRow.getChildren().indexOf(deckIcon);
        topRow.getChildren().add(Math.max(0, idx), dev);
    }

    /**
     * 开发者模式：在 HUD 上再挂一个红色的「杀」按钮（紧挨着「开」按钮），
     * 点一下直接秒杀当前敌人。
     * 只有<b>战斗中</b>且设置里开了开发者模式时，外层才会调用这个方法。
     */
    public void addDevKillButton(Runnable onClick) {
        StackPane kill = iconCard("杀",
                "linear-gradient(to bottom right, #fca5a5, #dc2626);", "#450a0a");
        Tooltip.install(kill, new Tooltip("开发者模式：一键秒杀当前敌人（直接结算胜利）"));
        kill.setOnMouseClicked(e -> onClick.run());

        int idx = topRow.getChildren().indexOf(deckIcon);
        topRow.getChildren().add(Math.max(0, idx), kill);
    }

    /** 通用小图标（地图用） */
    private static StackPane iconCard(String glyph, String grad, String color) {
        StackPane icon = new StackPane();
        icon.setPrefSize(46, 58);
        icon.setMaxSize(46, 58);
        icon.setCursor(javafx.scene.Cursor.HAND);
        icon.setStyle("-fx-background-color: " + grad + "; -fx-background-radius: 8;");
        Label g = new Label(glyph);
        g.setTextFill(Color.web(color));
        g.setFont(Font.font(17));
        g.setStyle("-fx-font-weight: bold;");
        icon.getChildren().add(g);
        return icon;
    }

    /** 数量下标挂右下角（牌组图标用） */
    private static void attachBadge(StackPane icon, Label badge) {
        badge.setTextFill(Color.WHITE);
        badge.setFont(Font.font(12));
        badge.setStyle("-fx-font-weight: bold; -fx-background-color: #dc2626; "
                + "-fx-background-radius: 8; -fx-padding: 0 5 0 5;");
        icon.getChildren().add(badge);
        StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(badge, new Insets(0, 2, 2, 0));
        badge.setVisible(false);
    }

    /** 牌组图标（右上角）；卡牌飞入牌组特效以此为终点。 */
    public Node getDeckIcon() {
        return deckIcon;
    }

    /** 刷新：血条 / 牌组数量 / 遗物图标 */
    public void refresh() {
        hpText.setText(player.hp() + " / " + player.maxHp);
        double ratio = player.maxHp > 0 ? (double) player.hp() / player.maxHp : 0.0;
        hpFill.setPrefWidth(Math.max(0, BAR_WIDTH * ratio));
        hpFill.setStyle("-fx-background-color: " + (ratio < 0.4 ? "#ef4444" : "#22c55e")
                + "; -fx-background-radius: 7;");

        deckBadge.setText(String.valueOf(player.deck.size()));
        deckBadge.setVisible(true);
        Tooltip.install(deckIcon, new Tooltip("牌组：共 " + player.deck.size() + " 张（点击查看）"));

        relicRow.getChildren().clear();
        for (Relic r : player.relics) {
            relicRow.getChildren().add(r.buildIcon(onRelicClick));
        }
        relicRow.setVisible(!player.relics.isEmpty());
    }
}
