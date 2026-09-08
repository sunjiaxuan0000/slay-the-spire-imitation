package com.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class HelloApplication extends Application {

    /** 默认窗口分辨率（1280×720 = 16:9，和主菜单背景图同比例，铺满零裁切） */
    private static final double W = 1280;
    private static final double H = 720;

    /** 当前场景的“整页窗口”宿主：最上层放半透明遮罩 + 居中大面板 */
    private StackPane overlayHost;

    @Override
    public void start(Stage stage) {
        stage.setTitle("我的游戏");
        stage.setScene(buildMenuScene(stage));
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

    /** 主菜单场景 */
    private Scene buildMenuScene(Stage stage) {
        MainMenu menu = new MainMenu(
                () -> startCharacterSelect(stage),
                () -> stage.close()
        );
        Scene scene = new Scene(menu, W, H);
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

    /** 角色选择场景 */
    private void startCharacterSelect(Stage stage) {
        CharacterSelect select = new CharacterSelect(
                () -> startMap(stage)
        );
        Scene scene = new Scene(select, W, H);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.setScene(buildMenuScene(stage));
            }
        });
        stage.setScene(scene);
    }

    /** 开始一局：新玩家 + 新地图 */
    private void startMap(Stage stage) {
        Player player = new Player();
        GameMap map = GameMap.generate();
        showMapScene(stage, map, player);
    }

    /** 地图场景（战斗后回同一张地图也用这个） */
    private void showMapScene(Stage stage, GameMap map, Player player) {
        RunHud hud = buildHud(player, map);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background: #0b1020; -fx-background-color: #0b1020;");

        MapView view = new MapView(map,
                type -> handleArrive(stage, map, player, hud, type), scroll, true);
        scroll.setContent(view);

        BorderPane content = new BorderPane();
        content.setTop(hud);
        content.setCenter(scroll);

        Scene scene = wrapOverlay(content);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.setScene(buildMenuScene(stage));
            }
        });
        Platform.runLater(() -> scroll.setVvalue(1.0));
        stage.setScene(scene);
    }

    /** 战斗场景：顶部 HUD + 战斗主体，外面再包整页窗口层 */
    private void startBattle(Stage stage, GameMap map, Player player,
                             GameMap.NodeType type, Enemy enemy) {
        RunHud hud = buildHud(player, map);
        BattleView battle = new BattleView(player, hud, enemy, won -> {
            if (won) {
                if (type == GameMap.NodeType.BOSS) {
                    stage.setScene(buildMenuScene(stage)); // 通关
                } else {
                    showMapScene(stage, map, player);
                }
            } else {
                stage.setScene(buildMenuScene(stage));     // 阵亡
            }
        });

        BorderPane content = new BorderPane();
        content.setTop(hud);
        content.setCenter(battle);

        Scene scene = wrapOverlay(content);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.setScene(buildMenuScene(stage)); // 逃跑=放弃本局
            }
        });
        stage.setScene(scene);
    }

    /** 把普通场景内容包一层：下面内容，上面是整页窗口层 */
    private Scene wrapOverlay(Node content) {
        overlayHost = new StackPane();
        overlayHost.setMouseTransparent(true); // 平时不挡鼠标（没开窗口时不拦截点击）
        StackPane root = new StackPane();
        root.getChildren().addAll(content, overlayHost);
        return new Scene(root, W, H);
    }

    // ================= 顶部 HUD（含三个整页窗口入口） =================

    private RunHud buildHud(Player player, GameMap map) {
        return new RunHud(
                player,
                r -> showWindow(page(relicPage(r))),   // 点遗物图标
                () -> showWindow(page(deckPage(player))), // 点牌组图标
                () -> showWindow(page(mapPage(map)))     // 点地图图标
        );
    }

    // ================= 整页窗口机制 =================

    /** 打开一个整页窗口（清掉旧的） */
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
        overlayHost.setMouseTransparent(true); // 关掉后恢复“不挡鼠标”
    }

    // ================= 三个整页窗口 =================

    /** 牌组页：所有牌按 id 排列 */
    private VBox deckPage(Player player) {
        List<Card> sorted = new ArrayList<>(player.deck);
        sorted.sort(Comparator.comparingInt(c -> c.id));

        FlowPane cards = new FlowPane(8, 8);
        cards.setPrefWrapLength(900);
        for (Card c : sorted) {
            cards.getChildren().add(miniCard(c));
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

    /** 地图页：战斗中也能查看的整页大地图（只读） */
    private VBox mapPage(GameMap map) {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
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

    /** 遗物页：大图标 + 详细介绍 */
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
            case STRIKE -> "#991b1b";
            case DEFEND -> "#1d4ed8";
            case BASH   -> "#b45309";
            case HEAVY  -> "#7c2d12";
            case IRON   -> "#334155";
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
            case MONSTER -> startBattle(stage, map, player, GameMap.NodeType.MONSTER, Enemy.slime());
            case ELITE   -> startBattle(stage, map, player, GameMap.NodeType.ELITE, Enemy.eliteSlime());
            case BOSS    -> startBattle(stage, map, player, GameMap.NodeType.BOSS, Enemy.boss());
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
        List<Relic> pool = new ArrayList<>(List.of(
                new Relic("青铜怀表", "战斗开始时获得 2 点格挡"),
                new Relic("请假条", "每回合多抽 1 张牌"),
                new Relic("保温杯", "每场战斗开始时恢复 10 点生命")
        ));
        pool.removeIf(r -> player.relics.stream().anyMatch(h -> h.name.equals(r.name)));
        if (pool.isEmpty()) return null;
        int idx = new java.util.Random().nextInt(pool.size());
        Relic gained = pool.get(idx);
        player.addRelic(gained);
        return gained;
    }

    /** 随机奖励一张卡（事件/奖励用） */
    private Card randomRewardCard() {
        List<Card> pool = List.of(
                Card.strike(), Card.defend(), Card.bash(),
                Card.heavyHit(), Card.ironWall());
        int idx = new java.util.Random().nextInt(pool.size());
        return pool.get(idx);
    }

    // ================= 事件 =================

    /** 事件场景：专属背景图 + 右侧名称/描述 + 选项，选完结算回地图 */
    private void startEventScene(Stage stage, GameMap map, Player player, EventDef ev) {
        EventView view = new EventView(ev, opt -> {
            String msg = applyEventOption(player, opt);
            if (player.hp() == 0) {
                Alert over = new Alert(Alert.AlertType.INFORMATION);
                over.setTitle("事件结果");
                over.setHeaderText(null);
                over.setContentText(msg + "\n\n你的生命归零……本局结束。");
                over.setOnHidden(e -> stage.setScene(buildMenuScene(stage)));
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

        Scene scene = new Scene(view, W, H);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.setScene(buildMenuScene(stage)); // 放弃本局
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
        List<Relic> starters = List.of(
                new Relic("青铜怀表", "战斗开始时获得 2 点格挡"),
                new Relic("请假条", "每回合多抽 1 张牌"),
                new Relic("保温杯", "每场战斗开始时恢复 10 点生命")
        );

        RoomView room = new RoomView(
                player, starters, "猪神",
                "猪，你未到校，也未请假。若10点前未及时到场，将不再是迟到，而记为旷课。收到速回",
                () -> showMapScene(stage, map, player)
        );

        Scene scene = new Scene(room, W, H);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.setScene(buildMenuScene(stage));
            }
        });
        stage.setScene(scene);
    }
}
