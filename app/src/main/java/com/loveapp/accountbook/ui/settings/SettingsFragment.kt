package com.loveapp.accountbook.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.repository.ExcelRepository
import com.loveapp.accountbook.util.AppSettings
import com.loveapp.accountbook.util.EasterEggManager
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var versionClickCount = 0
    private var suppressSwitchCallbacks = false
    private lateinit var repo: ExcelRepository

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            try {
                requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                    repo.exportShareable(out)
                }
                Toast.makeText(requireContext(), "导出成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        importFromUri(uri)
    }

    private val legacyImportLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        importFromUri(uri, true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = ExcelRepository(requireContext())

        val switchAutoBackup = view.findViewById<SwitchMaterial>(R.id.switch_auto_backup)
        val switchDarkMode = view.findViewById<SwitchMaterial>(R.id.switch_dark_mode)
        val switchNotification = view.findViewById<SwitchMaterial>(R.id.switch_notification)
        val tvStoragePath = view.findViewById<TextView>(R.id.tv_storage_path)
        val tvVersion = view.findViewById<TextView>(R.id.tv_version_value)

        tvStoragePath.text = repo.getExcelFilePath()
        tvVersion.text = "v${getVersionName()}"

        suppressSwitchCallbacks = true
        switchAutoBackup.isChecked = AppSettings.isAutoBackupEnabled(requireContext())
        switchNotification.isChecked = AppSettings.isNotificationEnabled(requireContext())
        switchDarkMode.isChecked = isDarkModeCurrentlyEnabled()
        suppressSwitchCallbacks = false

        switchAutoBackup.setOnCheckedChangeListener { _, checked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            AppSettings.setAutoBackupEnabled(requireContext(), checked)
            Toast.makeText(
                requireContext(),
                if (checked) "已开启自动备份" else "已关闭自动备份",
                Toast.LENGTH_SHORT
            ).show()
        }

        switchNotification.setOnCheckedChangeListener { _, checked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            AppSettings.setNotificationEnabled(requireContext(), checked)
            Toast.makeText(
                requireContext(),
                if (checked) "已开启消息提醒" else "已关闭消息提醒",
                Toast.LENGTH_SHORT
            ).show()
        }

        switchDarkMode.setOnCheckedChangeListener { _, checked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            AppSettings.setDarkModeEnabled(requireContext(), checked)
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            Toast.makeText(
                requireContext(),
                if (checked) "已切换到深色模式" else "已切换到浅色模式",
                Toast.LENGTH_SHORT
            ).show()
        }

        view.findViewById<View>(R.id.btn_auto_backup).setOnClickListener { switchAutoBackup.toggle() }
        view.findViewById<View>(R.id.btn_notification).setOnClickListener { switchNotification.toggle() }
        view.findViewById<View>(R.id.btn_dark_mode).setOnClickListener { switchDarkMode.toggle() }

        view.findViewById<View>(R.id.btn_export).setOnClickListener {
            val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(java.util.Date())
            exportLauncher.launch("账本备份_${ts}.xlsx")
        }

        view.findViewById<View>(R.id.btn_import).setOnClickListener {
            importLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }

        view.findViewById<View>(R.id.btn_import_legacy).setOnClickListener {
            legacyImportLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }

        view.findViewById<View>(R.id.btn_import_sample).setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("导入示例数据")
                .setMessage("将写入一批示例记录，已存在数据不会被清空。是否继续？")
                .setPositiveButton("继续") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            val (accounts, diaries, meetings) = repo.importSampleData()
                            Toast.makeText(
                                requireContext(),
                                "导入完成：记账 $accounts 条，日记 $diaries 条，会议 $meetings 条",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(),
                                "导入失败：${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }

        view.findViewById<View>(R.id.btn_storage_path).setOnClickListener {
            copyToClipboard("账本路径", repo.getExcelFilePath())
            Toast.makeText(requireContext(), "路径已复制", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_password).setOnClickListener {
            EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggLock)
        }

        view.findViewById<View>(R.id.btn_feedback).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_love)
        }

        view.findViewById<View>(R.id.btn_love_menu).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_love_menu)
        }

        view.findViewById<View>(R.id.tv_version).setOnClickListener {
            versionClickCount++
            if (versionClickCount >= 3) {
                versionClickCount = 0
                EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggVersion)
            }
        }
    }

    private fun importFromUri(uri: android.net.Uri, legacyHint: Boolean = false) {
        lifecycleScope.launch {
            try {
                val result = requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    repo.importSmart(input)
                } ?: return@launch

                val prefix = if (legacyHint) "旧版导入完成" else "导入完成"
                Toast.makeText(
                    requireContext(),
                    "$prefix：记账 ${result.accounts} 条，日记 ${result.diaries} 条，会议 ${result.meetings} 条，分类 ${result.categories} 条",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "导入失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getVersionName(): String {
        return try {
            val pkgInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            pkgInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    private fun isDarkModeCurrentlyEnabled(): Boolean {
        val mode = AppCompatDelegate.getDefaultNightMode()
        return when (mode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                val nightMask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    }
}
