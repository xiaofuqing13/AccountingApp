package com.loveapp.accountbook.data.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

/**
 * AlarmManager 保活接收器
 * 每15分钟触发一次，检查并重启定位服务
 * 即使在 Doze 模式下也能触发（setExactAndAllowWhileIdle）
 */
class AlarmKeepAliveReceiver : BroadcastReceiver() {

    companion object {
        private const val INTERVAL = 5 * 60 * 1000L // 5分钟
        private const val REQUEST_CODE = 2001

        fun schedule(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmKeepAliveReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAt = SystemClock.elapsedRealtime() + INTERVAL
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ 用非精确闹钟，不需要 SCHEDULE_EXACT_ALARM 权限
                    am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
                } else {
                    am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
                }
            } catch (e: Exception) {
                // 兜底：如果还是失败就用普通闹钟
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            }
            Log.i("AlarmKeepAlive", "下次唤醒已安排：${INTERVAL / 60000}分钟后")
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.i("AlarmKeepAlive", "AlarmManager 触发，检查定位服务...")

        // 重启定位服务
        val serviceIntent = Intent(context, LocationService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("AlarmKeepAlive", "重启服务失败: ${e.message}")
        }

        // 重新安排下一次唤醒（链式调度）
        schedule(context)
    }
}
