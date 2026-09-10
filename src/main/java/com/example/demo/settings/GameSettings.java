package com.example.demo.settings;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 全局设置：音乐音量、音效音量、开发者模式开关。
 *
 * 数据存在用户目录下的 .slay-the-spire-imitation.properties 里，下次启动自动读回。
 * 读写失败（没有权限等）不影响游戏，只是设置不会被记住。
 *
 * 谁在用：
 *   - {@link com.example.demo.sound.SoundFx}  → 音效音量
 *   - {@link com.example.demo.sound.MusicFx}  → 音乐音量
 *   - 地图 / HUD / 战斗                      → 开发者模式开关
 */
public final class GameSettings {

    /** 默认值（第一次运行时用） */
    public static final double DEFAULT_MUSIC = 0.5;
    public static final double DEFAULT_SFX = 0.7;

    private static final Path FILE = Paths.get(System.getProperty("user.home"),
            ".slay-the-spire-imitation.properties");

    private static double musicVolume = DEFAULT_MUSIC;
    private static double sfxVolume = DEFAULT_SFX;
    private static boolean devMode = false;

    static {
        load();
    }

    private GameSettings() {
    }

    // ================= 音乐 =================

    public static double getMusicVolume() {
        return musicVolume;
    }

    public static void setMusicVolume(double v) {
        double nv = clamp(v);
        if (Math.abs(nv - musicVolume) < 0.005) return; // 滑条拖动时别疯狂写文件
        musicVolume = nv;
        save();
    }

    // ================= 音效 =================

    public static double getSfxVolume() {
        return sfxVolume;
    }

    public static void setSfxVolume(double v) {
        double nv = clamp(v);
        if (Math.abs(nv - sfxVolume) < 0.005) return;
        sfxVolume = nv;
        save();
    }

    // ================= 开发者模式 =================

    public static boolean isDevMode() {
        return devMode;
    }

    public static void setDevMode(boolean on) {
        if (devMode == on) return;
        devMode = on;
        save();
    }

    // ================= 内部 =================

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static void load() {
        if (!Files.isReadable(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            musicVolume = clamp(Double.parseDouble(p.getProperty("music", "" + DEFAULT_MUSIC)));
            sfxVolume = clamp(Double.parseDouble(p.getProperty("sfx", "" + DEFAULT_SFX)));
            devMode = Boolean.parseBoolean(p.getProperty("devMode", "false"));
        } catch (Exception ignored) {
            // 读不出来就用默认值
        }
    }

    private static void save() {
        Properties p = new Properties();
        p.setProperty("music", String.valueOf(musicVolume));
        p.setProperty("sfx", String.valueOf(sfxVolume));
        p.setProperty("devMode", String.valueOf(devMode));
        try (OutputStream out = Files.newOutputStream(FILE)) {
            p.store(out, "Slay the Spire Imitation settings");
            warned = false;
        } catch (Exception ex) {
            // 存不下就算了，不影响本局；只在控制台提醒一次，方便排查
            if (!warned) {
                warned = true;
                System.out.println("[GameSettings] 设置保存失败（不影响游戏）：" + FILE
                        + " —— " + ex.getClass().getSimpleName());
            }
        }
    }

    private static boolean warned = false;
}
