package com.loveapp.accountbook

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.loveapp.accountbook.data.sync.ConnectionMonitor
import com.loveapp.accountbook.data.sync.LocationKeepAliveWorker
import com.loveapp.accountbook.data.sync.LocationService
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 静默启动公网连接监控
        ConnectionMonitor.startAutoMonitor()
        // 启动定位前台服务（常驻后台，防止查杀）
        startLocationService()
        // WorkManager 兜底：每15分钟检查服务存活
        scheduleKeepAlive()
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun scheduleKeepAlive() {
        val request = PeriodicWorkRequestBuilder<LocationKeepAliveWorker>(
            15, TimeUnit.MINUTES  // WorkManager 最小间隔 15 分钟
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "location_keep_alive",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
