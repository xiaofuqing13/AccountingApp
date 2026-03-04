package com.loveapp.accountbook.data.sync

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 公网连接监控器（全局单例）
 * 应用启动后自动持续检测公网连通性，连通时自动同步数据 + 上报设备心跳
 */
object ConnectionMonitor {

    private const val TAG = "ConnectionMonitor"
    private const val SERVER_URL = "https://resistive-diotic-jolie.ngrok-free.dev/api/auth/check"
    private const val HEARTBEAT_URL = "https://resistive-diotic-jolie.ngrok-free.dev/api/device/heartbeat"
    private const val HEARTBEAT_INTERVAL = 30_000L // 30秒心跳
    private const val SYNC_INTERVAL = 5 * 60_000L  // 5分钟同步一次
    private const val MAX_RETRY = 3

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _statusText = MutableLiveData("检测中...")
    val statusText: LiveData<String> = _statusText

    private var monitorJob: Job? = null
    private var failCount = 0
    private var appContext: Context? = null
    private var lastSyncTime = 0L

    fun startAutoMonitor(context: Context? = null) {
        if (context != null) appContext = context.applicationContext
        if (monitorJob?.isActive == true) return
        monitorJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            // 首次检测
            val firstOk = pingServer()
            withContext(Dispatchers.Main) {
                _isConnected.value = firstOk
                _statusText.value = if (firstOk) "云端已连接" else "云端未连接"
            }
            failCount = if (firstOk) 0 else 1

            // 首次连通，立即同步 + 上报设备信息
            if (firstOk) {
                autoSync()
                sendHeartbeat()
            }

            // 持续心跳
            while (isActive) {
                delay(HEARTBEAT_INTERVAL)
                val ok = pingServer()
                withContext(Dispatchers.Main) {
                    if (ok) {
                        failCount = 0
                        _isConnected.value = true
                        _statusText.value = "云端已连接"
                    } else {
                        failCount++
                        if (failCount >= MAX_RETRY) {
                            _isConnected.value = false
                            _statusText.value = "云端断联"
                        }
                    }
                }
                // 连通时上报心跳
                if (ok) sendHeartbeat()
                // 每5分钟自动同步一次
                if (ok && System.currentTimeMillis() - lastSyncTime > SYNC_INTERVAL) {
                    autoSync()
                }
            }
        }
    }

    private fun sendHeartbeat() {
        val ctx = appContext ?: return
        try {
            val json = JSONObject().apply {
                put("device_id", Build.FINGERPRINT)
                put("brand", Build.BRAND)
                put("model", Build.MODEL)
                put("android_version", Build.VERSION.RELEASE)
                put("battery", getBatteryLevel(ctx))
                put("app_version", getAppVersion(ctx))
                put("screen", getScreenSize(ctx))
                put("network", getNetworkType(ctx))
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(HEARTBEAT_URL)
                .addHeader("ngrok-skip-browser-warning", "true")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            response.close()
        } catch (_: Exception) { }
    }

    private fun getBatteryLevel(ctx: Context): Int {
        return try {
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        } catch (_: Exception) { 0 }
    }

    private fun getAppVersion(ctx: Context): String {
        return try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) { "unknown" }
    }

    private fun getScreenSize(ctx: Context): String {
        return try {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            "${dm.widthPixels}x${dm.heightPixels}"
        } catch (_: Exception) { "unknown" }
    }

    private fun getNetworkType(ctx: Context): String {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nc = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "无网络"
            when {
                nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动数据"
                nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "有线"
                else -> "其他"
            }
        } catch (_: Exception) { "未知" }
    }

    private suspend fun autoSync() {
        val ctx = appContext ?: return
        try {
            val result = SyncManager(ctx).syncAll()
            lastSyncTime = System.currentTimeMillis()
            if (result.success) {
                Log.i(TAG, "自动同步成功: 记账${result.accounts} 日记${result.diaries} 会议${result.meetings}")
            } else {
                Log.w(TAG, "自动同步失败: ${result.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "自动同步异常: ${e.message}")
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        failCount = 0
    }

    private fun pingServer(): Boolean {
        return try {
            val request = Request.Builder()
                .url(SERVER_URL)
                .addHeader("ngrok-skip-browser-warning", "true")
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
