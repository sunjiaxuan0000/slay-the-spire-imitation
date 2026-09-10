package com.example.demo.sound;

import com.example.demo.settings.GameSettings;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * 背景音乐（BGM）：循环播放 sound 目录里的 wav，音量由设置页的「音乐」滑条控制。
 *
 * 可用的曲目名（放在 resources/com/example/demo/sound/ 下，没有就静音，不会报错）：
 *   bgm_menu.wav    —— 主菜单 / 设置页
 *   bgm_map.wav     —— 地图页
 *   bgm_battle.wav  —— 战斗
 *
 * 用法：MusicFx.playLoop("bgm_menu");
 * 换一首会自动停掉上一首；同一个名字重复调用不会重头播。
 */
public final class MusicFx {

    private static final String DIR = "/com/example/demo/sound/";

    private static Clip clip;
    private static String current; // 正在播的曲目名，null = 没在播

    private MusicFx() {
    }

    /** 循环播放某首 BGM（文件不存在则静音） */
    public static void playLoop(String name) {
        if (name == null) return;
        if (name.equals(current) && clip != null && clip.isRunning()) return; // 已经在播了
        stop();

        InputStream raw = MusicFx.class.getResourceAsStream(DIR + name + ".wav");
        if (raw == null) return; // 还没放这首 BGM：安静地什么都不做

        try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
            Clip c = AudioSystem.getClip();
            c.open(in);
            clip = c;
            current = name;
            applyVolume();
            c.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception ex) {
            clip = null;
            current = null; // 播放失败不影响游戏
        }
    }

    /** 停止当前 BGM */
    public static void stop() {
        if (clip != null) {
            try {
                clip.stop();
                clip.close();
            } catch (Exception ignored) {
            }
            clip = null;
        }
        current = null;
    }

    /** 设置页拖「音乐」滑条时调用：正在播的 BGM 立刻跟着变响/变轻 */
    public static void setVolume(double volume) {
        GameSettings.setMusicVolume(volume);
        applyVolume();
    }

    private static void applyVolume() {
        if (clip == null) return;
        try {
            if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            double v = Math.max(GameSettings.getMusicVolume(), 0.0001);
            float dB = (float) (Math.log(v) / Math.log(10.0) * 20.0);
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
        } catch (Exception ignored) {
        }
    }
}
