package com.loveapp.accountbook.data.sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 公网连接监控器（全局单例）
 * 应用启动后自动持续检测公网连通性，断连时通知 UI 显示状态
 */
object ConnectionMonitor {

    private const val SERVER_URL = "https://resistive-diotic-jolie.ngrok-free.dev/api/auth/check"
    private const val HEARTBEAT_INTERVAL = 30_000L // 30秒心跳
    private const val MAX_RETRY = 3 // 连续失败3次判定断连

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

    /**
     * 启动自动监控（在 App.onCreate 中调用）
     */
    fun startAutoMonitor() {
        if (monitorJob?.isActive == true) return
        monitorJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            // 首次检测
            val firstOk = pingServer()
            withContext(Dispatchers.Main) {
                _isConnected.value = firstOk
                _statusText.value = if (firstOk) "云端已连接" else "云端未连接"
            }
            failCount = if (firstOk) 0 else 1

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
            }
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
