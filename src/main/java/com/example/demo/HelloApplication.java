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
import com.example.demo.save.SaveData;
import com.example.demo.view.CardFlyFx;
import com.example.demo.view.ConfirmOverlay;
import com.example.demo.view.GameMap;
import com.example.demo.view.MainMenu;
import com.example.demo.view.RewardOverlay;
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
import javafx.event.EventHandler;
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
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
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

    /** 当前场景的“整页窗口”宿主：最上层放半透明遮罩 + 居中大面�?*/
    private StackPane overlayHost;
    private BattleView activeBattle = null; // 进行中的战斗（供只读地图暂停/恢复�?
    private boolean battleMapOpen = false;  // 战斗里是否开着“只读地图�?

    /**
     * 切场景：<b>复用同一个 Scene，只换它的 root</b>。
     *
     * <p>⚠ 绝不能用 {@code Stage.setScene()} —— 它会按新场景的尺寸给窗口重新定大小，
     * 而给窗口重新定大小<b>会把全屏挤掉</b>（先退出全屏、再补回来，肉眼就是
     * 「闪一下再回到全屏」）。{@code Scene.setRoot()} 只替换内容，窗口尺寸一点不动，
     * 全屏自然完全不受影响。</p>
     *
     * <p>代价：按键处理器挂在 Scene 上、不会跟着 root 走，所以必须由调用方传进来。
     * 每次切换都会覆盖掉上一个场景的处理器；传 {@code null} 表示该场景不响应按键。</p>
     *
     * @param root  新场景的内容
     * @param onKey 该场景的按键处理器（可为 null）
     */
    private void switchScene(Stage stage, Parent root, EventHandler<? super KeyEvent> onKey) {
        Scene cur = stage.getScene();
        if (cur == null) {
            // 第一次（start 里）：还没有 Scene，建一个并定下初始窗口尺寸
            cur = decorate(new Scene(root, W, H));
            stage.setScene(cur);
        } else {
            cur.setRoot(root); // 只换内容：窗口尺寸不变 → 全屏不会被挤掉
        }
        cur.setOnKeyPressed(onKey);
    }

    /** 场景通用的 Esc：返回主菜单 */
    private EventHandler<? super KeyEvent> escToMenu(Stage stage) {
        return e -> {
            if (e.getCode() == KeyCode.ESCAPE) returnToMenu(stage);
        };
    }

    /** 主菜单的按键：方向键微调 / TAB 切换选中项 / F3 调试框 */
    private static EventHandler<? super KeyEvent> menuKeys(MainMenu menu) {
        return e -> {
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
        };
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
        // 关掉 JavaFX 默认的「Esc 退出全屏」—— 各场景的 Esc 都有自己的用处
        // （返回主菜单 / 返回战斗），不然全屏下按 Esc 会同时退出全屏 + 跳场景。
        // 想退出全屏请走设置页的「全屏」开关。
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
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

    /**
     * 主菜单内容（按键处理器见 {@link #menuKeys(MainMenu)}，
     * 由 {@link #switchScene} 挂到复用的 Scene 上）。
     */
    private MainMenu buildMenu(Stage stage) {
        MusicFx.playLoop("bgm_menu"); // 主菜单 BGM（没放 bgm_menu.wav 就静音）
        // 有存档时：「开始游戏」= 回到存档里那个节点继续；旁边再多一个「放弃上局游戏」。
        // 没存档时：「开始游戏」= 正常走选人 → 开新局，放弃按钮不出现。
        boolean hasSave = SaveData.exists();
        return new MainMenu(
                () -> {
                    if (SaveData.exists()) resumeRun(stage);
                    else startCharacterSelect(stage);
                },
                () -> showSettingsScene(stage),
                () -> stage.close(),
                hasSave ? () -> confirmAbandon(stage) : null
        );
    }

    /**
     * 「放弃上局游戏」：先确认，再删档。
     *
     * <p>删了就真没了，所以必须过一道确认；确认框里带上存档摘要，
     * 让玩家看清要放弃的到底是哪一局。</p>
     */
    private void confirmAbandon(Stage stage) {
        SaveData s = SaveData.read();
        String detail = (s == null) ? "存档已损坏，将直接清除。" : s.summary();

        // 场景内弹窗，不用 Alert：和游戏画风一致，也不会踩
        // 「动画 / 布局处理中不能 showAndWait」那颗雷。
        ConfirmOverlay confirm = new ConfirmOverlay(
                "放弃上局游戏",
                "确定要放弃当前这一局吗？\n\n" + detail + "\n\n放弃后存档会被删除，无法恢复。",
                "放弃这一局", "再想想",
                () -> {
                    SaveData.delete();
                    returnToMenu(stage); // 重画主菜单：放弃按钮自己消失
                },
                null);

        // 主菜单本身就是场景 root（且是个 Pane），直接往上挂
        if (stage.getScene().getRoot() instanceof Pane menu) {
            confirm.show(menu);
        }
    }

    /**
     * 回到主菜单。
     *
     * <p>不再强制恢复任何「记忆中的分辨率」—— 窗口尺寸全程由用户自己拖动决定；
     * 而且切场景只换 Scene 的 root（{@link #switchScene}），窗口尺寸一点不会变。</p>
     */
    private void returnToMenu(Stage stage) {
        MainMenu menu = buildMenu(stage);
        switchScene(stage, menu, menuKeys(menu));
    }

    /** 设置页面：音乐/音效音量 + 开发者模式开关 + 全屏；点“返回主菜单”或按 Esc 回去 */
    private void showSettingsScene(Stage stage) {
        SettingsView settings = new SettingsView(() -> returnToMenu(stage), stage);
        switchScene(stage, settings, escToMenu(stage));
    }

    /** 角色选择场景 */
    private void startCharacterSelect(Stage stage) {
        CharacterSelect select = new CharacterSelect(
                () -> startMap(stage)
        );
        switchScene(stage, select, escToMenu(stage));
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

        StackPane root = wrapOverlay(content);
        switchScene(stage, root, e -> {
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
        // 回到地图 = 这个节点已经过了：存档刷成「已清」。
        // 这样退出重进会直接站回地图上，而不是把刚打完的这一战重打一遍。
        // ⚠ 例外：读档回到「胜利选牌页」时会先调这里，那个流程会紧接着把阶段改回 REWARD。
        markCleared(map, player);
        return hud;
    }

    /** 开场演出：先显示“第一阶段 / 猪塔底”，再从地图顶部(BOSS)一路滑到底（起点）展示全图 */
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
                    // Boss 战胜利：这一局打完了，存档作废
                    SaveData.delete();
                    // 延迟返回主菜单，让玩家看到通关画面
                    PauseTransition delay = new PauseTransition(Duration.millis(2000));
                    delay.setOnFinished(e -> returnToMenu(stage));
                    delay.play();
                } else if (eliteRelic != null) {
                    // 精英战利品入账后再刷一次存档，否则读档回来这件遗物就蒸发了
                    offerRelic(stage, player, mapHud, eliteRelic, () -> markCleared(map, player));
                }
                // 普通怪物：直接回到地图，无额外操作
            } else {
                SaveData.delete();       // 阵亡：这一局结束，存档作废
                returnToMenu(stage);
            }
        }, type == GameMap.NodeType.BOSS); // true=用 boss 战斗背景，否则 default 背景
        activeBattle = battle;

        // 战斗胜利、三张奖励牌刚抽好 → 存档记成「已胜利，待领奖励」。
        // 读档会直接回到这个选牌页（而且还是同样的三张牌），不用把这一战重打一遍。
        battle.setOnRewardOffers(offers ->
                SaveData.capture(map, player, type, enemy.name, "", SaveData.Phase.REWARD, offers));

        // 开发者模式：战斗里也能改牌组/手牌/遗物（手牌改完立刻重画）
        DevEntry.attachDevButton(hud, player, battle, hud::refresh,
                node -> showWindow(page(node)), this::closeWindow);
        // 开发者模式：战斗 HUD 上再挂一个红色的「杀」，一键秒杀当前敌人
        DevEntry.attachKillButton(hud, battle);

        BorderPane content = new BorderPane();
        content.setTop(hud);
        content.setCenter(battle);

        StackPane root = wrapOverlay(content);
        switchScene(stage, root, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                if (isWindowOpen()) {
                    closeBattleMapOrWindow(); // 开着只读地图/牌组窗口 �?先关，不退出战�?
                } else {
                    returnToMenu(stage); // 逃跑=放弃本局
                }
            }
        });
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
    private StackPane wrapOverlay(Node content) {
        overlayHost = new StackPane();
        overlayHost.setMouseTransparent(true); // 平时不挡鼠标（没开窗口时不拦截点击�?
        StackPane root = new StackPane();
        root.getChildren().addAll(content, overlayHost);
        return root; // 只给 root，场景由 switchScene 复用（不新建 Scene → 不动窗口尺寸）
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
        offerRelic(stage, player, hud, relic, null);
    }

    /**
     * 遗物获取界面（可拾取、可丢弃）。
     *
     * @param onResolved 界面出结果后（<b>拾取或丢弃都算</b>）要跑的收尾，
     *                   通常是把存档刷成「这个节点已经处理完了」
     */
    private void offerRelic(Stage stage, Player player, RunHud hud, Relic relic,
                            Runnable onResolved) {
        if (relic == null) return;
        RelicObtainToast.showChoice(stage.getScene(), relic, taken -> {
            if (taken) {
                RelicFun.grantRelic(player, relic);  // 含草莓 +7 / 荔枝 +13 最大生命
                if (hud != null) hud.refresh();      // 右上角遗物栏 / 生命值同步
            }
            if (onResolved != null) onResolved.run();
        });
    }

    /**
     * 把存档刷成「当前节点已经过了」—— 读档时直接回到地图，不用把这一战重打一遍。
     *
     * <p>⚠ 只在这个节点<b>确实处理完了</b>之后调用（战斗领完奖励 / 事件选完 /
     * 篝火用完 / 宝箱开完）。提前调用会让玩家白白丢掉还没领的奖励。</p>
     */
    private static void markCleared(GameMap map, Player player) {
        if (map == null || player == null || map.current == null) return;
        SaveData.capture(map, player, map.current.type, "", "", SaveData.Phase.CLEARED, null);
    }

    private Enemy monsterForRow(GameMap map) {
        int row = map.current == null ? 0 : map.current.row;
        return EnemyFactory.normal(row);
    }

    /**
     * 玩家在地图上点了一个可达节点：判定房间类型（含混沌改判）后进入。
     *
     * <p>{@code MapView} 在调用这里之前已经把 {@code map.current} 指到该节点了。</p>
     */
    private void handleArrive(Stage stage, GameMap map, Player player, RunHud hud,
                              GameMap.NodeType type) {
        // 混沌：非固定层节点在地图上都画成「事件」，但真正进哪个房间是等概率掷出来的。
        // 只覆盖非固定层 —— 起点 / 固定宝箱层 / 固定篝火层 / BOSS 层照旧按自身类型走。
        if (player.chaos && map.current != null && !GameMap.isFixedRow(map.current.row)) {
            type = CHAOS_ROOMS[CHAOS_RND.nextInt(CHAOS_ROOMS.length)];
        }
        enterNode(stage, map, player, hud, type, null, null);
    }

    /**
     * 进入一个节点：<b>先存档，再进房间</b>。
     *
     * <p>这是全局唯一的「进节点」入口 —— 地图上点节点（{@link #handleArrive}）和
     * 读档续命（{@link #resumeRun}）都走这里，所以存档点只需要维护一处；
     * 读档时进房间会把存档原样重写一遍，内容不变。</p>
     *
     * <p>存档记的是「刚进入这个节点」的状态：战斗从头打、事件重新选，
     * 但玩家的血量 / 牌组 / 遗物和所在节点都和离开时一模一样。</p>
     *
     * @param presetEnemy 读档时指定的敌人；新进节点传 {@code null} → 按层随机
     * @param presetEvent 读档时指定的事件；新进节点传 {@code null} → 随机挑一个
     */
    private void enterNode(Stage stage, GameMap map, Player player, RunHud hud,
                           GameMap.NodeType type, Enemy presetEnemy, EventDef presetEvent) {
        Enemy enemy = presetEnemy;
        EventDef ev = presetEvent;

        // 先把「这次进去会遇到什么」定下来，才能一起存进档里
        switch (type) {
            case MONSTER -> { if (enemy == null) enemy = monsterForRow(map); }
            case ELITE   -> { if (enemy == null) enemy = new GuardPig(); }
            // BOSS 按地图定好的种类出（和地图上画的那只对得上），不再临场随机
            case BOSS    -> { if (enemy == null) enemy = EnemyFactory.boss(map.bossKind); }
            case EVENT   -> {
                if (ev == null) {
                    List<EventDef> pool = EventDef.pool();
                    ev = pool.get(new java.util.Random().nextInt(pool.size()));
                }
            }
            default -> { }
        }

        // ★ 存档点：进入节点的这一刻落盘
        SaveData.capture(map, player, type,
                enemy == null ? "" : enemy.name,
                ev == null ? "" : ev.name);

        switch (type) {
            case MONSTER -> startBattle(stage, map, player, GameMap.NodeType.MONSTER, enemy);
            case ELITE   -> startBattle(stage, map, player, GameMap.NodeType.ELITE, enemy);
            case BOSS    -> startBattle(stage, map, player, GameMap.NodeType.BOSS, enemy);
            case START   -> showRoomScene(stage, map, player);
            case EVENT   -> startEventScene(stage, map, player, ev);
            case REST    -> showRestScene(stage, map, player);
            case TREASURE -> {
                // 只挑不拿：等玩家在获取界面上点「拾取」才入账
                Relic gained = RelicFun.pickEliteRelic(player);
                if (gained != null) {
                    offerRelic(stage, player, hud, gained, () -> markCleared(map, player));
                } else {
                    // 池子里没有新遗物了：不再弹提示框，直接算这个宝箱处理完
                    markCleared(map, player);
                }
            }
        }
    }

    /**
     * 读档继续：重建地图和玩家，直接回到「当时进入的那个节点」。
     *
     * <p>存档不存在、或读出来是坏的 → 退回正常开新局，不让玩家卡在坏档上。</p>
     */
    private void resumeRun(Stage stage) {
        SaveData s = SaveData.read();
        if (s == null) {
            startMap(stage); // 没存档 / 存档坏了：当新的一局开
            return;
        }

        CardRewardPool.resetPity();
        GameMap map = s.buildMap();
        Player player = s.buildPlayer();
        if (map.current == null) {
            // 坐标对不上（比如以后改了地图生成规则）：清掉坏档，重开一局
            SaveData.delete();
            startMap(stage);
            return;
        }

        GameMap.NodeType type = s.resolvedType(map);
        switch (s.phase) {
            // 这个节点已经过了：直接站回地图上，继续往上走
            case CLEARED -> showMapScene(stage, map, player);
            // 战斗已经打赢：回到「胜利后的选牌页」
            case REWARD -> resumeReward(stage, map, player, type, s);
            // 刚进节点：重进这个房间（战斗从头打 / 事件重新选）
            case ENTER -> {
                Enemy enemy = null;
                EventDef ev = null;
                switch (type) {
                    // BOSS 用地图的种类（和节点图标一致），普通怪/精英按存档里记的名字还原
                    case BOSS -> enemy = EnemyFactory.boss(map.bossKind);
                    case MONSTER, ELITE -> enemy =
                            EnemyFactory.named(s.enemy, map.current.row, false);
                    case EVENT -> ev = findEvent(s.event);
                    default -> { }
                }
                // hud 传 null：宝箱房要弹遗物界面，而刚读档还没有 HUD —— offerRelic 允许 hud 为 null，
                // 只是不刷新遗物栏；玩家下一次进地图就会带着正确的 HUD 重画。
                enterNode(stage, map, player, null, type, enemy, ev);
            }
        }
    }

    /**
     * 读档回到「战斗胜利后的选牌页」。
     *
     * <p>⚠ <b>顺序不能反</b>：{@link #showMapScene} 会把存档刷成 CLEARED，所以必须在它
     * <b>之后</b>再把阶段改回 REWARD —— 否则玩家在选牌页直接退出，这张奖励就读不回来了。</p>
     */
    private void resumeReward(Stage stage, GameMap map, Player player,
                              GameMap.NodeType type, SaveData s) {
        RunHud hud = showMapScene(stage, map, player); // 先回地图（HUD 也在这里建好）

        List<Card> offers = s.buildRewardCards();
        if (offers.isEmpty()) {
            // 没记奖励牌的旧存档：现抽三张兜底，不至于把玩家卡在空选牌页上
            offers = CardRewardPool.draw(CardRewardPool.rewardPool(), 3,
                    type == GameMap.NodeType.ELITE);
        }
        SaveData.capture(map, player, type, "", "", SaveData.Phase.REWARD, offers);

        RewardOverlay overlay = new RewardOverlay(() -> {
            closeWindow();
            afterBattleReward(stage, map, player, hud, type);
        });
        showWindow(overlay); // 叠在地图页上（overlayHost 由 showMapScene 建好）
        overlay.show(offers, (c, node) -> {
            player.deck.add(c);
            hud.refresh();
            overlay.hide();
            CardFlyFx.flyIntoDeck(stage.getScene(), node, hud.getDeckIcon(), c, () -> {
                closeWindow();
                afterBattleReward(stage, map, player, hud, type);
            });
        });
    }

    /**
     * 领完（或跳过）卡牌奖励之后的收尾：节点标记成「已过」，再补上精英战利品。
     *
     * <p>正常战斗胜利的 {@code won} 回调、和读档续上的 {@link #resumeReward}，
     * 最后都归到这一套逻辑，免得两条路走岔。</p>
     */
    private void afterBattleReward(Stage stage, GameMap map, Player player, RunHud hud,
                                   GameMap.NodeType type) {
        markCleared(map, player);
        if (type == GameMap.NodeType.BOSS) {
            SaveData.delete(); // 通关了，这一局结束
            PauseTransition delay = new PauseTransition(Duration.millis(2000));
            delay.setOnFinished(e -> returnToMenu(stage));
            delay.play();
            return;
        }
        if (type == GameMap.NodeType.ELITE) {
            Relic eliteRelic = RelicFun.pickEliteRelic(player);
            offerRelic(stage, player, hud, eliteRelic, () -> markCleared(map, player));
        }
    }

    /** 按名字找回存档里的事件；找不到（或旧存档没记）就随机挑一个，和「新进节点」一致。 */
    private static EventDef findEvent(String name) {
        List<EventDef> pool = EventDef.pool();
        if (name != null && !name.isEmpty()) {
            for (EventDef e : pool) {
                if (e.name.equals(name)) return e;
            }
        }
        return pool.get(new java.util.Random().nextInt(pool.size()));
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
                // 事件里把血扣光了：这一局结束，存档作废
                SaveData.delete();
                Alert over = new Alert(Alert.AlertType.INFORMATION);
                over.setTitle("事件结果");
                over.setHeaderText(null);
                over.setContentText(out.message() + "\n\n你的生命归零……本局结束。");
                over.setOnHidden(e -> returnToMenu(stage));
                over.showAndWait();
            } else {
                // 不再弹「事件结果」的 Alert：选完直接回地图。
                // 事件给的遗物同样「可拿可不拿」，所以切完场景之后再弹获取界面，
                // 并用新 HUD 刷遗物栏。
                RunHud mapHud = showMapScene(stage, map, player);

                // 事件给了卡牌：复用「获得卡牌 → 飞入牌组」演出（和战斗奖励同一套）。
                // ⚠ 必须等回到地图才有牌组图标（事件页本身没有 HUD），所以放在 showMapScene 之后；
                //    再包一层 runLater 等新场景完成一次布局，否则图标尺寸还是 0，飞落点会算错。
                if (out.gainedCard() != null) {
                    Platform.runLater(() -> {
                        Scene sc = stage.getScene();
                        if (sc == null) return;
                        CardFlyFx.flyIntoDeck(sc, sc.getRoot(), mapHud.getDeckIcon(),
                                out.gainedCard(), null);
                    });
                }

                offerRelic(stage, player, mapHud, out.relic(),
                        () -> markCleared(map, player));
            }
        });

        switchScene(stage, view, escToMenu(stage));
    }

    /**
     * 事件选项的结算结果：提示文字 + 一件「待玩家决定去留」的遗物（没有就为 null）。
     *
     * <p>遗物不在这里入账 —— 要等玩家在获取界面上点「拾取」。
     * 所以 ADD_RELIC 分支只「挑」不「拿」，把遗物交给调用方去弹界面。</p>
     */
    private record EventOutcome(String message, Relic relic, Card gainedCard) {}

    /** 结算事件选项的真实效果，返回提示文字（遗物只挑不拿，见 {@link EventOutcome}） */
    private EventOutcome applyEventOption(Player player, EventDef.Option opt) {
        return switch (opt.action) {
            case HEAL -> {
                int before = player.hp();
                player.heal(opt.amount);
                yield new EventOutcome(
                        "回复 " + opt.amount + " 点生命：" + before + " → " + player.hp(), null, null);
            }
            case DAMAGE -> {
                int before = player.hp();
                player.damage(opt.amount);
                yield new EventOutcome(
                        "失去 " + opt.amount + " 点生命：" + before + " → " + player.hp(), null, null);
            }
            case ADD_CARD -> {
                // 数据照常即时入账；「飞入牌组」的演出交给调用方（要等回到地图才有牌组图标）
                Card c = randomRewardCard();
                player.deck.add(c);
                yield new EventOutcome("获得卡牌：「" + c.name() + "」加入牌组（#" + c.id + "）", null, c);
            }
            case ADD_RELIC -> {
                // 只挑不拿：等玩家在获取界面上点「拾取」才入账
                Relic r = RelicFun.pickEventRelic(player);
                yield r == null
                        ? new EventOutcome("遗物池里已经没有新遗物了……", null, null)
                        : new EventOutcome("发现遗物：「" + r.name + "」\n" + r.desc, r, null);
            }
            case NOTHING -> new EventOutcome(opt.effectDesc + "（无事发生）", null, null);
        };
    }

    /** 篝火（休息）节点：休息恢复 30% 最大生命，或强化一张牌（二选一） */
    private void showRestScene(Stage stage, GameMap map, Player player) {
        RestView rest = new RestView(player, () -> showMapScene(stage, map, player));

        switchScene(stage, rest, escToMenu(stage));
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
                player, starters, "“猪神”", npcLines,
                () -> showMapScene(stage, map, player)
        );

        switchScene(stage, room, escToMenu(stage));
    }
}
