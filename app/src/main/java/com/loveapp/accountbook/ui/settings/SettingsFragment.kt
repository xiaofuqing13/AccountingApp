package com.loveapp.accountbook.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.repository.ExcelRepository
import com.loveapp.accountbook.util.EasterEggManager
import kotlinx.coroutines.launch
import java.io.File

class SettingsFragment : Fragment() {

    private var versionClickCount = 0
    private lateinit var repo: ExcelRepository

    // 导出：选择保存位置
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val sourceFile = File(repo.getExcelFilePath())
                requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { it.copyTo(out) }
                }
                Toast.makeText(requireContext(), "导出成功 ✅", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 导入：选择文件
    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val destFile = File(repo.getExcelFilePath())
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { input.copyTo(it) }
                }
                Toast.makeText(requireContext(), "导入成功，请重启应用 ✅", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = ExcelRepository(requireContext())

        // 导出
        view.findViewById<View>(R.id.btn_export).setOnClickListener {
            exportLauncher.launch("我们的小账本.xlsx")
        }

        // 导入
        view.findViewById<View>(R.id.btn_import).setOnClickListener {
            importLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }

        // 深色模式（真实功能）
        view.findViewById<View>(R.id.btn_dark_mode).setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(requireContext(), "已切换到浅色模式 ☀️", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(requireContext(), "已切换到深色模式 🌙", Toast.LENGTH_SHORT).show()
            }
        }

        // 密码锁（彩蛋保留）
        view.findViewById<View>(R.id.btn_password).setOnClickListener {
            EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggLock)
        }

        // 意见反馈 → 情书页
        view.findViewById<View>(R.id.btn_feedback).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_love)
        }

        // 情话小屋入口
        view.findViewById<View>(R.id.btn_love_menu)?.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_love_menu)
        }

        // 彩蛋: 版本信息连点3次
        view.findViewById<View>(R.id.tv_version).setOnClickListener {
            versionClickCount++
            if (versionClickCount >= 3) {
                versionClickCount = 0
                EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggVersion)
            }
        }
    }
}
