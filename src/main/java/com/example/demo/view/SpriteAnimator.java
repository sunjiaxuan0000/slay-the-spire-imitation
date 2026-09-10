package com.example.demo.view;

import javafx.scene.Node;

/**
 * 统一的角色立绘动画器。
 *
 * 用状态机 + 手动帧更新驱动，替代 JavaFX Transition / Timeline / AnimationTimer，
 * 避免多个动画对象竞争同一 Node 的变换属性。
 *
 * 状态：
 *   IDLE        — 待机（可选呼吸：Y 上下浮动 + 轻微旋转）
 *   HIT_KNOCK   — 受击弹开（弹性缓动回弹）
 *   ATTACK_DASH — 攻击突进（前冲后收回）
 *   HURT_SHAKE  — 受击抖动（左右快速摇晃）
 *
 * 由外部唯一的 AnimationTimer 每帧调用 {@link #update(double)}。
 */
public class SpriteAnimator {

    public enum State { IDLE, HIT_KNOCK, ATTACK_DASH, HURT_SHAKE }

    private final Node target;
    private State state = State.IDLE;
    private double timer = 0;        // 当前状态已运行时间（秒）
    private double idleTime = 0;     // 待机呼吸累计时间（秒）

    // 可配置参数
    private final boolean hasIdleBreath;   // 是否有待机呼吸
    private final double attackOffset;      // 攻击突进距离（正=向右）
    private final double knockOffset;       // 受击弹开起始偏移（正=向右）

    /**
     * @param target         要驱动的立绘 Node
     * @param hasIdleBreath  是否有待机呼吸动画
     * @param attackOffset   攻击突进的像素距离（正=向右冲）
     * @param knockOffset    受击弹开的像素距离（正=向右弹）
     */
    public SpriteAnimator(Node target, boolean hasIdleBreath,
                          double attackOffset, double knockOffset) {
        this.target = target;
        this.hasIdleBreath = hasIdleBreath;
        this.attackOffset = attackOffset;
        this.knockOffset = knockOffset;
    }

    // ================= 触发方法 =================

    /** 受击弹开（弹性回弹） */
    public void triggerHitKnock() {
        timer = 0;
        state = State.HIT_KNOCK;
    }

    /** 攻击突进（前冲后收回） */
    public void triggerAttackDash() {
        timer = 0;
        state = State.ATTACK_DASH;
    }

    /** 受击抖动（左右摇晃） */
    public void triggerHurt() {
        timer = 0;
        state = State.HURT_SHAKE;
    }

    public State getState() {
        return state;
    }

    /** 停止所有动画，复位变换 */
    public void stop() {
        state = State.IDLE;
        timer = 0;
        idleTime = 0;
        target.setTranslateX(0);
        target.setTranslateY(0);
        target.setRotate(0);
    }

    // ================= 每帧更新 =================

    /**
     * 由外部 AnimationTimer 每帧调用。
     *
     * @param dt 距上一帧的秒数
     */
    public void update(double dt) {
        switch (state) {
            case IDLE -> updateIdle(dt);
            case HIT_KNOCK -> updateHitKnock(dt);
            case ATTACK_DASH -> updateAttackDash(dt);
            case HURT_SHAKE -> updateHurtShake(dt);
        }
    }

    // ---- IDLE：待机呼吸 ----
    private void updateIdle(double dt) {
        if (!hasIdleBreath) return;
        idleTime += dt;
        double s = idleTime;
        target.setTranslateY(Math.sin(s * 2.2) * 5);
        target.setRotate(Math.sin(s * 2.2) * 1.2);
    }

    // ---- HIT_KNOCK：受击弹性弹开 ----
    private void updateHitKnock(double dt) {
        double dur = 0.25;
        timer += dt;
        target.setTranslateX(easeOutElastic(timer, knockOffset, -knockOffset, dur));
        if (timer > dur) {
            target.setTranslateX(0);
            state = State.IDLE;
        }
    }

    // ---- ATTACK_DASH：攻击前冲后收回 ----
    private void updateAttackDash(double dt) {
        double total = 0.40;
        double peak = total * 0.45;
        timer += dt;
        double x;
        if (timer < peak) {
            x = easeOutCubic(timer, 0, attackOffset, peak);
        } else if (timer < total) {
            x = easeInCubic(timer - peak, attackOffset, -attackOffset, total - peak);
        } else {
            x = 0;
            state = State.IDLE;
        }
        target.setTranslateX(x);
    }

    // ---- HURT_SHAKE：受击左右抖动 ----
    // 原 Timeline：0ms→0, 60ms→-12, 120ms→+10, 200ms→0
    private void updateHurtShake(double dt) {
        double dur = 0.20;
        timer += dt;
        double x;
        if (timer < 0.06) {
            x = lerp(0, -12, timer / 0.06);
        } else if (timer < 0.12) {
            x = lerp(-12, 10, (timer - 0.06) / 0.06);
        } else if (timer < dur) {
            x = lerp(10, 0, (timer - 0.12) / 0.08);
        } else {
            x = 0;
            state = State.IDLE;
        }
        target.setTranslateX(x);
    }

    // ========= 缓动函数 =========

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static double easeOutCubic(double t, double b, double c, double d) {
        t /= d;
        return c * (t - 1) * t * t + 1 + b;
    }

    private static double easeInCubic(double t, double b, double c, double d) {
        t /= d;
        return c * t * t * t + b;
    }

    private static double easeOutElastic(double t, double b, double c, double d) {
        if ((t /= d) == 1) return b + c;
        double p = d * 0.3;
        double a = c;
        double s = p / 4;
        return a * Math.pow(2, -10 * t) * Math.sin((t * d - s) * (2 * Math.PI) / p) + c + b;
    }
}
