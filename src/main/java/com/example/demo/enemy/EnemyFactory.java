package com.example.demo.enemy;

import com.example.demo.view.GameMap;

/**
 * 普通怪的生成规则集中在此，供战斗入口调用。
 *
 * 普通怪按楼层分两档：
 *   前 5 层（row 0~4）：史莱姆 / 邪教猪
 *   第 6 层起（row >= 5）：大史莱姆 / 老兵邪教猪（升级替换）
 */
public final class EnemyFactory {

    /** 第 6 层起普通怪替换为升级版（row 从 0 开始，>=5 即第 6 层） */
    private static final int UPGRADE_ROW = 5;

    /** 邪教猪系在普通怪中的出现概率（其余为史莱姆系） */
    private static final double CULTIST_CHANCE = 0.4;
    /** 基础怪的最小概率 */
    private static final double MIN_BASE_RATE=0.4;
    private EnemyFactory() {
    }

    /** 生成某层的普通怪 */
    public static Enemy normal(int row) {
        boolean cultist = Math.random() < CULTIST_CHANCE;
        double baseRate;
        if (row < UPGRADE_ROW) {
            baseRate = 1.0;
        } else {
            double factor = (double) row / GameMap.ROWS;
            baseRate=1.0-factor;
            if(baseRate<MIN_BASE_RATE){
                baseRate=MIN_BASE_RATE;
            }
        }
        boolean spawnBase=Math.random()<baseRate;
        if(cultist){
            return spawnBase?CultistPig.base():CultistPig.veteran();
        }else{
            return spawnBase?Slime.base():Slime.big();
        }
    }

    /** BOSS 房：猪龙鱼公爵 / 巨猪骑士各 50% */
    public static Enemy boss() {
        return Math.random() < 0.5 ? new DukePorcodraco() : new GiantBoarKnight();
    }
}
