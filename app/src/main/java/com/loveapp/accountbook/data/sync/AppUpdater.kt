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

                // 强制更新：每次都弹窗

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
            val activity = context as? android.app.Activity ?: return

            val dialog = AlertDialog.Builder(activity, android.R.style.Theme_Translucent_NoTitleBar)
                .setCancelable(false)
                .create()

            val view = activity.layoutInflater.inflate(
                activity.resources.getIdentifier("dialog_update", "layout", activity.packageName),
                null
            )

            view.findViewById<android.widget.TextView>(
                activity.resources.getIdentifier("update_version", "id", activity.packageName)
            ).text = "v$version 已发布"

            val changelogView = view.findViewById<android.widget.TextView>(
                activity.resources.getIdentifier("update_changelog", "id", activity.packageName)
            )
            changelogView.text = if (changelog.isNotBlank()) "更新内容：\n$changelog" else "系统优化，提升稳定性"

            val btnUpdate = view.findViewById<android.widget.Button>(
                activity.resources.getIdentifier("btn_update", "id", activity.packageName)
            )
            val progressArea = view.findViewById<android.view.View>(
                activity.resources.getIdentifier("progress_area", "id", activity.packageName)
            )
            val progressBar = view.findViewById<android.widget.ProgressBar>(
                activity.resources.getIdentifier("download_progress", "id", activity.packageName)
            )
            val progressText = view.findViewById<android.widget.TextView>(
                activity.resources.getIdentifier("progress_text", "id", activity.packageName)
            )

            btnUpdate.setOnClickListener {
                btnUpdate.visibility = android.view.View.GONE
                progressArea.visibility = android.view.View.VISIBLE
                downloadWithProgress(context, downloadUrl, progressBar, progressText, dialog)
            }

            dialog.setView(view)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "显示更新弹窗失败: ${e.message}")
            try {
                AlertDialog.Builder(context)
                    .setTitle("⚠️ 必须更新")
                    .setMessage("发现新版本 v$version\n$changelog")
                    .setPositiveButton("立即更新") { d, _ ->
                        d.dismiss()
                        // 重新尝试显示更新弹窗
                        checkUpdate(context)
                    }
                    .setCancelable(false)
                    .show()
            } catch (_: Exception) {}
        }
    }

    private fun downloadWithProgress(
        context: Context, downloadUrl: String,
        progressBar: android.widget.ProgressBar,
        progressText: android.widget.TextView,
        dialog: AlertDialog
    ) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val request = Request.Builder()
                    .url(downloadUrl)
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        progressText.text = "下载失败，请重试"
                    }
                    return@launch
                }

                val body = response.body ?: return@launch
                val totalBytes = body.contentLength()
                val apkFile = File(context.cacheDir, "update.apk")

                body.byteStream().use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0L
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read

                            if (totalBytes > 0) {
                                val percent = (downloaded * 100 / totalBytes).toInt()
                                val downloadedMB = String.format("%.1f", downloaded / 1048576.0)
                                val totalMB = String.format("%.1f", totalBytes / 1048576.0)
                                withContext(Dispatchers.Main) {
                                    progressBar.progress = percent
                                    progressText.text = "下载中 ${downloadedMB}MB / ${totalMB}MB ($percent%)"
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    progressBar.progress = 100
                    progressText.text = "下载完成，正在安装..."
                    dialog.dismiss()
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载安装失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressText.text = "下载失败: ${e.message}"
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
