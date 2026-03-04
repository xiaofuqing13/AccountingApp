package com.loveapp.accountbook.data.sync

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * APK 自动更新管理器
 * 启动时检查服务器是否有新版本，有则弹窗提示下载安装
 */
object AppUpdater {

    private const val TAG = "AppUpdater"
    private const val BASE_URL = "https://resistive-diotic-jolie.ngrok-free.dev"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // 记录已弹窗的版本号，同一版本只弹一次
    private var lastPromptedCode = -1

    /**
     * 检查更新（在后台线程执行，有更新时切到主线程弹窗）
     */
    fun checkUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val versionCode = getVersionCode(context)
                val request = Request.Builder()
                    .url("$BASE_URL/api/app/check-update?versionCode=$versionCode")
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@launch

                val json = JSONObject(response.body?.string() ?: return@launch)
                if (!json.optBoolean("hasUpdate", false)) {
                    Log.i(TAG, "当前已是最新版本")
                    return@launch
                }

                val serverCode = json.optInt("versionCode", 0)
                val newVersion = json.optString("versionName", "")
                val changelog = json.optString("changelog", "")
                val downloadUrl = "$BASE_URL${json.optString("downloadUrl", "/api/app/latest")}"

                // 同一版本只弹一次
                if (serverCode <= lastPromptedCode) {
                    Log.i(TAG, "版本 $serverCode 已提示过，跳过")
                    return@launch
                }
                lastPromptedCode = serverCode

                // 切到主线程弹窗
                withContext(Dispatchers.Main) {
                    showUpdateDialog(context, newVersion, changelog, downloadUrl)
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查更新失败: ${e.message}")
            }
        }
    }

    /**
     * 启动推送轮询（每30秒检查服务器是否有推送指令）
     */
    fun startPushPolling(context: Context) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (true) {
                try {
                    delay(30_000) // 30秒轮询
                    val request = Request.Builder()
                        .url("$BASE_URL/api/app/check-push")
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .get()
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: continue)
                        if (json.optBoolean("pending", false)) {
                            Log.i(TAG, "收到推送更新指令，开始检查更新")
                            checkUpdate(context)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "轮询推送失败: ${e.message}")
                }
            }
        }
    }

    private fun showUpdateDialog(context: Context, version: String, changelog: String, downloadUrl: String) {
        try {
            val message = buildString {
                append("发现新版本 v$version")
                if (changelog.isNotBlank()) {
                    append("\n\n更新内容：\n$changelog")
                }
            }
            AlertDialog.Builder(context)
                .setTitle("📦 版本更新")
                .setMessage(message)
                .setPositiveButton("立即更新") { _, _ ->
                    downloadAndInstall(context, downloadUrl)
                }
                .setNegativeButton("稍后再说", null)
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "显示更新弹窗失败: ${e.message}")
        }
    }

    private fun downloadAndInstall(context: Context, downloadUrl: String) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "正在下载更新...", android.widget.Toast.LENGTH_LONG).show()
                }

                val request = Request.Builder()
                    .url(downloadUrl)
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "下载失败", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 保存到 cache 目录
                val apkFile = File(context.cacheDir, "update.apk")
                response.body?.byteStream()?.use { input ->
                    apkFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // 触发安装
                withContext(Dispatchers.Main) {
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载安装失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "下载失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "安装失败: ${e.message}")
            android.widget.Toast.makeText(context, "安装失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun getVersionCode(context: Context): Int {
        return try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }
}
