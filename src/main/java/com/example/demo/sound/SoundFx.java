package com.example.demo.sound;

import com.example.demo.settings.GameSettings;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 音效工具（用 JDK 自带的 javax.sound，无需额外依赖）。
 *
 * 用法：SoundFx.play("click");
 *   → 对应 resources/com/example/demo/sound/click.wav
 *
 * 素材说明：
 *  - Java 内置解码器只支持 WAV/AIFF，所以 .ogg 素材已用 ffmpeg 转成同名 .wav，
 *    统一放在 resources/com/example/demo/sound/ 下。
 *  - 每次播放开一个后台线程 + 独立 Clip：可叠加播放、不卡界面。
 *
 * 新增音效：把 xxx.wav 丢进 sound 目录，然后代码里 SoundFx.play("xxx") 即可。
 */
public final class SoundFx {

    private static final String DIR = "/com/example/demo/sound/";

    /**
     * 单个音效的音量微调（0~1，相对设置页里的「音效」滑条）。
     * 不写的音效用 1.0（即完全跟随滑条）。
     *
     * 注意：底层 MASTER_GAIN 最大就是 0 dB（=1.0），所以这里最多把音量提到滑条值，
     * 没法超过它。如果某个素材本身录得太轻（比如比别的音效低 20 dB），
     * 光靠这里救不回来，需要用 ffmpeg 直接给文件加增益，例如：
     *   ffmpeg -i 原名.wav -af "volume=22.5dB" -c:a pcm_s16le 新名.wav
     * 再配合 volumedetect 检查 max_volume 别超过 0 dB（会削波）：
     *   ffmpeg -i 新名.wav -af volumedetect -f null NUL
     */
    private static final Map<String, Float> VOLUME_OF = Map.of(
            "normalOink", 1.0f,
            "normalDie", 1.0f,
            "fishronOink", 1.0f,
            "fishronDie", 1.0f);

    private static final Set<String> MISSING_LOGGED = new HashSet<>();

    private SoundFx() {
    }

    /** 播放音效；文件不存在时静默跳过（只提示一次） */
    public static void play(String name) {
        InputStream raw = SoundFx.class.getResourceAsStream(DIR + name + ".wav");
        if (raw == null) {
            if (MISSING_LOGGED.add(name)) {
                System.out.println("[SoundFx] 缺少音效文件: " + DIR + name + ".wav");
            }
            return;
        }
        Thread t = new Thread(() -> playInternal(name, raw), "sound-" + name);
        t.setDaemon(true);
        t.start();
    }

    /**
     * 按优先级依次尝试多个音效名，播放第一个存在的。
     * 用于「BOSS 专属 → 通用」这类有备选方案的场景，例如：
     *   playAny(actorSound("attack"))  → 有 boss_attack.wav 就播它，没有就退回 enemy_attack.wav
     * 全部都不存在时什么都不播，也不会打印缺文件提示（因为备选是正常的）。
     */
    public static void playAny(List<String> names) {
        for (String n : names) {
            if (exists(n)) {
                play(n);
                return;
            }
        }
    }

    /** 音效文件是否已放进 sound 目录 */
    public static boolean exists(String name) {
        try (InputStream raw = SoundFx.class.getResourceAsStream(DIR + name + ".wav")) {
            return raw != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private static void playInternal(String name, InputStream raw) {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
            Clip clip = AudioSystem.getClip();
            clip.open(in);
            applyVolume(clip, name);
            clip.addLineListener(ev -> {
                if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (Exception ex) {
            // 播放失败不影响游戏
        }
    }

    private static void applyVolume(Clip clip, String name) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                // 设置页的「音效」滑条 × 该音效的单独微调系数
                double vol = GameSettings.getSfxVolume() * VOLUME_OF.getOrDefault(name, 1.0f);
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(Math.max(vol, 0.0001)) / Math.log(10.0) * 20.0);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
            }
        } catch (Exception ignored) {
        }
    }
}
