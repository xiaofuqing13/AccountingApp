package com.loveapp.accountbook

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.loveapp.accountbook.data.sync.ConnectionMonitor
import com.loveapp.accountbook.data.sync.LocationService

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 静默启动公网连接监控
        ConnectionMonitor.startAutoMonitor()
        // 启动定位前台服务（常驻后台，防止查杀）
        startLocationService()
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
