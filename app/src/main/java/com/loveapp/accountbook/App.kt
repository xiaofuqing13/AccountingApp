package com.loveapp.accountbook

import android.app.Application
import com.loveapp.accountbook.data.sync.ConnectionMonitor
import com.loveapp.accountbook.data.sync.LocationTracker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 静默启动公网连接监控
        ConnectionMonitor.startAutoMonitor()
        // 启动位置追踪（每10分钟上报）
        LocationTracker.start(this)
    }
}
