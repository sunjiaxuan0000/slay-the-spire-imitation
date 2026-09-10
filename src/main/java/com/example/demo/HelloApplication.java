package com.example.demo;

import com.example.demo.battle.BattleView;
import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;
import com.example.demo.character.CharacterSelect;
import com.example.demo.character.Player;
import com.example.demo.character.Relic;
import com.example.demo.enemy.Boss;
import com.example.demo.enemy.Cultist_Pig;
import com.example.demo.enemy.Enemy;
import com.example.demo.enemy.GuardPig;
import com.example.demo.enemy.Slime;
import com.example.demo.event.EventDef;
import com.example.demo.event.EventView;
import com.example.demo.operator.DevEntry;
import com.example.demo.view.GameMap;
import com.example.demo.view.MainMenu;
import com.example.demo.view.MapView;
import com.example.demo.view.RoomView;
import com.example.demo.view.RunHud;
import com.example.demo.view.SettingsView;
import com.example.demo.sound.MusicFx;
import com.example.demo.sound.SoundFx;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class HelloApplication extends Application {

    /** 默认窗口分辨率（1280×720 = 16:9，和主菜单背景图同比例，铺满零裁切） */
    private static final double W = 1280;
    private static final double H = 720;

    /** 地图两侧黑边宽度（像素）——想调黑边宽窄就改这个数 */
    private static final double MAP_SIDE_MARGIN = 190;

    /** 主菜单的固定尺寸：离开菜单时记录，返回菜单时强制恢复，防止被游戏场景带�?*/
    private double menuW = W;
    private double menuH = H;

    /** 当前场景的“整页窗口”宿主：最上层放半透明遮罩 + 居中大面�?*/
    private StackPane overlayHost;
    private BattleView activeBattle = null; // 进行中的战斗（供只读地图暂停/恢复�?
    private boolean battleMapOpen = false;  // 战斗里是否开着“只读地图�?

    /** 按指定尺寸建场景（并自动挂上“点按钮出声”） */
    private Scene sizedAt(double w, double h, Parent root) {
        return decorate(new Scene(root, (w > 0) ? w : W, (h > 0) ? h : H));
    }

    /** 按“窗口当前大小”建场景（游戏内场景用，保持用户当前窗口尺寸） */
    private Scene sizedScene(Stage stage, Parent root) {
        double w = (stage.isShowing() && stage.getWidth() > 0) ? stage.getWidth() : W;
        double h = (stage.isShowing() && stage.getHeight() > 0) ? stage.getHeight() : H;
        return decorate(new Scene(root, w, h));
    }

    /** 给场景统一挂“点击各类按钮 → click.wav” */
    private Scene decorate(Scene scene) {
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            javafx.scene.Node n = (javafx.scene.Node) e.getTarget();
            while (n != null) {
                if (n instanceof Button) {
                    SoundFx.play("click");
                    break;
                }
                n = n.getParent();
            }
        });
        return scene;
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("杀戮猪塔");
        returnToMenu(stage);
        stage.show();

        stage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("退出确认");
            alert.setHeaderText(null);
            alert.setContentText("确定要退出程序吗？");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                event.consume();
            }
        });
    }

    // ================= 场景构建 =================

    /** 主菜单场景：始终按“离开菜单时记录的尺寸”构�?*/
    private Scene buildMenuScene(Stage stage) {
        MusicFx.playLoop("bgm_menu"); // 主菜单 BGM（没放 bgm_menu.wav 就静音）
        MainMenu menu = new MainMenu(
                () -> startCharacterSelect(stage),
                () -> showSettingsScene(stage),
                () -> stage.close()
        );
        Scene scene = sizedAt(menuW, menuH, menu);
        scene.setOnKeyPressed(e -> {
            double step = e.isShiftDown() ? 1 : 10;
            switch (e.getCode()) {
                case UP    -> menu.nudge(0, -step);
                case DOWN  -> menu.nudge(0, step);
                case LEFT  -> menu.nudge(-step, 0);
                case RIGHT -> menu.nudge(step, 0);
                case TAB   -> menu.selectNext();  // 切换方向键调节哪个按钮
                case F3    -> menu.toggleDebug();
                default    -> { }
            }
        });
        return scene;
    }

    /** 设置页面：音乐/音效音量 + 开发者模式开关；点“返回主菜单”或按 Esc 回去 */
    private void showSettingsScene(Stage stage) {
        SettingsView settings = new SettingsView(() -> returnToMenu(stage));
        Scene scene = sizedAt(menuW, menuH, settings);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) returnToMenu(stage);
        });
        stage.setScene(scene);
    }

    /** 角色选择场景（离开主菜单时记录当前窗口尺寸，返回时恢复�?*/
    private void startCharacterSelect(Stage stage) {
        if (stage.isShowing() && stage.getWidth() > 0 && stage.getHeight() > 0) {
            menuW = stage.getWidth(); // 记住用户在主菜单调的窗口大小
            menuH = stage.getHeight();
        }
        CharacterSelect select = new CharacterSelect(
                () -> startMap(stage)
        );
        Scene scene = sizedScene(stage, select);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                returnToMenu(stage);
            }
        });
        stage.setScene(scene);
    }

    /** 回到主菜单：强制恢复离开菜单时记录的窗口尺寸，避免分辨率被游戏场景带�?*/
    private void returnToMenu(Stage stage) {
        if (stage.isShowing() && stage.getWidth() > 0 && stage.getHeight() > 0) {
            // 只有回主菜单后用户手动调整过，这里才跟随；游戏期间的增长一律忽�?
        }
        stage.setWidth(menuW);
        stage.setHeight(menuH);
        stage.setScene(buildMenuScene(stage));
    }

    /** 开始一局：新玩家 + 新地�?*/
    private void startMap(Stage stage) {
        Player player = new Player();
        GameMap map = GameMap.generate();
        showMapScene(stage, map, player);
    }

    /** 地图场景（战斗后回同一张地图也用这个） */
    private void showMapScene(Stage stage, GameMap map, Player player) {
        SoundFx.play("map"); // 进入地图页音效
        MusicFx.playLoop("bgm_map"); // 地图 BGM（没放 bgm_map.wav 就保持安静）
        // 地图页本身不显示右上角“地图”按钮
        RunHud hud = buildHud(player, map, () -> showWindow(page(mapPage(map))), false);
        // 开发者模式：HUD 上多挂一个「开」按钮（地图场景没有战斗 → battle 传 null）
        DevEntry.attachDevButton(hud, player, null, hud::refresh,
                node -> showWindow(page(node)), this::closeWindow);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // 隐藏滚动条（滚轮/拖动仍可用）
        scroll.setPannable(true);
        scroll.setMinSize(0, 0); // 内容再高也不许把窗口撑大（否则回主菜单会被拉大）
        scroll.setStyle("-fx-background: #0b1020; -fx-background-color: #0b1020;");

        MapView view = new MapView(map,
                type -> handleArrive(stage, map, player, hud, type), scroll, true);
        DevEntry.enableDevMap(view); // 开发者模式：任意节点都能点
        scroll.setContent(view);

        // 让地图两侧留黑边：地图限宽居中，两侧露出黑底（HUD 仍占满宽度）
        StackPane mapArea = new StackPane(scroll);
        mapArea.setStyle("-fx-background-color: black;");
        scroll.maxWidthProperty().bind(
                mapArea.widthProperty().subtract(MAP_SIDE_MARGIN * 2));

        // 右侧黑边处贴图例 example.png（宽度随黑边宽度自适应）
        Node legend = legendNode();
        if (legend != null) {
            mapArea.getChildren().add(legend);
            StackPane.setAlignment(legend, Pos.CENTER_RIGHT);
            StackPane.setMargin(legend, new Insets(12, 6, 0, 0));
        }

        BorderPane content = new BorderPane();
        content.setTop(hud);
        content.setCenter(mapArea);

        Scene scene = wrapOverlay(stage, content);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                if (isWindowOpen()) {
                    closeWindow(); // 先关掉牌�?遗物等窗�?
                } else {
                    returnToMenu(stage);
                }
            }
        });
        // 新一局（还没出发）：弹阶段标题 �?�?BOSS 顶部向下滑过整张地图
        // 战斗/事件结束后回来：直接把“下一层可走的节点”滚到屏幕中�?
        Platform.runLater(() -> {
            if (map.current == null) {
                playStageIntro(scroll);
            } else {
                view.scrollToLayer(Math.min(map.current.row + 1, GameMap.ROWS - 1));
            }
        });
        stage.setScene(scene);
    }

    /** 开场演出：先显示“第一阶段 / 猪塔底”，再从地图顶部(BOSS)一路滑到底�?起点)展示全图 */
    private void playStageIntro(ScrollPane scroll) {
        // 1) 阶段标题遮罩
        StackPane dim = new StackPane();
        dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.6);");

        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);

        Label line1 = new Label("第一阶段");
        line1.setTextFill(Color.rgb(252, 211, 77));
        line1.setFont(Font.font(34));
        line1.setStyle("-fx-font-weight: bold;");

        Label line2 = new Label("猪塔底");
        line2.setTextFill(Color.WHITE);
        line2.setFont(Font.font(78));
        line2.setStyle("-fx-font-weight: bold;");

        box.getChildren().addAll(line1, line2);

        StackPane window = new StackPane();
        window.getChildren().addAll(dim, box);

        // 2) 显示标题�?0.4 秒后收起，开始从顶部滑到底部的展�?
        showWindow(window);
        PauseTransition hold = new PauseTransition(Duration.millis(400));
        hold.setOnFinished(e -> {
            closeWindow();
            scroll.setVvalue(0.0); // 从顶部（BOSS）开�?
            Timeline sweep = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(scroll.vvalueProperty(), 0.0)),
                    new KeyFrame(Duration.millis(1800),
                            new KeyValue(scroll.vvalueProperty(), 1.0, Interpolator.EASE_BOTH))
            );
            sweep.play();
        });
        hold.play();
    }

    /** 战斗场景：顶�?HUD + 战斗主体，外面再包整页窗口层 */
    private void startBattle(Stage stage, GameMap map, Player player,
                             GameMap.NodeType type, Enemy enemy) {
        MusicFx.playLoop("bgm_battle"); // 战斗 BGM
        RunHud hud = buildHud(player, map, () -> openBattleMapReadOnly(stage, map), true);

        BattleView battle = new BattleView(player, hud, enemy,
                won -> {
            activeBattle = null;
            battleMapOpen = false;
            if (won) {
                if (type == GameMap.NodeType.BOSS) {
                    returnToMenu(stage); // 通关
                } else {
                    showMapScene(stage, map, player);
                }
            } else {
                returnToMenu(stage);     // 阵亡
            }
        }, type == GameMap.NodeType.BOSS); // true=用 boss 战斗背景，否则 default 背景
        activeBattle = battle;

        // 开发者模式：战斗里也能改牌组/手牌/遗物（手牌改完立刻重画）
        DevEntry.attachDevButton(hud, player, battle, hud::refresh,
                node -> showWindow(page(node)), this::closeWindow);

        BorderPane content = new BorderPane();
        content.setTop(hud);
        content.setCenter(battle);

        Scene scene = wrapOverlay(stage, content);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                if (isWindowOpen()) {
                    closeBattleMapOrWindow(); // 开着只读地图/牌组窗口 �?先关，不退出战�?
                } else {
                    returnToMenu(stage); // 逃跑=放弃本局
                }
            }
        });
        stage.setScene(scene);
    }

    // ================= 战斗中的只读地图 =================

    /** 点“查看地图”：暂停战斗，铺满整屏的只读地图；点“返回战斗”恢�?*/
    private void openBattleMapReadOnly(Stage stage, GameMap map) {
        BattleView b = activeBattle;
        if (b == null) return;
        b.setPaused(true);
        battleMapOpen = true;
        SoundFx.play("map"); // 进入地图页音效

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // 隐藏滚动条（滚轮/拖动仍可用）
        scroll.setPannable(true);
        scroll.setStyle("-fx-background: #0b1020; -fx-background-color: #0b1020;");

        MapView preview = new MapView(map, t -> { }, scroll, false); // 只读地图界面
        scroll.setContent(preview);

        // 同样的两侧黑边：地图限宽居中
        StackPane mapArea = new StackPane(scroll);
        mapArea.setStyle("-fx-background-color: black;");
        scroll.maxWidthProperty().bind(
                mapArea.widthProperty().subtract(MAP_SIDE_MARGIN * 2));

        // 右侧黑边处同样贴图例
        Node legend2 = legendNode();
        if (legend2 != null) {
            mapArea.getChildren().add(legend2);
            StackPane.setAlignment(legend2, Pos.TOP_RIGHT);
            StackPane.setMargin(legend2, new Insets(12, 6, 0, 0));
        }

        Button back = new Button("返回战斗");
        back.setFont(Font.font(17));
        back.setPrefSize(170, 46);
        back.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; "
                + "-fx-background-radius: 12; -fx-cursor: hand;");
        back.setOnAction(e -> closeBattleMapOrWindow());
        StackPane.setAlignment(back, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(back, new Insets(0, 30, 24, 0));

        StackPane window = new StackPane();
        window.getChildren().addAll(mapArea, back);

        // 把“当前所在层”滚到屏幕中�?
        Platform.runLater(() -> preview.scrollToLayer(
                map.current == null ? 0 : map.current.row));
        showWindow(window);
    }

    /** 关闭只读地图 / 其它窗口，并恢复战斗 */
    private void closeBattleMapOrWindow() {
        closeWindow();
        if (battleMapOpen && activeBattle != null) {
            battleMapOpen = false;
            activeBattle.setPaused(false); // 回到战斗继续
        }
    }

    private boolean isWindowOpen() {
        return overlayHost != null && !overlayHost.getChildren().isEmpty();
    }

    /** 把普通场景内容包一层：下面内容，上面是整页窗口层（尺寸跟随当前窗口�?*/
    private Scene wrapOverlay(Stage stage, Node content) {
        overlayHost = new StackPane();
        overlayHost.setMouseTransparent(true); // 平时不挡鼠标（没开窗口时不拦截点击�?
        StackPane root = new StackPane();
        root.getChildren().addAll(content, overlayHost);
        return sizedScene(stage, root);
    }

    /** 右侧黑边上的图例（example.png），宽度自动跟随 MAP_SIDE_MARGIN */
    private Node legendNode() {
        var in = getClass().getResourceAsStream("/com/example/demo/icons/example.png");
        if (in == null) return null;
        ImageView iv = new ImageView(new Image(in));
        iv.setPreserveRatio(true);
        iv.setMouseTransparent(true);
        double w = 300;
        iv.setFitWidth(w);
        iv.setFitHeight(w);
        return iv;
    }

    // ================= 顶部 HUD（含三个整页窗口入口�?=================

    private RunHud buildHud(Player player, GameMap map, Runnable onMapClick, boolean showMapIcon) {
        return new RunHud(
                player,
                r -> showWindow(page(relicPage(r))),   // 点遗物图�?
                () -> showWindow(page(deckPage(player))), // 点牌组图�?
                onMapClick,                           // 点地图图标（场景自定义）
                showMapIcon
        );
    }

    // ================= 整页窗口机制 =================

    /** 打开一个整页窗口（清掉旧的�?*/
    private void showWindow(StackPane window) {
        overlayHost.getChildren().clear();
        overlayHost.setMouseTransparent(false); // 窗口打开后要能点遮罩/按钮
        overlayHost.getChildren().add(window);
    }

    /** 半透明遮罩 + 居中内容：点遮罩关闭（和抽牌堆窗口同款结构） */
    private StackPane page(Node centerBox) {
        Pane dim = new Pane();
        dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.72);");
        dim.setOnMouseClicked(e -> closeWindow());

        StackPane page = new StackPane();
        page.getChildren().addAll(dim, centerBox);
        StackPane.setAlignment(centerBox, Pos.CENTER);
        return page;
    }

    private void closeWindow() {
        overlayHost.getChildren().clear();
        overlayHost.setMouseTransparent(true); // 关掉后恢复“不挡鼠标�?
    }

    // ================= 三个整页窗口 =================

    /** 牌组页：所有牌�?id 排列 */
    private VBox deckPage(Player player) {
        List<Card> sorted = new ArrayList<>(player.deck);
        sorted.sort(Comparator.comparingInt(c -> c.id));

        FlowPane cards = new FlowPane(8, 8);
        cards.setPrefWrapLength(900);
        for (Card c : sorted) {
            cards.getChildren().add(CardFaceView.buildAt(c, 120)); // 分层贴图卡面
        }

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setPrefSize(900, 360);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: #0f172a; -fx-background-color: #0f172a;");

        Label title = new Label("牌组 · 共 " + player.deck.size() + " 张");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(24));
        title.setStyle("-fx-font-weight: bold;");

        Label sub = new Label("按牌 id 排列 · 点遮罩或“关闭”退出");
        sub.setTextFill(Color.rgb(148, 163, 184));
        sub.setFont(Font.font(13));

        Button close = closeButton();

        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 16; -fx-padding: 18;");
        panel.getChildren().addAll(title, sub, scroll, close);
        return panel;
    }

    /** 地图页：战斗中也能查看的整页大地图（只读�?*/
    private VBox mapPage(GameMap map) {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // 隐藏滚动条（滚轮/拖动仍可用）
        scroll.setPannable(true);
        scroll.setStyle("-fx-background: #0b1020; -fx-background-color: #0b1020;");
        scroll.setPrefSize(1060, 430);

        MapView preview = new MapView(map, t -> { }, scroll, false); // 只读
        scroll.setContent(preview);
        Platform.runLater(() -> scroll.setVvalue(1.0));

        Label title = new Label("当前地图（查看）");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(24));
        title.setStyle("-fx-font-weight: bold;");

        Label sub = new Label("战斗中也可以查看 · 滚轮浏览");
        sub.setTextFill(Color.rgb(148, 163, 184));
        sub.setFont(Font.font(13));

        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 16; -fx-padding: 18;");
        panel.getChildren().addAll(title, sub, scroll, closeButton());
        return panel;
    }

    /** 遗物页：大图�?+ 详细介绍 */
    private VBox relicPage(Relic r) {
        StackPane icon = new StackPane();
        icon.setPrefSize(96, 96);
        icon.setMaxSize(96, 96);
        icon.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 18;");
        Label g = new Label(r.name.substring(0, 1));
        g.setTextFill(Color.WHITE);
        g.setFont(Font.font(44));
        g.setStyle("-fx-font-weight: bold;");
        icon.getChildren().add(g);

        Label name = new Label(r.name);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(26));
        name.setStyle("-fx-font-weight: bold;");

        Label desc = new Label(r.desc);
        desc.setTextFill(Color.rgb(203, 213, 225));
        desc.setFont(Font.font(16));
        desc.setWrapText(true);
        desc.setMaxWidth(360);

        VBox panel = new VBox(12);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 16; -fx-padding: 22 30 16 30;");
        panel.getChildren().addAll(icon, name, desc, closeButton());
        return panel;
    }

    private Button closeButton() {
        Button close = new Button("关闭");
        close.setFont(Font.font(14));
        close.setPrefSize(110, 34);
        close.setStyle("-fx-background-color: #475569; -fx-text-fill: white; "
                + "-fx-background-radius: 9; -fx-cursor: hand;");
        close.setOnAction(e -> closeWindow());
        return close;
    }

    /** 详情/牌组里的小牌 */
    private VBox miniCard(Card c) {
        String color = switch (c.kind) {
            case STRIKE   -> "#991b1b";
            case DEFEND   -> "#1d4ed8";
            case BASH     -> "#b45309";
            case HAMMER   -> "#7c2d12";
            case IMPREGNABLE -> "#334155";
            case DOUBLE_STRIKE -> "#c2410c";
            case KINDLE   -> "#9a3412";
            default       -> "#475569";
        };
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(118, 150);
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12;");

        Label id = new Label("#" + c.id);
        id.setTextFill(Color.rgb(255, 255, 255, 0.75));
        id.setFont(Font.font(11));

        Label name = new Label(c.kind.label);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(16));
        name.setStyle("-fx-font-weight: bold;");

        Label value = new Label(c.damage > 0 ? "伤害 " + c.damage : "格挡 " + c.block);
        value.setTextFill(Color.rgb(254, 243, 199));
        value.setFont(Font.font(13));

        card.getChildren().addAll(id, name, value);
        return card;
    }

    // ================= 地图节点事件 =================

    private void handleArrive(Stage stage, GameMap map, Player player, RunHud hud,
                              GameMap.NodeType type) {
        switch (type) {
            case MONSTER -> startBattle(stage, map, player, GameMap.NodeType.MONSTER,
                    Math.random() < 0.4 ? new Cultist_Pig() : new Slime());
            case ELITE   -> startBattle(stage, map, player, GameMap.NodeType.ELITE, new GuardPig());
            case BOSS    -> startBattle(stage, map, player, GameMap.NodeType.BOSS, new Boss());
            case START   -> showRoomScene(stage, map, player);
            case EVENT   -> {
                List<EventDef> events = EventDef.pool();
                EventDef ev = events.get(new java.util.Random().nextInt(events.size()));
                startEventScene(stage, map, player, ev);
            }
            case REST    -> {
                int before = player.hp();
                player.heal(player.maxHp);
                hud.refresh();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("节点事件");
                alert.setHeaderText(null);
                alert.setContentText("你点燃篝火休息，生命 " + before + " → " + player.hp());
                alert.showAndWait();
            }
            case TREASURE -> {
                Relic gained = randomTreasure(player);
                hud.refresh();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("宝箱");
                alert.setHeaderText(null);
                alert.setContentText(gained == null
                        ? "宝箱里的遗物你已经有了……空空如也。"
                        : "你打开宝箱，获得遗物：「" + gained.name + "」\n" + gained.desc);
                alert.showAndWait();
            }
        }
    }

    /** 宝箱：从遗物池随机给一个还没拿过的遗物 */
    private Relic randomTreasure(Player player) {
        List<Relic> pool = Relic.pool(); // 全部遗物（起点三选一 / 开发者面板用同一份）
        pool.removeIf(r -> player.relics.stream().anyMatch(h -> h.name.equals(r.name)));
        if (pool.isEmpty()) return null;
        int idx = new java.util.Random().nextInt(pool.size());
        Relic gained = pool.get(idx);
        player.addRelic(gained);
        return gained;
    }

    /** 随机奖励一张卡（事�?奖励用） */
    private Card randomRewardCard() {
        List<Card> pool = List.of(
                Card.strike(), Card.defend(), Card.bash(),
                Card.hammer(), Card.impregnable(),
                Card.doubleStrike(), Card.kindle());
        int idx = new java.util.Random().nextInt(pool.size());
        return pool.get(idx);
    }

    // ================= 事件 =================

    /** 事件场景：专属背景图 + 右侧名称/描述 + 选项，选完结算回地�?*/
    private void startEventScene(Stage stage, GameMap map, Player player, EventDef ev) {
        EventView view = new EventView(ev, opt -> {
            String msg = applyEventOption(player, opt);
            if (player.hp() == 0) {
                Alert over = new Alert(Alert.AlertType.INFORMATION);
                over.setTitle("事件结果");
                over.setHeaderText(null);
                over.setContentText(msg + "\n\n你的生命归零……本局结束。");
                over.setOnHidden(e -> returnToMenu(stage));
                over.showAndWait();
            } else {
                Alert result = new Alert(Alert.AlertType.INFORMATION);
                result.setTitle("事件结果");
                result.setHeaderText(null);
                result.setContentText(msg);
                result.setOnHidden(e -> showMapScene(stage, map, player));
                result.showAndWait();
            }
        });

        Scene scene = sizedScene(stage, view);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                returnToMenu(stage); // 放弃本局
            }
        });
        stage.setScene(scene);
    }

    /** 结算事件选项的真实效果，返回结果描述文字 */
    private String applyEventOption(Player player, EventDef.Option opt) {
        return switch (opt.action) {
            case HEAL -> {
                int before = player.hp();
                player.heal(opt.amount);
                yield "回复 " + opt.amount + " 点生命：" + before + " → " + player.hp();
            }
            case DAMAGE -> {
                int before = player.hp();
                player.damage(opt.amount);
                yield "失去 " + opt.amount + " 点生命：" + before + " → " + player.hp();
            }
            case ADD_CARD -> {
                Card c = randomRewardCard();
                player.deck.add(c);
                yield "获得卡牌：「" + c.kind.label + "」加入牌组（#" + c.id + "）";
            }
            case ADD_RELIC -> {
                Relic r = randomTreasure(player);
                yield r == null
                        ? "遗物池里已经没有新遗物了……"
                        : "获得遗物：「" + r.name + "」\n" + r.desc;
            }
            case NOTHING -> opt.effectDesc + "（无事发生）";
        };
    }

    /** 起点房间：NPC + 三选一初始遗物 */
    private void showRoomScene(Stage stage, GameMap map, Player player) {
        List<Relic> starters = Relic.pool(); // 起点三选一

        RoomView room = new RoomView(
                player, starters, "猪神",
                "猪，你未到校，也未请假。若10点前未及时到场，将不再是迟到，而记为旷课。收到速回",
                () -> showMapScene(stage, map, player)
        );

        Scene scene = sizedScene(stage, room);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                returnToMenu(stage);
            }
        });
        stage.setScene(scene);
    }
}
