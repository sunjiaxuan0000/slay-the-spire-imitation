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
 * 现在各场景用的曲子（放在 resources/com/example/demo/sound/ 下）：
 *   主菜单 / 设置页 —— bgm_menu.wav
 *   地图 / 小怪战斗 —— Level1.wav
 *   BOSS 战斗       —— bgm_boss.wav（没有就沿用 Level1.wav）
 *
 * 用法：MusicFx.playLoop("Level1", "bgm_map");
 *   → 按顺序找，播第一个存在的；都没有就保持当前音乐不动（不会突然静音）。
 *   已经在播的曲子不会被重头开始（地图→战斗用同一首时音乐是连续的）。
 *
 * 素材只支持 wav（Java 内置解码器不认 mp3/ogg），转换：
 *   ffmpeg -i 原名.ogg -ac 1 -ar 44100 -c:a pcm_s16le 新名.wav
 */
public final class MusicFx {

    private static final String DIR = "/com/example/demo/sound/";

    /**
     * BGM 最短时长（秒）。短于这个长度的文件会被当成“放错了的小音效”跳过，
     * 免得把 0.1 秒的点击声循环成“一直咔咔响”。真想用很短的循环音乐就改小这个数。
     */
    private static final double MIN_BGM_SECONDS = 2.0;

    private static Clip clip;
    private static String current; // 正在播的曲目名，null = 没在播

    private MusicFx() {
    }

    /**
     * 循环播放某首 BGM，可给多个备选名（按顺序取第一个存在的）。
     * 例：playLoop("bgm_boss", "Level1") → 有 BOSS 专属曲就播它，没有就用 Level1。
     */
    public static void playLoop(String... candidates) {
        if (candidates == null || candidates.length == 0) return;

        // 1) 先确定「这次实际该播哪首」= 备选里第一个存在的文件。
        //    注意顺序：一定要先定位再判断，否则会变成
        //    “只要备选里有一首正在播就不换”，新放的更优先的曲子（比如 bgm_boss）永远不会生效。
        String name = null;
        InputStream raw = null;
        for (String c : candidates) {
            if (c == null) continue;
            InputStream in = MusicFx.class.getResourceAsStream(DIR + c + ".wav");
            if (in != null) {
                name = c;
                raw = in;
                break;
            }
        }
        if (raw == null) return; // 一首都没有：保持现在的音乐，不打断

        // 2) 该播的就是当前正在播的那首 → 不打断（地图↔小怪战斗用同一首时音乐连续）。
        //    这里只看 current，不看 clip.isRunning()：某些机器上 isRunning() 会返回 false，
        //    会导致同一首曲子被反复从头播。
        if (name.equals(current) && clip != null) {
            try {
                raw.close();
            } catch (Exception ignored) {
            }
            return;
        }

        // 3) 真的换曲子
        stop();

        try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
            Clip c = AudioSystem.getClip();
            c.open(in);

            double seconds = c.getMicrosecondLength() / 1_000_000.0;
            if (seconds < MIN_BGM_SECONDS) {
                // 常见事故：把 click.wav 之类的小音效复制成了 bgm_xxx.wav，
                // 循环播放会变成“一直咔咔响”。这里直接跳过并说清楚。
                c.close();
                System.out.printf("[MusicFx] 跳过 %s.wav：只有 %.2f 秒，看着不是音乐"
                        + "（是不是把小音效复制过来当 BGM 了？）%n", name, seconds);
                return;
            }

            clip = c;
            current = name;
            applyVolume();
            c.loop(Clip.LOOP_CONTINUOUSLY);
            System.out.printf("[MusicFx] 开始循环播放 %s.wav（时长 %.1f 秒，音量 %.2f）%n",
                    name, seconds, GameSettings.getMusicVolume());
        } catch (Exception ex) {
            clip = null;
            current = null; // 播放失败不影响游戏
            System.out.println("[MusicFx] 播放失败：" + name + ".wav —— " + ex);
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
