package com.example.demo.character;

import com.example.demo.card.Card;
import com.example.demo.enemy.Enemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 遗物效果逻辑层：所有遗物的触发逻辑集中在此类。
 * 新增遗物时只需修改 {@link Relic}（数据）和本类（逻辑）。
 *
 * 设计原则：本类方法只做"计算 / 查询"，不直接修改战斗状态（格挡、力量等）。
 * 调用方（BattleView / RoomView）根据返回值自行应用。
 */
public class RelicFun {

    /* ================= 查询 ================= */

    public static boolean hasRelic(Player player, String name) {
        for (Relic r : player.relics) {
            if (r.name.equals(name)) return true;
        }
        return false;
    }

    public static List<Relic> filterOwned(List<Relic> pool, Player player) {
        List<Relic> available = new ArrayList<>(pool);
        available.removeIf(r -> player.relics.stream().anyMatch(h -> h.name.equals(r.name)));
        return available;
    }

    /* ================= 战斗开始 ================= */

    /**
     * 战斗开始时触发（纯效果）：请假条、小血瓶、忘情牛肉面、金刚杵。
     * 请假条直接在 player 上递减计数器；小血瓶回血。
     * 力量增益由返回值告诉调用方。
     *
     * @return 战斗开始时应增加的力量值
     */
    public static int onBattleStart(Player player) {
        // 请假条：接下来的三场战斗怪物血量变为 1
        if (player.leaveNoteBattles > 0) {
            player.leaveNoteBattles--;
        }
        // 小血瓶：战斗开始时恢复 2 点生命
        if (hasRelic(player, "小血瓶")) {
            player.heal(2);
        }
        int strength = 0;
        // 忘情牛肉面：战斗开始时获得 3 点力量，仅第一回合有效
        if (hasRelic(player, "忘情牛肉面")) {
            strength += 3;
        }
        // 金刚杵：战斗开始时获得 1 点力量（永久）
        if (hasRelic(player, "金刚杵")) {
            strength += 1;
        }
        return strength;
    }

    /** 牛来：战斗开始时对敌人造成的伤害（3），无此遗物返回 0 */
    public static int battleStartDamage(Player player) {
        return hasRelic(player, "牛来") ? 3 : 0;
    }

    /** 请假条：战斗开始时是否将怪物血量设为 1 */
    public static boolean isLeaveNoteActive(Player player) {
        return player.leaveNoteBattles > 0;
    }

    /* ================= 回合开始 ================= */

    /** 回合开始时计算额外能量：奴隶贩子颈环、古茶具套装、孙子兵法 */
    public static int extraEnergy(Player player, Enemy enemy, int turn, boolean playedCardLastTurn) {
        int e = 0;
        // 奴隶贩子颈环：Boss 战和精英战每回合 +1 能量
        if ((enemy.isBoss || enemy.isElite) && hasRelic(player, "奴隶贩子颈环")) {
            e += 1;
        }
        // 古茶具套装：篝火休息后下一场战斗第一回合 +2 能量
        if (turn == 1 && hasRelic(player, "古茶具套装") && player.restedAtCampfire) {
            player.restedAtCampfire = false; // 消耗标记
            e += 2;
        }
        // 孙子兵法：上回合未出牌则获得 1 点额外能量
        if (!playedCardLastTurn && turn > 1 && hasRelic(player, "孙子兵法")) {
            e += 1;
        }
        return e;
    }

    /** 回合开始时计算额外格挡：青铜怀表、猫、奶龙 */
    public static int startBlock(Player player, int turn) {
        int block = 0;
        if (turn == 1) {
            if (hasRelic(player, "青铜怀表")) {
                block += 2;
            }
            if (hasRelic(player, "猫")) {
                block += 10;
            }
        }
        // 奶龙：第二回合开始时获得 12 点格挡
        if (turn == 2 && hasRelic(player, "奶龙")) {
            block += 12;
        }
        return block;
    }

    /** 第一回合开始时往手牌中添加额外卡牌：英雄宝典 */
    public static void addTurnStartCards(Player player, List<Card> hand, int turn) {
        // 英雄宝典：战斗开始时增加一张免费能力牌
        if (turn == 1 && hasRelic(player, "英雄宝典")) {
            hand.add(Card.freePower());
        }
    }

    /* ================= 回合结束 ================= */

    /**
     * 回合结束时计算额外格挡：奥利哈钢、taffy。
     * @param currentBlock 当前格挡值（忘情牛肉面判断用）
     * @param hp           当前生命
     * @param maxHp        最大生命
     * @return 应额外获得的格挡值
     */
    public static int endTurnBlock(Player player, int currentBlock, int hp, int maxHp) {
        int block = 0;
        // 奥利哈钢：回合结束时若无格挡，获得 6 点格挡
        if (currentBlock == 0 && hasRelic(player, "奥利哈钢")) {
            block += 6;
        }
        // taffy：回合结束时生命值高于 50% 额外获得 5 点格挡
        if (hp * 2 > maxHp && hasRelic(player, "taffy")) {
            block += 5;
        }
        return block;
    }

    /** 回合结束时是否应移除临时力量（忘情牛肉面第一回合结束后 -3 力量） */
    public static boolean shouldRemoveNoodleBonus(Player player, int turn) {
        return turn == 1 && hasRelic(player, "忘情牛肉面");
    }

    /* ================= 受伤触发 ================= */

    /**
     * 玩家首次受伤时是否触发百年积木（抽 3 张牌）。
     * @param firstDamage 是否为本场战斗首次受伤（由调用方维护标记）
     * @return true 表示应抽牌
     */
    public static boolean shouldDrawOnFirstDamage(Player player, boolean firstDamage) {
        return firstDamage && hasRelic(player, "百年积木");
    }

    /* ================= 战斗结束 ================= */

    /** 战斗结束后触发：燃烧之血（战士被动）、带骨肉、老牧师 */
    public static void onBattleEnd(Player player) {
        // 战士被动：燃烧之血 —— 每次战斗结束后恢复 6 点生命
        player.heal(6);
        // 带骨肉：战斗结束时若生命值 < 50%，恢复 12 点生命
        if (hasRelic(player, "带骨肉") && player.hp() * 2 < player.maxHp) {
            player.heal(12);
        }
        // 老牧师：战斗结束后最大生命值永久 +1
        if (hasRelic(player, "老牧师")) {
            player.increaseMaxHp(1);
        }
    }

    /* ================= Boss 胜利奖励 ================= */

    /**
     * Boss 战胜利后，根据获得的 Boss 遗物返回后续动作类型：
     * "REMOVE_CARDS" / "CHOOSE_ELITE" / null
     */
    public static String bossVictoryAction(Relic bossRelic) {
        if (bossRelic == null) return null;
        if (bossRelic.name.equals("空鸟笼")) return "REMOVE_CARDS";
        if (bossRelic.name.equals("召唤铃铛")) return "CHOOSE_ELITE";
        return null;
    }

    /** 显示精英遗物选择：随机三个精英遗物，选一个获得 */
    public static void showEliteRelicChoice(Player player, Runnable onDone) {
        List<Relic> pool = filterOwned(Relic.eliteRelics(), player);
        if (pool.isEmpty()) {
            onDone.run();
            return;
        }

        List<Relic> candidates = new ArrayList<>(pool);
        List<Relic> offers = new ArrayList<>();
        int count = Math.min(3, candidates.size());
        for (int i = 0; i < count; i++) {
            int idx = new Random().nextInt(candidates.size());
            offers.add(candidates.remove(idx));
        }

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("召唤铃铛");
        alert.setHeaderText("从精英遗物池中选择一个遗物");
        alert.getDialogPane().setPrefSize(500, 350);

        javafx.scene.layout.VBox relicList = new javafx.scene.layout.VBox(10);
        relicList.setPadding(new javafx.geometry.Insets(15));

        for (Relic r : offers) {
            javafx.scene.layout.VBox relicBox = new javafx.scene.layout.VBox(4);
            javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(r.name);
            nameLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 16px; -fx-font-weight: bold;");
            javafx.scene.control.Label descLabel = new javafx.scene.control.Label(r.desc);
            descLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px;");
            descLabel.setWrapText(true);
            relicBox.getChildren().addAll(nameLabel, descLabel);

            javafx.scene.control.Button relicBtn = new javafx.scene.control.Button();
            relicBtn.setGraphic(relicBox);
            relicBtn.setPrefWidth(450);
            relicBtn.setStyle("-fx-background-color: #2d3748; -fx-padding: 12; -fx-cursor: hand; "
                    + "-fx-background-radius: 8;");
            relicBtn.setOnMouseEntered(e ->
                relicBtn.setStyle("-fx-background-color: #4a5568; -fx-padding: 12; -fx-cursor: hand; "
                        + "-fx-background-radius: 8;"));
            relicBtn.setOnMouseExited(e ->
                relicBtn.setStyle("-fx-background-color: #2d3748; -fx-padding: 12; -fx-cursor: hand; "
                        + "-fx-background-radius: 8;"));
            relicBtn.setOnAction(e -> {
                player.addRelic(r);
                alert.close();
                onDone.run();
            });
            relicList.getChildren().add(relicBtn);
        }

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(relicList);
        scrollPane.setFitToWidth(true);
        alert.getDialogPane().setContent(scrollPane);

        alert.getDialogPane().getButtonTypes().clear();
        alert.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);

        alert.showAndWait().ifPresent(btnType -> {
            if (btnType == javafx.scene.control.ButtonType.CANCEL) {
                onDone.run();
            }
        });
    }

    /* ================= 获得遗物时 ================= */

    /**
     * 获得遗物时的即时效果：保温杯、请假条、破镜。
     * @param removeCards 卡牌移除回调（破镜需要 UI 交互，由调用方提供）
     */
    public static void onRelicObtained(Player player, Relic relic, Runnable removeCards) {
        // 保温杯：获得时永久增加 8 点最大生命值
        if (relic.name.equals("保温杯")) {
            player.increaseMaxHp(8);
        }
        // 请假条：获得时设置 3 场战斗生效
        if (relic.name.equals("请假条")) {
            player.leaveNoteBattles = 3;
        }
        // 破镜：删除一张卡牌
        if (relic.name.equals("破镜") && removeCards != null) {
            removeCards.run();
        }
    }

    /* ================= 遗物获取（随机池） ================= */

    public static int extraDraw(Player player) {
        return 0;
    }

    public static List<Relic> starterRelics() {
        return Relic.starterRelics();
    }

    /** 全部遗物（起点 + 精英 + 事件 + Boss），开发者模式面板用它列出所有可加/可删的遗物 */
    public static List<Relic> allRelics() {
        return Relic.allRelics();
    }

    public static Relic randomEliteRelic(Player player) {
        List<Relic> eliteRelics = Relic.eliteRelics();
        List<Integer> eliteWeights = Relic.eliteWeights();
        List<Relic> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (int i = 0; i < eliteRelics.size(); i++) {
            Relic r = eliteRelics.get(i);
            if (player.relics.stream().noneMatch(h -> h.name.equals(r.name))) {
                pool.add(r);
                weights.add(eliteWeights.get(i));
            }
        }
        if (pool.isEmpty()) return null;

        int total = 0;
        for (int w : weights) total += w;
        int roll = new Random().nextInt(total);
        int cumulative = 0;
        Relic gained = pool.get(pool.size() - 1);
        for (int i = 0; i < pool.size(); i++) {
            cumulative += weights.get(i);
            if (roll < cumulative) {
                gained = pool.get(i);
                break;
            }
        }

        player.addRelic(gained);
        if (gained.name.equals("草莓")) {
            player.increaseMaxHp(7);
        }
        if (gained.name.equals("荔枝")) {
            player.increaseMaxHp(13);
        }
        return gained;
    }

    public static Relic randomEventRelic(Player player) {
        List<Relic> pool = filterOwned(Relic.eventRelics(), player);
        if (pool.isEmpty()) return null;
        Relic gained = pool.get(new Random().nextInt(pool.size()));
        player.addRelic(gained);
        return gained;
    }

    /** 击败 Boss 后随机获得一个 Boss 遗物（池为空时返回 null） */
    public static Relic randomBossRelic(Player player) {
        List<Relic> pool = filterOwned(Relic.bossRelics(), player);
        if (pool.isEmpty()) return null;
        Relic gained = pool.get(new Random().nextInt(pool.size()));
        player.addRelic(gained);
        return gained;
    }
}
