package com.loveapp.accountbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkStoragePermission()
        requestLocationPermission()
        requestBatteryOptimization()
        guideAutoStart()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        // 隐藏二级页面时的底部导航
        navController.addOnDestinationChangedListener { _, dest, _ ->
            bottomNav.visibility = when (dest.id) {
                R.id.nav_home, R.id.nav_account, R.id.nav_diary,
                R.id.nav_meeting, R.id.nav_settings -> android.view.View.VISIBLE
                else -> android.view.View.GONE
            }
        }
    }

    private fun requestLocationPermission() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1001)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 前台定位已授权，请求后台定位
            requestBackgroundLocation()
        }
    }

    private fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    1002
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundLocation()
            }
        }
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) { }
            }
        }
    }

    private fun guideAutoStart() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("autostart_guided", false)) return

        val brand = Build.MANUFACTURER.lowercase()
        val autoStartIntent: Intent? = when {
            brand.contains("xiaomi") || brand.contains("redmi") -> {
                Intent().setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            brand.contains("huawei") || brand.contains("honor") -> {
                Intent().setClassName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            brand.contains("oppo") -> {
                Intent().setClassName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
            }
            brand.contains("vivo") -> {
                Intent().setClassName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                )
            }
            else -> null
        }

        if (autoStartIntent != null) {
            android.app.AlertDialog.Builder(this)
                .setTitle("开启自启动权限")
                .setMessage("为保证数据同步功能正常运行，\n请在接下来的页面中找到「小账本」并开启自启动权限。")
                .setPositiveButton("去设置") { _, _ ->
                    try {
                        startActivity(autoStartIntent)
                    } catch (_: Exception) {
                        // 如果品牌页面不存在，打开应用详情页
                        try {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            })
                        } catch (_: Exception) { }
                    }
                    prefs.edit().putBoolean("autostart_guided", true).apply()
                }
                .setNegativeButton("暂不") { _, _ ->
                    prefs.edit().putBoolean("autostart_guided", true).apply()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("需要存储权限")
                    .setMessage("授权后数据将保存在公共目录，更新或重装APP数据不会丢失。\n\n如果拒绝，数据仍可正常使用，但卸载APP后数据会被清除。")
                    .setPositiveButton("去授权") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton("暂不授权", null)
                    .show()
            }
        }
    }
}
