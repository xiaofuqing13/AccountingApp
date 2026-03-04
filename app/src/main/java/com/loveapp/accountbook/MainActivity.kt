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
import android.view.View
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
        // 主界面先隐藏，等权限全部通过再显示
        findViewById<View>(R.id.nav_host_fragment).visibility = View.GONE
        findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE

        checkAllPermissions()
    }

    override fun onResume() {
        super.onResume()
        // 从设置页面返回时重新检查权限
        checkAllPermissions()
    }

    /**
     * 统一检查所有必需权限，全部通过才放行
     */
    private fun checkAllPermissions() {
        when {
            // 1. 定位权限
            !hasLocationPermission() -> {
                showBlockDialog(
                    "需要定位权限",
                    "本应用需要定位权限才能正常运行。\n请授予定位权限后继续使用。"
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        1001
                    )
                }
            }
            // 2. 后台定位权限 (Android 10+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission() -> {
                showBlockDialog(
                    "需要始终允许定位",
                    "请选择「始终允许」定位权限，\n确保后台数据同步正常工作。"
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        1002
                    )
                }
            }
            // 3. 电池优化白名单
            !hasBatteryOptimization() -> {
                showBlockDialog(
                    "需要关闭电池优化",
                    "请允许应用不受电池优化限制，\n确保后台服务不被系统杀死。"
                ) {
                    try {
                        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        })
                    } catch (_: Exception) { }
                }
            }
            // 4. 存储权限 (Android 11+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager() -> {
                showBlockDialog(
                    "需要存储权限",
                    "授权后数据将保存在公共目录，\n更新或重装APP数据不会丢失。"
                ) {
                    try {
                        startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        })
                    } catch (_: Exception) {
                        startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                }
            }
            // 全部通过 → 显示主界面
            else -> {
                showMainUI()
                guideAutoStart()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun hasBatteryOptimization(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true
    }

    private fun showBlockDialog(title: String, message: String, onGo: () -> Unit) {
        // 隐藏主界面
        findViewById<View>(R.id.nav_host_fragment).visibility = View.GONE
        findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE

        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("去授权") { _, _ -> onGo() }
            .setNegativeButton("退出应用") { _, _ -> finishAffinity() }
            .setCancelable(false)
            .show()
    }

    private var mainUIShown = false

    private fun showMainUI() {
        if (mainUIShown) return
        mainUIShown = true

        findViewById<View>(R.id.nav_host_fragment).visibility = View.VISIBLE
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.visibility = View.VISIBLE

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, dest, _ ->
            bottomNav.visibility = when (dest.id) {
                R.id.nav_home, R.id.nav_account, R.id.nav_diary,
                R.id.nav_meeting, R.id.nav_settings -> View.VISIBLE
                else -> View.GONE
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 权限结果回来后重新走检查流程
        checkAllPermissions()
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
                        try {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            })
                        } catch (_: Exception) { }
                    }
                    prefs.edit().putBoolean("autostart_guided", true).apply()
                }
                .setNegativeButton("已开启") { _, _ ->
                    prefs.edit().putBoolean("autostart_guided", true).apply()
                }
                .setCancelable(false)
                .show()
        }
    }
}
