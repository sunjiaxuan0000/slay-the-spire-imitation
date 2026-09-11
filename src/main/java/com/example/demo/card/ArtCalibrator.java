package com.example.demo.card;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * 卡面美术「位置校准器」——不是游戏的一部分，是个开发者小工具。
 *
 * <p><b>用法</b>：在 IDEA 里直接右键 Run 这个类的 main。
 * <ol>
 *   <li>右上角选一张卡（决定用哪张基础图：攻击 / 技能 / 能力）</li>
 *   <li>拖 X / Y / 宽 / 高 / 圆角 五个滑块，左边预览实时跟着动</li>
 *   <li>调满意了，把右下角打印出来的常量粘回 {@link CardFaceView}</li>
 * </ol>
 *
 * <p>预览里：<b>红色虚线框</b> = 美术要放的框；框里显示的是
 * {@code art/<Kind>.png}（放了图才看得到）。没放图也能调框 ——
 * 先把位置量好，回头把图丢进去即可。
 */
public class ArtCalibrator extends Application {

    /** 预览放大倍数（卡面是 150×210，放大后才看得清细节） */
    private static final double ZOOM = 3;

    private Card.Kind kind = Card.Kind.STRIKE;

    private StackPane previewHost;
    private Pane face;                       // 150×210 的卡面，被 ZOOM 放大显示
    private final Label info = new Label();
    private final Label artHint = new Label();

    private final Slider sx = slider(0, 150, 13, 1);
    private final Slider sy = slider(0, 210, 15, 1);
    private final Slider sw = slider(10, 150, 126, 1);
    private final Slider sh = slider(10, 210, 80, 1);

    private static Slider slider(double min, double max, double val, double step) {
        Slider s = new Slider(min, max, val);
        s.setBlockIncrement(step);
        s.setMajorTickUnit((max - min) / 4);
        s.setShowTickMarks(true);
        s.setPrefWidth(240);
        return s;
    }

    @Override
    public void start(Stage stage) {
        // 正式美术层先别画 —— 这里要用滑块这套参数来预览
        CardFaceView.artLayerSuppressed = true;

        previewHost = new StackPane();
        previewHost.setPrefSize(CardFaceView.CARD_W * ZOOM + 60, CardFaceView.CARD_H * ZOOM + 60);
        previewHost.setMinSize(CardFaceView.CARD_W * ZOOM + 60, CardFaceView.CARD_H * ZOOM + 60);
        previewHost.setStyle("-fx-background-color: #2b2b30;");

        VBox panel = buildPanel();

        HBox root = new HBox(18, previewHost, panel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #17171a;");

        rebuildFace();
        for (Slider s : new Slider[]{sx, sy, sw, sh}) {
            s.valueProperty().addListener((o, a, b) -> redrawOverlay());
        }

        stage.setScene(new Scene(root, CardFaceView.CARD_W * ZOOM + 60 + 400 + 70,
                CardFaceView.CARD_H * ZOOM + 90));
        stage.setTitle("卡面美术校准器");
        stage.show();
    }

    private VBox buildPanel() {
        ComboBox<Card.Kind> picker = new ComboBox<>();
        picker.getItems().addAll(Card.Kind.values());
        picker.setValue(kind);
        picker.setPrefWidth(240);
        picker.valueProperty().addListener((o, a, b) -> {
            if (b == null) return;
            kind = b;
            resetToDefault();
            rebuildFace();
        });

        Button reset = new Button("恢复该类型默认值");
        reset.setOnAction(e -> {
            resetToDefault();
            redrawOverlay();
        });

        Button dump = new Button("把常量打印到控制台");
        dump.setOnAction(e -> {
            String txt = constantsText();
            System.out.println(txt);
            info.setText(txt);
        });

        artHint.setFont(Font.font(11.5));
        artHint.setStyle("-fx-text-fill: #9aa0a6;");
        artHint.setWrapText(true);
        artHint.setPrefWidth(280);

        info.setFont(Font.font("Consolas", 12));
        info.setStyle("-fx-text-fill: #d7e3c8; -fx-background-color: #101014; -fx-padding: 10;");
        info.setWrapText(true);
        info.setPrefWidth(300);

        VBox box = new VBox(10,
                label("选卡（决定用哪张基础图）"), picker,
                new Separator(),
                label("X（左上角横坐标）"), sx,
                label("Y（左上角纵坐标）"), sy,
                label("宽"), sw,
                label("高"), sh,
                new HBox(10, reset, dump),
                new Separator(),
                artHint,
                info);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1f1f24; -fx-background-radius: 12;");
        box.setPrefWidth(340);
        return box;
    }

    private static Label label(String t) {
        Label l = new Label(t);
        l.setFont(Font.font(12));
        l.setStyle("-fx-text-fill: #c8ccd2;");
        return l;
    }

    private void resetToDefault() {
        double[] b = CardFaceView.defaultArtBox(kind.type);
        sx.setValue(b[0]);
        sy.setValue(b[1]);
        sw.setValue(b[2]);
        sh.setValue(b[3]);
    }

    /** 重建整张卡面（换卡时调用） */
    private void rebuildFace() {
        previewHost.getChildren().clear();
        face = CardFaceView.build(Card.of(kind)); // 此时美术层已被抑制
        face.setScaleX(ZOOM);
        face.setScaleY(ZOOM);
        previewHost.getChildren().add(face);
        redrawOverlay();
    }

    /** 只重画覆盖层（拖滑块时调用，不重建整张卡） */
    private void redrawOverlay() {
        if (face == null) return;
        // 把上一次的覆盖层摘掉（保留卡面自身的那些层）
        face.getChildren().removeIf(n -> "overlay".equals(n.getId()));

        double x = sx.getValue(), y = sy.getValue();
        double w = sw.getValue(), h = sh.getValue();

        // --- 美术预览：插到 index 0，跟正式实现一样垫在基础图"下面"，
        //     所以这里看到的就是最终效果（基础图的透明区域把美术裁出形状）---
        Image art = CardFaceView.artOf(kind);
        if (art != null && w > 0 && h > 0) {
            double iw = art.getWidth(), ih = art.getHeight();
            if (iw > 0 && ih > 0) {
                double s = Math.max(w / iw, h / ih); // 与 ART_FIT = COVER 对齐
                ImageView v = new ImageView(art);
                v.setId("overlay");
                v.setFitWidth(iw * s);
                v.setFitHeight(ih * s);
                v.setPreserveRatio(false);
                v.setLayoutX(x + w / 2 - iw * s / 2); // 以框中心居中
                v.setLayoutY(y + h / 2 - ih * s / 2);
                v.setMouseTransparent(true);
                face.getChildren().add(0, v);
            }
        }

        // --- 红色虚线框：美术"框"的范围（画在最上面，随时能看到）---
        Rectangle outline = new Rectangle(x, y, w, h);
        outline.setId("overlay");
        outline.setFill(Color.TRANSPARENT);
        outline.setStroke(Color.rgb(255, 70, 70));
        outline.setStrokeWidth(2.0 / ZOOM);
        outline.getStrokeDashArray().setAll(6.0 / ZOOM, 4.0 / ZOOM);
        outline.setMouseTransparent(true);
        face.getChildren().add(outline);

        info.setText(constantsText());

        if (art == null) {
            artHint.setText("这张卡还没放美术图。\n应该放在：\n" + CardFaceView.artPathFor(kind)
                    + "\n（现在调框也能用，回头把图丢进去就自动生效）");
        } else {
            artHint.setText("已找到美术图：\n" + CardFaceView.artPathFor(kind)
                    + "\n原图 " + (int) art.getWidth() + "×" + (int) art.getHeight()
                    + "\n美术垫在基础图下面，形状由基础图决定，不用自己裁");
        }
    }

    /** 生成可以直接粘回 CardFaceView 的常量文本 */
    private String constantsText() {
        String suffix = switch (kind.type) {
            case ATTACK -> "ATTACK";
            case SKILL, STATUS -> "SKILL";
            case POWER -> "POWER";
        };
        return String.format("ART_BOX_%s = { %d, %d, %d, %d };",
                suffix,
                Math.round(sx.getValue()), Math.round(sy.getValue()),
                Math.round(sw.getValue()), Math.round(sh.getValue()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
