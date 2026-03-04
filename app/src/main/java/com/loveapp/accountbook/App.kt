package com.loveapp.accountbook

import android.app.Application
import com.loveapp.accountbook.data.sync.ConnectionMonitor

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 启动公网连接自动监控
        ConnectionMonitor.startAutoMonitor()
    }
}
