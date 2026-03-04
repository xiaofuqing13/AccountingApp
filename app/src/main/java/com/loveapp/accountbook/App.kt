package com.loveapp.accountbook

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.loveapp.accountbook.data.sync.AlarmKeepAliveReceiver
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
        // AlarmManager 兜底：链式调度每15分钟唤醒
        AlarmKeepAliveReceiver.schedule(this)
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
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "location_keep_alive",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
