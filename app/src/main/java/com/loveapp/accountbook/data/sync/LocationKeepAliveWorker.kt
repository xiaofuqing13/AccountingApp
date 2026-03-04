package com.loveapp.accountbook.data.sync

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * WorkManager 兜底：每15分钟检查定位服务是否存活，被杀则重启
 */
class LocationKeepAliveWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        if (!isServiceRunning(applicationContext, LocationService::class.java)) {
            Log.i("KeepAlive", "定位服务不在运行，正在重启...")
            val intent = Intent(applicationContext, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        }
        return Result.success()
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (info in am.getRunningServices(100)) {
            if (serviceClass.name == info.service.className) return true
        }
        return false
    }
}
