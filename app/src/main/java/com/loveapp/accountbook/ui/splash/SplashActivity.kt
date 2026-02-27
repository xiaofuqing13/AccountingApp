package com.loveapp.accountbook.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.loveapp.accountbook.MainActivity
import com.loveapp.accountbook.R

class SplashActivity : AppCompatActivity() {

    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        startIntroAnimation()
        findViewById<TextView>(R.id.btn_start).setOnClickListener {
            navigateToMain()
        }
        window.decorView.postDelayed({
            navigateToMain()
        }, AUTO_JUMP_DELAY_MS)
    }

    private fun startIntroAnimation() {
        // 主体发光爱心 缩放弹入
        val mainIcon = findViewById<View>(R.id.iv_main_icon)
        mainIcon.scaleX = 0f
        mainIcon.scaleY = 0f
        mainIcon.alpha = 0f
        mainIcon.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600L)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction { startHeartbeatAnimation(mainIcon) }
            .start()

        // 应用名称滑入
        findViewById<View>(R.id.tv_app_name).apply {
            alpha = 0f
            translationY = 24f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(250L)
                .setDuration(400L)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }

        // 描述文字淡入
        findViewById<View>(R.id.tv_desc).apply {
            alpha = 0f
            translationY = 20f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(400L)
                .setDuration(350L)
                .start()
        }

        // 按钮从底部弹入
        findViewById<View>(R.id.btn_start).apply {
            alpha = 0f
            translationY = 30f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(550L)
                .setDuration(400L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .start()
        }

        // 浮动爱心动画
        startFloatingHeartsAnimation()
    }

    private fun startHeartbeatAnimation(view: View) {
        val scaleXUp = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f).apply { duration = 300 }
        val scaleYUp = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.08f).apply { duration = 300 }
        val scaleXDown = ObjectAnimator.ofFloat(view, "scaleX", 1.08f, 1f).apply { duration = 300 }
        val scaleYDown = ObjectAnimator.ofFloat(view, "scaleY", 1.08f, 1f).apply { duration = 300 }

        val beatUp = AnimatorSet().apply { playTogether(scaleXUp, scaleYUp) }
        val beatDown = AnimatorSet().apply { playTogether(scaleXDown, scaleYDown) }

        AnimatorSet().apply {
            playSequentially(beatUp, beatDown)
            startDelay = 200
            start()
        }
    }

    private fun startFloatingHeartsAnimation() {
        val heartIds = intArrayOf(
            R.id.heart_float_1, R.id.heart_float_2,
            R.id.heart_float_3, R.id.heart_float_4
        )
        val delays = longArrayOf(300L, 500L, 700L, 900L)
        val floatY = floatArrayOf(-15f, -12f, -18f, -10f)

        for (i in heartIds.indices) {
            val heart = findViewById<ImageView>(heartIds[i])
            heart.alpha = 0f
            heart.translationY = 20f

            // 淡入 + 上浮
            heart.animate()
                .alpha(0.6f)
                .translationY(0f)
                .setStartDelay(delays[i])
                .setDuration(600L)
                .withEndAction {
                    // 循环微微浮动
                    startGentleFloat(heart, floatY[i])
                }
                .start()
        }
    }

    private fun startGentleFloat(view: View, floatAmount: Float) {
        val floatUp = ObjectAnimator.ofFloat(view, "translationY", 0f, floatAmount).apply {
            duration = 1500
        }
        val floatDown = ObjectAnimator.ofFloat(view, "translationY", floatAmount, 0f).apply {
            duration = 1500
        }
        AnimatorSet().apply {
            playSequentially(floatUp, floatDown)
            start()
        }
    }

    private fun navigateToMain() {
        if (hasNavigated) return
        hasNavigated = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        private const val AUTO_JUMP_DELAY_MS = 2500L
    }
}
