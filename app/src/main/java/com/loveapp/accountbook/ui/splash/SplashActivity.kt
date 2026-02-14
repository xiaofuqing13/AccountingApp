package com.loveapp.accountbook.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.loveapp.accountbook.MainActivity
import com.loveapp.accountbook.R
import com.loveapp.accountbook.util.EasterEggManager

class SplashActivity : AppCompatActivity() {

    private var clickCount = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvLogo = findViewById<View>(R.id.tv_logo)
        val btnStart = findViewById<View>(R.id.btn_start)

        // 彩蛋: logo连点3次
        tvLogo.setOnClickListener {
            clickCount++
            if (clickCount >= 3) {
                clickCount = 0
                EasterEggManager.showLovePopup(this, EasterEggManager.loveWords[7])
            }
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ clickCount = 0 }, 1000)
        }

        // 彩蛋: 长按"开始使用"
        btnStart.setOnLongClickListener {
            EasterEggManager.showLovePopup(this, EasterEggManager.eggLongPress)
            true
        }

        btnStart.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
