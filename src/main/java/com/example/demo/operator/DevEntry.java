package com.example.demo.operator;

import com.example.demo.battle.BattleView;
import com.example.demo.character.Player;
import com.example.demo.settings.GameSettings;
import com.example.demo.view.MapView;
import com.example.demo.view.RunHud;

import javafx.scene.Node;

import java.util.function.Consumer;

/**
 * 开发者模式的“接线口”：所有与开发者模式相关的胶水代码都收在 operator 包，
 * 游戏场景里只留一行调用，读起来不会到处看见 devMode 判断。
 *
 * 用法（HelloApplication 里）：
 *   地图场景： DevEntry.enableDevMap(view);
 *   地图 HUD： DevEntry.attachDevButton(hud, player, null,   hud::refresh, this::showOverlay, this::closeWindow);
 *   战斗 HUD： DevEntry.attachDevButton(hud, player, battle, hud::refresh, this::showOverlay, this::closeWindow);
 *             DevEntry.attachKillButton(hud, battle);   // 红色的「杀」= 一键秒杀敌人
 *
 * 开关本身存在 {@link GameSettings#isDevMode()}（设置页里的那个开关），
 * 这里所有方法在开关关闭时都是空操作。
 */
public final class DevEntry {

    private DevEntry() {
    }

    /** 开发者模式开着时：地图上可以点任意节点进入（否则什么都不做） */
    public static void enableDevMap(MapView view) {
        if (view == null || !GameSettings.isDevMode()) return;
        view.setDevMode(true);
    }

    /**
     * 开发者模式开着时：在 HUD 上挂一个「开」按钮，点开 {@link DevPanel}。
     *
     * @param player    当前玩家（面板要改它的牌组 / 遗物）
     * @param battle    当前战斗，地图场景传 null
     * @param refresh   面板改完数据后调用（一般是 hud::refresh）
     * @param showPanel 把面板显示成整页窗口（一般是 node -> showWindow(page(node))）
     * @param closePanel 关闭整页窗口（一般是 HelloApplication::closeWindow）
     */
    public static void attachDevButton(RunHud hud, Player player, BattleView battle,
                                       Runnable refresh, Consumer<Node> showPanel,
                                       Runnable closePanel) {
        if (hud == null || player == null || !GameSettings.isDevMode()) return;

        hud.addDevButton(() -> {
            DevPanel panel = new DevPanel(player, battle, refresh, closePanel);

//            if (battle != null) {
//                // 面板挂进窗口 = 战斗暂停；面板被移出窗口（点关闭 / 按 Esc 都会走清空窗口）
//                // = 自动恢复战斗。用 parent 变化来判断，两条关闭路径都不用额外记账。
//                panel.parentProperty().addListener(
//                        (o, oldParent, newParent) -> battle.setPaused(newParent != null));
//            }
            showPanel.accept(panel);
        });
    }

    /**
     * 开发者模式开着时：在战斗 HUD 上再挂一个红色的「杀」按钮，一键秒杀当前敌人。
     *
     * <p>只挂在<b>战斗</b>的 HUD 上 —— 地图场景传 null，那就什么都不挂。</p>
     *
     * @param battle 当前战斗；地图场景传 null
     */
    public static void attachKillButton(RunHud hud, BattleView battle) {
        if (hud == null || battle == null || !GameSettings.isDevMode()) return;
        hud.addDevKillButton(battle::devKillEnemy);
    }
}
