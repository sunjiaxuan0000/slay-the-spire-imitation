package com.example.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class HelloApplication extends Application {

    /** 默认窗口分辨率（1280×720 = 16:9，和主菜单背景图同比例，铺满零裁切） */
    private static final double W = 1280;
    private static final double H = 720;

    @Override
    public void start(Stage stage) {
        stage.setTitle("杀戮猪塔");
        stage.setScene(buildMenuScene(stage));
        stage.show();

        // 点击窗口右上角关闭按钮(X)时，弹出退出确认（所有界面都生效）
        stage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("退出确认");
            alert.setHeaderText(null);
            alert.setContentText("确定要退出程序吗？");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                // 点击“取消”或直接关掉对话框 → 阻止窗口关闭
                event.consume();
            }
            // 点击“确定” → 什么都不用做，窗口按默认方式关闭、程序退出
        });
    }

    // ================= 场景切换 =================

    /** 主菜单场景：一启动先看到这里。 */
    private Scene buildMenuScene(Stage stage) {
        MainMenu menu = new MainMenu(
                () -> startCharacterSelect(stage), // 点“开始游戏”→ 角色选择界面
                () -> stage.close()                // 点“退出游戏”→ 关闭窗口
        );
        Scene scene = new Scene(menu, W, H);

        // 菜单调试快捷键：方向键微调开始按钮（按住 Shift = 1px 微调，否则 10px）
        // F3 = 显示/隐藏按钮青色框，方便肉眼对齐
        scene.setOnKeyPressed(e -> {
            double step = e.isShiftDown() ? 1 : 10;
            switch (e.getCode()) {
                case UP    -> menu.nudge(0, -step);
                case DOWN  -> menu.nudge(0, step);
                case LEFT  -> menu.nudge(-step, 0);
                case RIGHT -> menu.nudge(step, 0);
                case F3    -> menu.toggleDebug();
                default    -> { }
            }
        });
        return scene;
    }

    /** 角色选择场景，Esc 返回主菜单。 */
    private void startCharacterSelect(Stage stage) {
        CharacterSelect select = new CharacterSelect(
                () -> startMap(stage) // 点右下角 ▶ → 开始一局
        );
        Scene scene = new Scene(select, W, H);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.setScene(buildMenuScene(stage));
            }
        });
        stage.setScene(scene);
    }

    /** 开始一局：新建玩家 + 随机地图。 */
    private void startMap(Stage stage) {
        Player player = new Player();
        GameMap map = GameMap.generate();
        showMapScene(stage, map, player);
    }

    /**
     * 地图场景：同一张地图/同一个玩家在战斗后还要回来，所以单独抽成方法。
     * Esc = 放弃本局回主菜单。
     */
    private void showMapScene(Stage stage, GameMap map, Player player) {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background: #0b1020; -fx-background-color: #0b1020;");

        RunHud hud = new RunHud(player);
        MapView view = new MapView(map,
                type -> handleArrive(stage, map, player, type), scroll);
        scroll.setContent(view);

        BorderPane root = new BorderPane();
        root.setTop(hud);
        root.setCenter(scroll);

        Scene scene = new Scene(root, W, H);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.setScene(buildMenuScene(stage)); // 放弃本局
            }
        });

        javafx.application.Platform.runLater(() -> scroll.setVvalue(1.0));
        stage.setScene(scene);
    }

    /** 走上地图节点后发生的事。 */
    private void handleArrive(Stage stage, GameMap map, Player player, GameMap.NodeType type) {
        switch (type) {
            case MONSTER -> startBattle(stage, map, player, GameMap.NodeType.MONSTER, Enemy.slime());
            case ELITE   -> startBattle(stage, map, player, GameMap.NodeType.ELITE, Enemy.eliteSlime());
            case BOSS    -> startBattle(stage, map, player, GameMap.NodeType.BOSS, Enemy.boss());
            case START   -> showRoomScene(stage, map, player); // 起点：选初始遗物
            case REST, TREASURE -> showNodeAlert(player, type);
        }
    }

    /** 起点房间：NPC 猪神对话 + 三选一初始遗物，选完才出现“离开房间”。 */
    private void showRoomScene(Stage stage, GameMap map, Player player) {
        List<Relic> starters = List.of(
                new Relic("青铜怀表", "战斗开始时获得 2 点格挡"),
                new Relic("请假条", "本局豁免一次“旷课”（占位，暂无效果）"),
                new Relic("保温杯", "每次休息额外恢复 10 点生命")
        );

        RoomView room = new RoomView(
                player,
                starters,
                "猪神",
                "猪，你未到校，也未请假。若10点前未及时到场，将不再是迟到，而记为旷课。收到速回",
                () -> showMapScene(stage, map, player) // 离开房间 → 回地图
        );

        Scene scene = new Scene(room, W, H);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.setScene(buildMenuScene(stage)); // 放弃本局
            }
        });
        stage.setScene(scene);
    }

    /** 非战斗节点：休息(真回血)/宝箱/起点，弹个信息窗。 */
    private void showNodeAlert(Player player, GameMap.NodeType type) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("节点事件");
        alert.setHeaderText(null);
        switch (type) {
            case REST -> {
                int before = player.hp();
                player.heal(player.maxHp);
                alert.setContentText("你点燃篝火休息，生命 " + before + " → " + player.hp());
            }
            case TREASURE -> alert.setContentText("你打开宝箱，获得一件遗物（演示）。");
            default       -> { }
        }
        alert.showAndWait();
    }

    /** 战斗场景：胜利后回地图（BOSS 通关则回主菜单），失败回主菜单。 */
    private void startBattle(Stage stage, GameMap map, Player player,
                             GameMap.NodeType type, Enemy enemy) {
        RunHud hud = new RunHud(player);
        BattleView battle = new BattleView(player, hud, enemy, won -> {
            if (won) {
                if (type == GameMap.NodeType.BOSS) {
                    stage.setScene(buildMenuScene(stage)); // 通关
                } else {
                    showMapScene(stage, map, player);      // 回同一张地图
                }
            } else {
                stage.setScene(buildMenuScene(stage));     // 阵亡，本局结束
            }
        });

        BorderPane root = new BorderPane();
        root.setTop(hud);
        root.setCenter(battle);

        Scene scene = new Scene(root, W, H);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.setScene(buildMenuScene(stage)); // 逃跑也算放弃本局
            }
        });
        stage.setScene(scene);
    }
}
