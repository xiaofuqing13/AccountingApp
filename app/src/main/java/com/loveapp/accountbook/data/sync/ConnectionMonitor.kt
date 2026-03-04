package com.loveapp.accountbook.data.sync

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 公网连接监控器
 * 连接成功后持续心跳检测，失败时回调通知，连接期间不可手动取消
 */
class ConnectionMonitor(
    private val onStatusChanged: (Boolean, String) -> Unit,
    private val onConnectionLost: (String) -> Unit
) {
    companion object {
        const val SERVER_URL = "https://resistive-diotic-jolie.ngrok-free.dev/api/dashboard"
        const val HEARTBEAT_INTERVAL = 15_000L // 15秒心跳
        const val MAX_RETRY = 3 // 连续失败3次才判定断连
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var monitorJob: Job? = null
    private var isRunning = false
    private var failCount = 0

    val isConnected: Boolean get() = isRunning

    /**
     * 尝试连接并启动心跳监控
     * @return true=首次连接成功, false=首次连接失败
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        val success = pingServer()
        if (success) {
            isRunning = true
            failCount = 0
            onStatusChanged(true, "✅ 公网连接成功")
            startHeartbeat()
        }
        success
    }

    /**
     * 强制停止（仅在 Activity 销毁时调用）
     */
    fun forceStop() {
        monitorJob?.cancel()
        monitorJob = null
        isRunning = false
        failCount = 0
    }

    private fun startHeartbeat() {
        monitorJob?.cancel()
        monitorJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive && isRunning) {
                delay(HEARTBEAT_INTERVAL)
                val ok = pingServer()
                if (ok) {
                    failCount = 0
                    withContext(Dispatchers.Main) {
                        onStatusChanged(true, "✅ 公网连接正常")
                    }
                } else {
                    failCount++
                    if (failCount >= MAX_RETRY) {
                        isRunning = false
                        withContext(Dispatchers.Main) {
                            onConnectionLost("⚠️ 公网连接已断开\n连续 $MAX_RETRY 次心跳失败\n请检查远程服务器和 ngrok 是否正常运行")
                        }
                        break
                    } else {
                        withContext(Dispatchers.Main) {
                            onStatusChanged(true, "⚠️ 心跳异常 ($failCount/$MAX_RETRY)")
                        }
                    }
                }
            }
        }
    }

    private fun pingServer(): Boolean {
        return try {
            val request = Request.Builder()
                .url(SERVER_URL)
                .addHeader("ngrok-skip-browser-warning", "true")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.isSuccessful && body.contains("success")
        } catch (e: Exception) {
            false
        }
    }
}
