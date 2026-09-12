package com.example.demo;

import com.example.demo.battle.BattleView;
import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;
import com.example.demo.card.CardRewardPool;
import com.example.demo.character.CharacterSelect;
import com.example.demo.character.Player;
import com.example.demo.character.Relic;
import com.example.demo.character.RelicFun;
import com.example.demo.enemy.Enemy;
import com.example.demo.enemy.EnemyFactory;
import com.example.demo.enemy.GuardPig;
import com.example.demo.event.EventDef;
import com.example.demo.event.EventView;
import com.example.demo.operator.DevEntry;
import com.example.demo.view.GameMap;
import com.example.demo.view.MainMenu;
import com.example.demo.view.MapView;
import com.example.demo.view.RoomView;
import com.example.demo.view.RunHud;
import com.example.demo.view.SettingsView;
import com.example.demo.view.RelicObtainToast;
import com.example.demo.view.RestView;
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
    private static final double W = 1600;
    private static final double H = 900;

    /**
     * 游戏图标（任务栏 / 标题栏 / Alt-Tab 都用它）。
     * 路径相对 resources/com/example/demo/，即
     * {@code demo/src/main/resources/com/example/demo/icon.png}。
     */
    private static final String APP_ICON = "/com/example/demo/icon.png";

    /** 地图两侧黑边宽度（像素）——想调黑边宽窄就改这个数 */
    private static final double MAP_SIDE_MARGIN = 190;

    /** 普通图例（example.png，485×483 近似正方）的显示边长 —— 想调大小改这个数 */
    private static final double LEGEND_W = 300;

    /**
     * 混沌图例（Chaosexample.png）的显示高度 —— 想调大小改这个数。
     *
     * <p><b>为什么混沌图例不按宽度缩：</b>那张图是 374×882 的竖图（一个举着图例牌的立绘）。
     * 如果沿用普通图例的 300×300 框去 fit，preserveRatio 只会把它压成 127×300 的一条细缝，
     * 牌子上「怪物 / 精英 / 事件 / 火堆 / 宝箱」全糊掉。所以混沌图例改成「按高度缩放」，
     * 宽度交给 preserveRatio 推（620 高 → 宽约 263px）。</p>
     */
    private static final double CHAOS_LEGEND_H = 620;

    /** 起点 NPC 给的候选遗物个数（从起点遗物池里随机抽这么多） */
    private static final int STARTER_RELIC_OPTIONS = 3;

    /**
     * 混沌（起点遗物）：非固定层节点进入时等可能地变成这五种房间之一。
     * 5 选 1 各 20%，顺序不影响概率，只是按「怪物 / 精英 / 事件 / 火堆 / 宝箱」排。
     */
    private static final GameMap.NodeType[] CHAOS_ROOMS = {
            GameMap.NodeType.MONSTER,
            GameMap.NodeType.ELITE,
            GameMap.NodeType.EVENT,
            GameMap.NodeType.REST,
            GameMap.NodeType.TREASURE,
    };

    /** 混沌掷房间用的随机源 */
    private static final java.util.Random CHAOS_RND = new java.util.Random();

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
        applyAppIcon(stage); // 贴上 icon.png，任务栏/标题栏显示游戏图标
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

    /**
     * 给窗口贴上游戏图标（{@link #APP_ICON}）。
     *
     * <p>JavaFX 只负责把图交给系统，缩放由 Windows 自己做，所以这里不预处理尺寸。
     * 图缺失时静默跳过 —— 窗口退回 Java 默认图标，不影响启动。</p>
     *
     * <p>本项目只有 {@code start(Stage)} 这一个 Stage，后面换场景都是换 Scene，
     * 图标挂在 Stage 上不会丢，所以贴这一次就够了。</p>
     */
    private void applyAppIcon(Stage stage) {
        var in = getClass().getResourceAsStream(APP_ICON);
        if (in == null) {
            System.out.println("[icon] 找不到 " + APP_ICON + "，使用默认窗口图标");
            return;
        }
        stage.getIcons().add(new Image(in));
    }

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

    /** 开始一局：新玩家 + 新地图 */
    private void startMap(Stage stage) {
        CardRewardPool.resetPity(); // 新的一局：怜悯偏移回归初始值
        Player player = new Player();
        GameMap map = GameMap.generate();
        showMapScene(stage, map, player);
    }

    /**
     * 地图场景（战斗后回同一张地图也用这个）。
     *
     * @return 本次新建的 HUD —— 调用方如果想在「切场景之后」再改玩家状态
     *         （比如遗物获取界面点完「拾取」），必须拿这个新 HUD 去 refresh()，
     *         刷旧的只会刷到已经被丢弃的节点。
     */
    private RunHud showMapScene(Stage stage, GameMap map, Player player) {
        SoundFx.play("map"); // 进入地图页音效
        MusicFx.playLoop("Level1", "bgm_map"); // 地图 BGM：Level1（没有就退回 bgm_map）
        // 地图页本身不显示右上角“地图”按钮
        RunHud hud = buildHud(player, map, () -> showWindow(page(mapPage(map, player))), false);
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
        view.setChaos(player.chaos); // 混沌（起点遗物）：非固定层节点全画成「事件」图标
        DevEntry.enableDevMap(view); // 开发者模式：任意节点都能点
        scroll.setContent(view);

        // 让地图两侧留黑边：地图限宽居中，两侧露出黑底（HUD 仍占满宽度）
        StackPane mapArea = new StackPane(scroll);
        mapArea.setStyle("-fx-background-color: black;");
        scroll.maxWidthProperty().bind(
                mapArea.widthProperty().subtract(MAP_SIDE_MARGIN * 2));

        // 右侧黑边处贴图例（混沌模式下换成 Chaosexample.png；宽度随黑边宽度自适应）
        Node legend = legendNode(player.chaos);
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
        return hud;
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
        // 战斗 BGM：小怪/精英用 Level1；BOSS 优先用专属曲，没有就沿用 Level1
        if (type == GameMap.NodeType.BOSS) {
            MusicFx.playLoop("bgm_boss", "Level1", "bgm_battle");
        } else {
            MusicFx.playLoop("Level1", "bgm_battle");
        }
        RunHud hud = buildHud(player, map, () -> openBattleMapReadOnly(stage, map, player), true);

        BattleView battle = new BattleView(player, hud, enemy,
                won -> {
            activeBattle = null;
            battleMapOpen = false;
            if (won) {
                // 精英战利品：只「挑」不「拿」。真正入账要等玩家在获取界面上点「拾取」，
                // 所以这里拿到的只是一个候选 —— 丢弃就什么都不发生。
                Relic eliteRelic = (type == GameMap.NodeType.ELITE)
                        ? RelicFun.pickEliteRelic(player)
                        : null;

                // 所有战斗胜利后都回到地图。新 HUD 是在遗物入账【之前】建的，
                // 所以点完「拾取」必须拿这里返回的 HUD 再 refresh() 一次，
                // 否则地图上的遗物栏不会更新（刷战斗场景那个 HUD 更是白刷）。
                RunHud mapHud = showMapScene(stage, map, player);

                if (type == GameMap.NodeType.BOSS) {
                    // Boss 战胜利：延迟返回主菜单，让玩家看到通关画面
                    PauseTransition delay = new PauseTransition(Duration.millis(2000));
                    delay.setOnFinished(e -> returnToMenu(stage));
                    delay.play();
                } else if (eliteRelic != null) {
                    offerRelic(stage, player, mapHud, eliteRelic);
                }
                // 普通怪物：直接回到地图，无额外操作
            } else {
                returnToMenu(stage);     // 阵亡
            }
        }, type == GameMap.NodeType.BOSS); // true=用 boss 战斗背景，否则 default 背景
        activeBattle = battle;

        // 开发者模式：战斗里也能改牌组/手牌/遗物（手牌改完立刻重画）
        DevEntry.attachDevButton(hud, player, battle, hud::refresh,
                node -> showWindow(page(node)), this::closeWindow);
        // 开发者模式：战斗 HUD 上再挂一个红色的「杀」，一键秒杀当前敌人
        DevEntry.attachKillButton(hud, battle);

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
    private void openBattleMapReadOnly(Stage stage, GameMap map, Player player) {
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
        preview.setChaos(player.chaos); // 混沌：只读地图也要显示同样的「事件」图标
        scroll.setContent(preview);

        // 同样的两侧黑边：地图限宽居中
        StackPane mapArea = new StackPane(scroll);
        mapArea.setStyle("-fx-background-color: black;");
        scroll.maxWidthProperty().bind(
                mapArea.widthProperty().subtract(MAP_SIDE_MARGIN * 2));

        // 右侧黑边处同样贴图例（混沌模式下换成 Chaosexample.png）
        Node legend2 = legendNode(player.chaos);
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

    /**
     * 右侧黑边上的图例。
     *
     * <p>普通模式贴 {@code example.png}；混沌模式（起点遗物「混沌」）换成
     * {@code Chaosexample.png} —— 一张画着「图例牌被划掉 + CHAOS!」的竖图。</p>
     *
     * <p>两张图比例差别很大（485×483 正方 vs 374×882 竖图），所以缩放策略也不同：
     * 普通图例按 300×300 的框缩，混沌图例按高度缩（见 {@link #CHAOS_LEGEND_H}）。</p>
     *
     * @param chaos true = 混沌模式，用 Chaosexample.png
     */
    private Node legendNode(boolean chaos) {
        var in = getClass().getResourceAsStream(chaos
                ? "/com/example/demo/icons/Chaosexample.png"
                : "/com/example/demo/icons/example.png");
        if (in == null) {
            // 混沌图缺失时退回普通图例（有总比没有强）；普通图也缺就干脆不显示
            return chaos ? legendNode(false) : null;
        }
        ImageView iv = new ImageView(new Image(in));
        iv.setPreserveRatio(true);
        iv.setMouseTransparent(true);
        if (chaos) {
            iv.setFitHeight(CHAOS_LEGEND_H); // 竖图：只定高，宽由比例推出来
        } else {
            iv.setFitWidth(LEGEND_W);
            iv.setFitHeight(LEGEND_W);
        }
        return iv;
    }

    // ================= 顶部 HUD（含三个整页窗口入口�?=================

    private RunHud buildHud(Player player, GameMap map, Runnable onMapClick, boolean showMapIcon) {
        return new RunHud(
                player,
                r -> showWindow(page(r.buildPage(this::closeWindow))),
                () -> showWindow(page(deckPage(player))),
                onMapClick,
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
    private VBox mapPage(GameMap map, Player player) {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // 隐藏滚动条（滚轮/拖动仍可用）
        scroll.setPannable(true);
        scroll.setStyle("-fx-background: #0b1020; -fx-background-color: #0b1020;");
        scroll.setPrefSize(1060, 430);

        MapView preview = new MapView(map, t -> { }, scroll, false); // 只读
        preview.setChaos(player.chaos); // 混沌：只读地图也要显示同样的「事件」图标
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

        Label name = new Label(c.name());
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

    /** 普通怪生成：具体规则见 {@link EnemyFactory#normal(int)} */
    // ================= 遗物获取（可拿可不拿） =================

    /**
     * 弹遗物获取界面：点「拾取」才真正入账并刷新 HUD，点「丢弃」什么都不做。
     *
     * <p>调用方必须先「只挑不拿」（{@link RelicFun#pickEliteRelic} /
     * {@link RelicFun#pickEventRelic}），把结果交给本方法，不要自己先 addRelic
     * —— 那样「丢弃」就成了先加后减，遗物栏和最大生命值都减不干净。</p>
     *
     * @param hud 遗物入账后要刷新的 HUD；传 null 表示不用刷
     */
    private void offerRelic(Stage stage, Player player, RunHud hud, Relic relic) {
        if (relic == null) return;
        RelicObtainToast.showChoice(stage.getScene(), relic, taken -> {
            if (!taken) return;                  // 丢弃：不入账，也不用刷新
            RelicFun.grantRelic(player, relic);  // 含草莓 +7 / 荔枝 +13 最大生命
            if (hud != null) hud.refresh();      // 右上角遗物栏 / 生命值同步
        });
    }

    private Enemy monsterForRow(GameMap map) {
        int row = map.current == null ? 0 : map.current.row;
        return EnemyFactory.normal(row);
    }

    private void handleArrive(Stage stage, GameMap map, Player player, RunHud hud,
                              GameMap.NodeType type) {
        // 混沌：非固定层节点在地图上都画成「事件」，但真正进哪个房间是等概率掷出来的。
        // 只覆盖非固定层 —— 起点 / 固定宝箱层 / 固定篝火层 / BOSS 层照旧按自身类型走。
        if (player.chaos && map.current != null && !GameMap.isFixedRow(map.current.row)) {
            type = CHAOS_ROOMS[CHAOS_RND.nextInt(CHAOS_ROOMS.length)];
        }
        switch (type) {
            case MONSTER -> startBattle(stage, map, player, GameMap.NodeType.MONSTER,
                    monsterForRow(map));
            case ELITE   -> startBattle(stage, map, player, GameMap.NodeType.ELITE, new GuardPig());
            case BOSS    -> startBattle(stage, map, player, GameMap.NodeType.BOSS, EnemyFactory.boss());
            case START   -> showRoomScene(stage, map, player);
            case EVENT   -> {
                List<EventDef> events = EventDef.pool();
                EventDef ev = events.get(new java.util.Random().nextInt(events.size()));
                startEventScene(stage, map, player, ev);
            }
            case REST    -> showRestScene(stage, map, player);
            case TREASURE -> {
                // 只挑不拿：等玩家在获取界面上点「拾取」才入账并刷新 HUD
                Relic gained = RelicFun.pickEliteRelic(player);
                if (gained != null) {
                    offerRelic(stage, player, hud, gained);
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("宝箱");
                    alert.setHeaderText(null);
                    alert.setContentText("宝箱里的遗物你已经有了……空空如也。");
                    alert.showAndWait();
                }
            }
        }
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
            EventOutcome out = applyEventOption(player, opt);
            if (player.hp() == 0) {
                Alert over = new Alert(Alert.AlertType.INFORMATION);
                over.setTitle("事件结果");
                over.setHeaderText(null);
                over.setContentText(out.message() + "\n\n你的生命归零……本局结束。");
                over.setOnHidden(e -> returnToMenu(stage));
                over.showAndWait();
            } else {
                Alert result = new Alert(Alert.AlertType.INFORMATION);
                result.setTitle("事件结果");
                result.setHeaderText(null);
                result.setContentText(out.message());
                // 结果框关掉后才回地图。事件给的遗物同样「可拿可不拿」，
                // 所以在切完场景之后再弹获取界面，并用新 HUD 刷遗物栏。
                result.setOnHidden(e -> {
                    RunHud mapHud = showMapScene(stage, map, player);
                    offerRelic(stage, player, mapHud, out.relic());
                });
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

    /**
     * 事件选项的结算结果：提示文字 + 一件「待玩家决定去留」的遗物（没有就为 null）。
     *
     * <p>遗物不在这里入账 —— 要等玩家在获取界面上点「拾取」。
     * 所以 ADD_RELIC 分支只「挑」不「拿」，把遗物交给调用方去弹界面。</p>
     */
    private record EventOutcome(String message, Relic relic) {}

    /** 结算事件选项的真实效果，返回提示文字（遗物只挑不拿，见 {@link EventOutcome}） */
    private EventOutcome applyEventOption(Player player, EventDef.Option opt) {
        return switch (opt.action) {
            case HEAL -> {
                int before = player.hp();
                player.heal(opt.amount);
                yield new EventOutcome(
                        "回复 " + opt.amount + " 点生命：" + before + " → " + player.hp(), null);
            }
            case DAMAGE -> {
                int before = player.hp();
                player.damage(opt.amount);
                yield new EventOutcome(
                        "失去 " + opt.amount + " 点生命：" + before + " → " + player.hp(), null);
            }
            case ADD_CARD -> {
                Card c = randomRewardCard();
                player.deck.add(c);
                yield new EventOutcome("获得卡牌：「" + c.name() + "」加入牌组（#" + c.id + "）", null);
            }
            case ADD_RELIC -> {
                // 只挑不拿：等玩家在获取界面上点「拾取」才入账
                Relic r = RelicFun.pickEventRelic(player);
                yield r == null
                        ? new EventOutcome("遗物池里已经没有新遗物了……", null)
                        : new EventOutcome("发现遗物：「" + r.name + "」\n" + r.desc, r);
            }
            case NOTHING -> new EventOutcome(opt.effectDesc + "（无事发生）", null);
        };
    }

    /** 篝火（休息）节点：休息恢复 30% 最大生命，或强化一张牌（二选一） */
    private void showRestScene(Stage stage, GameMap map, Player player) {
        RestView rest = new RestView(player, () -> showMapScene(stage, map, player));

        Scene scene = sizedScene(stage, rest);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                returnToMenu(stage);
            }
        });
        stage.setScene(scene);
    }

    /** 起点房间：NPC + 三选一初始遗物 */
    private void showRoomScene(Stage stage, GameMap map, Player player) {
        // 从「起点遗物池」里随机抽 3 个当候选（不再把整个池子全列出来）
        List<Relic> starters = RelicFun.pickStarterOptions(player, STARTER_RELIC_OPTIONS);

        // 猪神的随机台词池（每次进房间随机一句；点对话框还能再换一句）
        List<String> npcLines = List.of(
                "猪，你未到校，也未请假。若10点前未及时到场，将不再是迟到，而记为旷课。收到速回",
                "又迟到了？猪塔的台阶可不会等你。挑一件东西带上，往上爬吧。",
                "塔里住着鱼龙公爵，它最讨厌迟到的猪。我不拦你，但别空着手上去。",
                "这三件玩意儿是我从学生处顺来的，挑一个，剩下的我还得还回去。",
                "上塔之前想清楚：格挡、抽牌、回血——你缺哪一样，就挑哪一样。",
                "猪，记住：在猪塔里，犹豫的猪会先掉血。"
        );

        RoomView room = new RoomView(
                player, starters, "猪神", npcLines,
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
