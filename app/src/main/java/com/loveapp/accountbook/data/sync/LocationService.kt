package com.loveapp.accountbook.data.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.SystemClock
import com.loveapp.accountbook.R

class LocationService : Service() {

    companion object {
        const val CHANNEL_ID = "location_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, buildNotification())
            LocationTracker.start(this)
            // 安排 AlarmManager 保活链
            AlarmKeepAliveReceiver.schedule(this)
            Log.i("LocationService", "前台服务已启动，保活已安排")
        } catch (e: Exception) {
            Log.e("LocationService", "启动失败: ${e.message}")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        LocationTracker.stop()
        // 被杀后尝试重启
        scheduleRestart()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 用户划掉任务卡时也重启
        scheduleRestart()
    }

    private fun scheduleRestart() {
        try {
            val restartIntent = Intent(this, LocationService::class.java)
            val pi = PendingIntent.getService(
                this, 9999, restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 5000, // 5秒后重启
                pi
            )
            Log.i("LocationService", "已安排5秒后自动重启")
        } catch (e: Exception) {
            Log.e("LocationService", "安排重启失败: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "数据同步",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "保持数据同步服务运行"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("小账本")
            .setContentText("数据同步中")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
