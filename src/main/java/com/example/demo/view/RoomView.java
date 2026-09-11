package com.example.demo.view;

import com.example.demo.character.Player;
import com.example.demo.character.Relic;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;
import java.util.Random;

/**
 * “起点房间”：进入地图冒险前和 NPC 对话、选一个初始遗物。
 *
 * 版面（从上到下）：
 *   …上半屏留白，露出背景图 nieo.png…
 *   对话框   ← 随机台词，点一下换一句
 *   遗物选项 1
 *   遗物选项 2   ← 三个选项在屏幕下半部分，一行一个，格子写“名字 + 描述”
 *   遗物选项 3
 *   提示 + “离开房间”按钮（选中遗物后才出现）
 *
 * 背景用 nieo.png 等比铺满（cover），所以窗口怎么拉都不会变形。
 */
public class RoomView extends StackPane {

    /** 背景图路径（放 resources/com/example/demo/ 下） */
    private static final String BG = "/com/example/demo/nieo.png";

    /** 下半部分内容块的宽度（遗物格子、对话框都按这个宽） */
    private static final double CONTENT_W = 880;

    private static final String CARD_STYLE = "-fx-background-color: rgba(15, 23, 42, 0.82); "
            + "-fx-background-radius: 12; -fx-border-color: rgba(148, 163, 184, 0.45); "
            + "-fx-border-width: 2; -fx-border-radius: 12; -fx-padding: 12 18 12 18;";
    private static final String CARD_STYLE_HOVER = "-fx-background-color: rgba(30, 41, 59, 0.92); "
            + "-fx-background-radius: 12; -fx-border-color: rgba(226, 232, 240, 0.75); "
            + "-fx-border-width: 2; -fx-border-radius: 12; -fx-padding: 12 18 12 18;";
    private static final String CARD_STYLE_CHOSEN = "-fx-background-color: rgba(69, 26, 3, 0.92); "
            + "-fx-background-radius: 12; -fx-border-color: #fbbf24; "
            + "-fx-border-width: 3; -fx-border-radius: 12; -fx-padding: 12 18 12 18;";

    private final Player player;
    private final List<Relic> options;
    private final List<String> dialogues; // 随机台词池
    private final String npcName;
    private final Runnable onLeave;
    private final Random rnd = new Random();

    private boolean chosen = false;
    private int dialogIndex = -1;
    private final List<Button> optionButtons = new java.util.ArrayList<>();

    private final Label dialogText = new Label();
    private final Button leaveBtn = new Button("离开房间");
    private final Label hint = new Label("选择一个初始遗物后即可离开");

    /** 兼容旧写法：只给一句台词 */
    public RoomView(Player player, List<Relic> options, String npcName,
                    String npcDialog, Runnable onLeave) {
        this(player, options, npcName, List.of(npcDialog), onLeave);
    }

    public RoomView(Player player, List<Relic> options, String npcName,
                    List<String> dialogues, Runnable onLeave) {
        this.player = player;
        this.options = options;
        this.npcName = npcName;
        this.dialogues = (dialogues == null || dialogues.isEmpty())
                ? List.of("……") : dialogues;
        this.onLeave = onLeave;

        // ---- 背景：nieo.png 等比铺满 ----
        Image bg = loadImage(BG);
        if (bg != null) {
            setBackground(cover(bg));
        } else {
            setStyle("-fx-background-color: linear-gradient(to bottom, #131a22, #1c2430);");
        }

        // ---- 下半部分：对话框 → 三个遗物格子 → 提示/离开 ----
        VBox lower = new VBox(11);
        lower.setAlignment(Pos.CENTER);
        lower.setMaxWidth(CONTENT_W);

        lower.getChildren().add(buildDialogBox());
        for (Relic r : options) {
            lower.getChildren().add(buildRelicRow(r));
        }
        lower.getChildren().add(buildFooter());

        StackPane.setAlignment(lower, Pos.BOTTOM_CENTER);
        StackPane.setMargin(lower, new Insets(0, 20, -400, 20));
        getChildren().add(lower);

        showRandomDialog(); // 开局随机一句

        // 左上角提示（Esc = 放弃本局回主菜单）
        Label esc = new Label("Esc 放弃本局回主菜单");
        esc.setTextFill(Color.rgb(203, 213, 225, 0.7));
        esc.setFont(Font.font(13));
        StackPane.setAlignment(esc, Pos.TOP_LEFT);
        StackPane.setMargin(esc, new Insets(14, 0, 0, 18));
        getChildren().add(esc);
    }

    // ================= 对话框 =================

    /** 对话框：选项上方那条，点一下随机换一句台词 */
    private StackPane buildDialogBox() {
        Label nameTag = new Label(npcName);
        nameTag.setTextFill(Color.rgb(253, 224, 71));
        nameTag.setFont(Font.font(16));
        nameTag.setStyle("-fx-font-weight: bold;");

        dialogText.setTextFill(Color.rgb(241, 245, 249));
        dialogText.setFont(Font.font(16));
        dialogText.setWrapText(true);
        dialogText.setMaxWidth(CONTENT_W - 70);

        VBox box = new VBox(6, nameTag, dialogText);
        box.setAlignment(Pos.CENTER_LEFT);

        StackPane bubble = new StackPane(box);
        bubble.setPadding(new Insets(14, 20, 14, 20));
        bubble.setMaxWidth(CONTENT_W);
        bubble.setMinHeight(104);
        bubble.setStyle("-fx-background-color: rgba(15, 23, 42, 0.86); "
                + "-fx-background-radius: 14; -fx-border-color: rgba(148, 163, 184, 0.5); "
                + "-fx-border-width: 1; -fx-border-radius: 14;");
        bubble.setCursor(Cursor.HAND);
        Tooltip.install(bubble, new Tooltip("点一下换一句台词"));
        bubble.setOnMouseClicked(e -> showRandomDialog());
        StackPane.setAlignment(box, Pos.CENTER_LEFT);
        return bubble;
    }

    /** 从台词池里随机挑一句（尽量不和上一句重复） */
    private void showRandomDialog() {
        if (dialogues.size() == 1) {
            dialogIndex = 0;
        } else {
            int next;
            do {
                next = rnd.nextInt(dialogues.size());
            } while (next == dialogIndex);
            dialogIndex = next;
        }
        dialogText.setText("“" + dialogues.get(dialogIndex) + "”");
    }

    // ================= 遗物选项 =================

    /** 一个遗物格子：一行搞定「名字 + 描述」 */
    private Button buildRelicRow(Relic r) {
        Label name = new Label(r.name);
        name.setTextFill(Color.rgb(253, 224, 71));
        name.setFont(Font.font(19));
        name.setStyle("-fx-font-weight: bold;");
        name.setMinWidth(150);

        Label desc = new Label(r.desc);
        desc.setTextFill(Color.rgb(226, 232, 240));
        desc.setFont(Font.font(15));
        desc.setWrapText(false); // 只占一行
        desc.setMaxWidth(CONTENT_W - 240);

        HBox row = new HBox(16, name, desc);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefWidth(CONTENT_W - 40);

        Button btn = new Button();
        btn.setGraphic(row);
        btn.setStyle(CARD_STYLE);
        btn.setCursor(Cursor.HAND);
        btn.setMaxWidth(CONTENT_W);
        btn.setMinWidth(CONTENT_W);
        btn.setPrefHeight(66);
        btn.setOnMouseEntered(e -> {
            if (!chosen) btn.setStyle(CARD_STYLE_HOVER);
        });
        btn.setOnMouseExited(e -> {
            if (!chosen) btn.setStyle(CARD_STYLE);
        });
        btn.setOnAction(e -> choose(r, btn));
        optionButtons.add(btn);
        return btn;
    }

    /** 选中一个遗物：加金框、清掉其它选项的高亮、出现离开按钮 */
    private void choose(Relic r, Button btn) {
        if (chosen) return;
        chosen = true;

        // 先把所有格子恢复普通样式，再高亮选中的那个
        for (Button b : optionButtons) b.setStyle(CARD_STYLE);
        btn.setStyle(CARD_STYLE_CHOSEN);
        btn.setDisable(false); // 保持可点（只是不再响应，choose 里有 chosen 守卫）

        player.addRelic(r); // 遗物记进玩家状态（含破镜等即时效果，由 RelicFun 统一处理）
        // 破镜：需要 UI 交互，单独处理
        if (r.name.equals("破镜")) {
            removeCardFromDeck();
        }
        
        hint.setText("已获得：" + r.name + " —— 可以离开了");
        hint.setTextFill(Color.rgb(251, 191, 36));
        leaveBtn.setVisible(true);
    }

    /**
     * 破镜效果：让玩家选择一张卡牌删除。
     *
     * <p>版面走 {@link RemoveCardOverlay}，与牌组页（{@code HelloApplication.deckPage}）同款 ——
     * 深色面板 + 24px 标题 + 卡面滚动区，区别只是这里的卡面可以点。
     */
    private void removeCardFromDeck() {
        if (player.deck.isEmpty()) return;

        RemoveCardOverlay picker = new RemoveCardOverlay(
                player,
                "破镜 · 选择一张牌移除",
                c -> {
                    player.deck.remove(c);
                    hint.setText("已移除：" + c.kind.label + " —— 可以离开了");
                });
        getChildren().add(picker); // RoomView 是 StackPane：铺在最上层盖住整个房间
        picker.show();
    }

    // ================= 底部 =================

    private HBox buildFooter() {
        hint.setTextFill(Color.rgb(226, 232, 240, 0.75));
        hint.setFont(Font.font(14));

        leaveBtn.setFont(Font.font(17));
        leaveBtn.setPrefSize(170, 46);
        leaveBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; "
                + "-fx-background-radius: 12; -fx-cursor: hand;");
        leaveBtn.setVisible(false);
        leaveBtn.setOnAction(e -> onLeave.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(16, hint, spacer, leaveBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setMaxWidth(CONTENT_W);
        footer.setMinWidth(CONTENT_W);
        return footer;
    }

    // ================= 工具 =================

    private static Image loadImage(String path) {
        var in = RoomView.class.getResourceAsStream(path);
        return in == null ? null : new Image(in);
    }

    /** 背景图等比铺满（cover）：按窗口比例放大，多余部分居中裁掉 */
    private static Background cover(Image image) {
        return new Background(new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1, 1, true, true, false, true)));
    }
}
