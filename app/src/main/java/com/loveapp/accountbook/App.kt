package com.loveapp.accountbook

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
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

        try { ConnectionMonitor.startAutoMonitor(this) } catch (e: Exception) {
            Log.e("App", "ConnectionMonitor启动失败: ${e.message}")
        }

        try { startLocationService() } catch (e: Exception) {
            Log.e("App", "LocationService启动失败: ${e.message}")
        }

        try { scheduleKeepAlive() } catch (e: Exception) {
            Log.e("App", "WorkManager启动失败: ${e.message}")
        }

        try { AlarmKeepAliveReceiver.schedule(this) } catch (e: Exception) {
            Log.e("App", "AlarmManager启动失败: ${e.message}")
        }
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
