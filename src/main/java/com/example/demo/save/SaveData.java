package com.example.demo.save;

import com.example.demo.card.Card;
import com.example.demo.character.Player;
import com.example.demo.character.Relic;
import com.example.demo.view.GameMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 单槽存档：只记录「刚进入某个节点」那一刻的状态。
 *
 * <p><b>为什么只存在节点入口：</b>这是需求定下来的语义 —— 战斗中 / 事件中按 Esc 返回主菜单，
 * 下次点「开始游戏」就直接回到<b>刚进入这个节点</b>的时候（战斗从头打、事件重新选），
 * 而不是恢复战斗进行到一半的手牌和血量。所以存档里既有「我是谁」（玩家状态），
 * 也有「我在哪」（地图种子 + 当前节点坐标），但没有战斗过程。</p>
 *
 * <p><b>敌人 / 事件名字也一起存：</b>否则每次读档都会重掷一只怪，
 * 玩家就能靠「退出重进」刷掉难打的敌人（打不过神风猪就退出去换个史莱姆）。</p>
 *
 * <p>存储格式沿用 {@code GameSettings} 的写法：一个 {@link Properties} 文件放在用户目录，
 * 读写失败只打印一次、绝不弹出异常打断游戏。</p>
 */
public final class SaveData {

    /** 存档文件：~/.slay-the-spire-save.properties（和设置文件分开，互不干扰） */
    private static final Path FILE =
            Paths.get(System.getProperty("user.home"), ".slay-the-spire-save.properties");

    /** 读写失败只提示一次，避免每次切场景刷一屏报错 */
    private static boolean warned = false;

    /**
     * 存档记录的「进度阶段」—— 决定读档之后回到哪一步。
     */
    public enum Phase {
        /** 刚进入这个节点：战斗还没打 / 事件还没选。读档 = 重进这个节点 */
        ENTER,
        /** 战斗已经打赢，正等着（或正在）领卡牌奖励。读档 = 回到选牌页 */
        REWARD,
        /** 这个节点已经处理完了，人站在地图上。读档 = 回到地图 */
        CLEARED
    }

    // ================= 存档内容 =================

    public long mapSeed;                 // 地图种子：同一个种子 = 同一张图
    public int row;                      // 当前节点所在层
    public int col;                      // 当前节点所在列
    public String nodeType = "";         // 进入时判定的房间类型（混沌改判后的结果）
    public Phase phase = Phase.ENTER;    // 进度阶段（见 Phase）
    public String enemy = "";            // 战斗节点的敌人名字（非战斗节点为空）
    public String event = "";            // 事件节点的事件名字（非事件节点为空）

    public int maxHp;
    public int hp;
    public boolean chaos;                // 混沌：本局地图变异
    public boolean restedAtCampfire;
    public int leaveNoteBattles;         // 请假条：剩余生效战斗场次

    /** 牌组：每张牌写成 {@code "STRIKE"} 或 {@code "STRIKE+"}（末尾 + 表示已升级） */
    public final List<String> deck = new ArrayList<>();
    /**
     * 战斗胜利后抽到的那几张奖励牌（写法同 deck）。只在 {@code phase=REWARD} 时有意义。
     *
     * <p>存下来是为了读档回到选牌页时还是<b>同样这几张</b> ——
     * 不然玩家能靠「退出重进」把奖励刷到满意为止。</p>
     */
    public final List<String> reward = new ArrayList<>();
    /** 遗物：只存名字，读档时回 {@link Relic#allRelics()} 里按名字找原对象 */
    public final List<String> relics = new ArrayList<>();

    // ================= 存 / 读 / 删 =================

    /** 有没有存档（损坏的存档也算有，读的时候才判坏） */
    public static boolean exists() {
        return Files.exists(FILE);
    }

    /** 删除存档（放弃本局、本局正常结束时调用） */
    public static void delete() {
        try {
            Files.deleteIfExists(FILE);
        } catch (IOException e) {
            warn("删除存档失败", e);
        }
    }

    /** 写存档（阶段默认 {@link Phase#ENTER}，也就是「刚进节点」）。 */
    public static void capture(GameMap map, Player player, GameMap.NodeType type,
                               String enemyName, String eventName) {
        capture(map, player, type, enemyName, eventName, Phase.ENTER, null);
    }

    /**
     * 写存档。
     *
     * @param type        房间类型（进入时判定，已经过混沌改判）
     * @param enemyName   这一战的敌人名字（不是战斗节点传 "" 即可）
     * @param eventName   这个事件的名字（不是事件节点传 "" 即可）
     * @param phase       进度阶段，见 {@link Phase}
     * @param rewardCards {@code phase=REWARD} 时抽到的奖励牌；其它阶段传 null
     */
    public static void capture(GameMap map, Player player, GameMap.NodeType type,
                               String enemyName, String eventName,
                               Phase phase, List<Card> rewardCards) {
        if (map == null || player == null || type == null) return;
        if (map.current == null) return; // 还没出发（没点过起点），没有可存的节点

        SaveData s = new SaveData();
        s.mapSeed = map.seed;
        s.row = map.current.row;
        s.col = map.current.col;
        s.nodeType = type.name();
        s.phase = (phase == null) ? Phase.ENTER : phase;
        s.enemy = enemyName == null ? "" : enemyName;
        s.event = eventName == null ? "" : eventName;
        if (rewardCards != null) {
            for (Card c : rewardCards) {
                s.reward.add(c.kind.name() + (c.upgraded ? "+" : ""));
            }
        }
        s.maxHp = player.maxHp;
        s.hp = player.hp;
        s.chaos = player.chaos;
        s.restedAtCampfire = player.restedAtCampfire;
        s.leaveNoteBattles = player.leaveNoteBattles;
        for (Card c : player.deck) {
            s.deck.add(c.kind.name() + (c.upgraded ? "+" : ""));
        }
        for (Relic r : player.relics) {
            s.relics.add(r.name);
        }

        Properties p = new Properties();
        p.setProperty("mapSeed", Long.toString(s.mapSeed));
        p.setProperty("row", Integer.toString(s.row));
        p.setProperty("col", Integer.toString(s.col));
        p.setProperty("nodeType", s.nodeType);
        p.setProperty("phase", s.phase.name());
        p.setProperty("enemy", s.enemy);
        p.setProperty("event", s.event);
        p.setProperty("maxHp", Integer.toString(s.maxHp));
        p.setProperty("hp", Integer.toString(s.hp));
        p.setProperty("chaos", Boolean.toString(s.chaos));
        p.setProperty("restedAtCampfire", Boolean.toString(s.restedAtCampfire));
        p.setProperty("leaveNoteBattles", Integer.toString(s.leaveNoteBattles));
        p.setProperty("deck", String.join(",", s.deck));
        p.setProperty("relics", String.join(",", s.relics));
        p.setProperty("reward", String.join(",", s.reward));

        try (OutputStream out = Files.newOutputStream(FILE)) {
            p.store(out, "slay-the-spire-imitation run save");
        } catch (IOException e) {
            warn("写入存档失败", e);
        }
    }

    /**
     * 读存档；没有存档、或存档内容损坏（比如手改坏了）都返回 {@code null}，
     * 调用方看到 null 就当「没有存档」处理，不会崩。
     */
    public static SaveData read() {
        if (!Files.exists(FILE)) return null;

        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
        } catch (IOException e) {
            warn("读取存档失败", e);
            return null;
        }

        try {
            SaveData s = new SaveData();
            s.mapSeed = Long.parseLong(p.getProperty("mapSeed", "0"));
            s.row = Integer.parseInt(p.getProperty("row", "0"));
            s.col = Integer.parseInt(p.getProperty("col", "0"));
            s.nodeType = p.getProperty("nodeType", "");
            s.phase = parsePhase(p.getProperty("phase", "ENTER"));
            s.enemy = p.getProperty("enemy", "");
            s.event = p.getProperty("event", "");
            s.maxHp = Integer.parseInt(p.getProperty("maxHp", "80"));
            s.hp = Integer.parseInt(p.getProperty("hp", "80"));
            s.chaos = Boolean.parseBoolean(p.getProperty("chaos", "false"));
            s.restedAtCampfire = Boolean.parseBoolean(p.getProperty("restedAtCampfire", "false"));
            s.leaveNoteBattles = Integer.parseInt(p.getProperty("leaveNoteBattles", "0"));
            s.deck.addAll(splitList(p.getProperty("deck", "")));
            s.relics.addAll(splitList(p.getProperty("relics", "")));
            s.reward.addAll(splitList(p.getProperty("reward", "")));

            if (s.nodeType.isEmpty()) return null; // 关键字段缺失 = 坏档
            return s;
        } catch (RuntimeException e) {
            warn("存档内容已损坏", e);
            return null;
        }
    }

    // ================= 还原成游戏对象 =================

    /**
     * 还原玩家。
     *
     * <p>⚠ 遗物这里是<b>直接 {@code relics.add}</b>，<b>不能</b>用
     * {@link Player#addRelic(Relic)} —— 那个方法会触发
     * {@code RelicFun.onRelicObtained} 的即时效果（草莓 +7 最大生命 / 请假条计数 /
     * 混沌置位 / 召唤铃铛塞伤口），而这些结果<b>已经算进存档里的 maxHp、chaos、牌组</b>了，
     * 再跑一遍就是重复结算。</p>
     */
    public Player buildPlayer() {
        Player p = new Player(); // 构造器会塞起始牌组 + 燃烧之血，下面整个覆盖掉
        p.deck.clear();
        for (String token : deck) {
            Card c = parseCard(token);
            if (c != null) p.deck.add(c);
        }
        p.relics.clear();
        for (String name : relics) {
            Relic r = findRelic(name);
            if (r != null) p.relics.add(r);
        }
        p.maxHp = maxHp;
        p.hp = Math.max(0, Math.min(hp, maxHp)); // 存档被改过也别让血量超上限
        p.chaos = chaos;
        p.restedAtCampfire = restedAtCampfire;
        p.leaveNoteBattles = leaveNoteBattles;
        return p;
    }

    /** 用存档里的种子重建<b>同一张</b>地图，并把当前节点指回存档记录的那个。 */
    public GameMap buildMap() {
        GameMap map = GameMap.generate(mapSeed);
        map.current = map.nodeAt(row, col);
        return map;
    }

    /**
     * 存档里记的卡牌奖励（读档回到「胜利后的选牌页」时用）。
     * 解析不出来的牌会被跳过；列表为空表示这是个没记奖励的旧存档，调用方应现抽三张兜底。
     */
    public List<Card> buildRewardCards() {
        List<Card> out = new ArrayList<>();
        for (String token : reward) {
            Card c = parseCard(token);
            if (c != null) out.add(c);
        }
        return out;
    }

    /** 存档记录的房间类型；字段坏了（比如手改过）就退回地图本身的类型。 */
    public GameMap.NodeType resolvedType(GameMap map) {
        try {
            return GameMap.NodeType.valueOf(nodeType);
        } catch (RuntimeException e) {
            return (map == null || map.current == null)
                    ? GameMap.NodeType.MONSTER : map.current.type;
        }
    }

    /** 存档摘要：放弃确认框里显示，让玩家知道要放弃的是哪一局。 */
    public String summary() {
        // 类型字段坏了（比如手改过）就只显示层数，其余信息照常
        String label = GameMap.typeLabel(nodeType);
        String phaseText = switch (phase) {
            case REWARD -> "（已胜利，待领卡牌奖励）";
            case CLEARED -> "（本层已通过）";
            case ENTER -> "";
        };
        return "当前进度：第 " + (row + 1) + " 层"
                + (label.isEmpty() ? "" : "（" + label + "）")
                + phaseText
                + " · 生命 " + hp + " / " + maxHp
                + " · 牌组 " + deck.size() + " 张"
                + " · 遗物 " + relics.size() + " 件";
    }

    // ================= 小工具 =================

    private static Phase parsePhase(String raw) {
        try {
            return Phase.valueOf(raw);
        } catch (RuntimeException e) {
            return Phase.ENTER; // 旧存档没有这个字段：按「刚进节点」处理最保守
        }
    }

    private static Card parseCard(String token) {
        if (token == null || token.isEmpty()) return null;
        boolean up = token.endsWith("+");
        String kindName = up ? token.substring(0, token.length() - 1) : token;
        try {
            return Card.of(Card.Kind.valueOf(kindName), up);
        } catch (RuntimeException e) {
            return null; // 旧存档里的牌种已经删了：跳过这张，不让它带崩读档
        }
    }

    /**
     * 按名字找回遗物的原对象（保住 imagePath，图标才不会退化成「名字首字」）。
     *
     * <p>⚠ <b>燃烧之血不在 {@link Relic#allRelics()} 里</b> —— 它是角色固有遗物，
     * 明确不进任何抽取池。所以扫完池子后必须再单独比对一次
     * {@link Player#STARTER_RELIC}，否则每次读档都会把燃烧之血悄悄弄丢
     * （连带它「每场战斗后回 6 点生命」的被动一起没，而且不报错）。</p>
     */
    private static Relic findRelic(String name) {
        for (Relic r : Relic.allRelics()) {
            if (r.name.equals(name)) return r;
        }
        Relic starter = Player.STARTER_RELIC;
        return (starter != null && starter.name.equals(name)) ? starter : null;
    }

    /** 拆逗号分隔的列表；空串得到空列表（不能直接 split，会得到 [""]）。 */
    private static List<String> splitList(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static void warn(String what, Exception e) {
        if (warned) return;
        warned = true;
        System.out.println("[save] " + what + "：" + e);
    }
}
