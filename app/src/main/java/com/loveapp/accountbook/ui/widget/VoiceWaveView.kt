package com.loveapp.accountbook.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.loveapp.accountbook.R

/**
 * 苹果风格语音录制波形动画视图
 * 显示多根圆角竖条，根据音频振幅平滑变化
 */
class VoiceWaveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barCount = 7
    private val barWidthDp = 4f
    private val barGapDp = 5f
    private val barMinHeightDp = 4f
    private val barMaxHeightDp = 32f
    private val barCornerDp = 2f

    private val dp = context.resources.displayMetrics.density
    private val barWidth = barWidthDp * dp
    private val barGap = barGapDp * dp
    private val barMinH = barMinHeightDp * dp
    private val barMaxH = barMaxHeightDp * dp
    private val barCorner = barCornerDp * dp

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pink_primary)
        style = Paint.Style.FILL
    }

    // 每根条当前显示高度 (0f ~ 1f)
    private val barLevels = FloatArray(barCount) { 0f }
    // 目标高度
    private val barTargets = FloatArray(barCount) { 0f }

    private val rect = RectF()

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 100
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            val fraction = it.animatedValue as Float
            for (i in barLevels.indices) {
                barLevels[i] += (barTargets[i] - barLevels[i]) * fraction * 0.6f
            }
            invalidate()
        }
    }

    // 闲置时的呼吸动画
    private var idleAnimator: ValueAnimator? = null
    private var idlePhase = 0f

    /**
     * 传入归一化振幅 (0f ~ 1f)
     */
    fun setAmplitude(amplitude: Float) {
        stopIdleAnimation()
        val amp = amplitude.coerceIn(0f, 1f)
        if (amp < 0.05f) {
            // 静音：全部归零，不抖动
            for (i in barTargets.indices) barTargets[i] = 0f
        } else {
            // 中间的条最高，两边递减，jitter 按振幅比例缩放
            for (i in 0 until barCount) {
                val distFromCenter = kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)
                val base = amp * (1f - distFromCenter * 0.5f)
                val jitter = amp * (Math.random().toFloat() * 0.15f - 0.075f)
                barTargets[i] = (base + jitter).coerceIn(0f, 1f)
            }
        }
        if (!animator.isRunning) animator.start()
    }

    /**
     * 重置为静止状态
     */
    fun reset() {
        stopIdleAnimation()
        playAnimator?.cancel()
        playAnimator = null
        animator.cancel()
        for (i in barLevels.indices) {
            barLevels[i] = 0f
            barTargets[i] = 0f
        }
        invalidate()
    }

    /**
     * 开始闲置呼吸动画（未录音时）
     */
    fun startIdleAnimation() {
        if (idleAnimator != null) return
        idleAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                idlePhase = it.animatedValue as Float
                for (i in 0 until barCount) {
                    val offset = i * 360f / barCount
                    barLevels[i] = 0.08f + 0.06f * kotlin.math.sin(Math.toRadians((idlePhase + offset).toDouble())).toFloat()
                }
                invalidate()
            }
            start()
        }
    }

    fun stopIdleAnimation() {
        idleAnimator?.cancel()
        idleAnimator = null
    }

    /**
     * 开始播放模拟动画（无法获取真实振幅时用）
     */
    private var playAnimator: ValueAnimator? = null
    private var playPhase = 0f

    fun startPlayingAnimation() {
        stopIdleAnimation()
        stopPlayingAnimation()
        playAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 600
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                playPhase = it.animatedValue as Float
                for (i in 0 until barCount) {
                    val offset = i * 360f / barCount
                    val wave = kotlin.math.sin(Math.toRadians((playPhase * 3 + offset).toDouble())).toFloat()
                    barLevels[i] = 0.25f + 0.35f * ((wave + 1f) / 2f) + (Math.random().toFloat() * 0.1f)
                }
                invalidate()
            }
            start()
        }
    }

    fun stopPlayingAnimation() {
        playAnimator?.cancel()
        playAnimator = null
        for (i in barLevels.indices) {
            barLevels[i] = 0f
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalW = (barWidth * barCount + barGap * (barCount - 1)).toInt() + paddingLeft + paddingRight
        val totalH = (barMaxH).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(totalW, widthMeasureSpec),
            resolveSize(totalH, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val totalBarsWidth = barWidth * barCount + barGap * (barCount - 1)
        val startX = (width - totalBarsWidth) / 2f
        val centerY = height / 2f

        for (i in 0 until barCount) {
            val h = barMinH + barLevels[i] * (barMaxH - barMinH)
            val x = startX + i * (barWidth + barGap)
            rect.set(x, centerY - h / 2f, x + barWidth, centerY + h / 2f)
            canvas.drawRoundRect(rect, barCorner, barCorner, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
        stopIdleAnimation()
        stopPlayingAnimation()
    }
}
