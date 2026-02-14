package com.loveapp.accountbook.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.repository.ExcelRepository
import com.loveapp.accountbook.util.EasterEggManager

class SettingsFragment : Fragment() {

    private var versionClickCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo = ExcelRepository(requireContext())

        // 导出
        view.findViewById<View>(R.id.btn_export).setOnClickListener {
            Toast.makeText(requireContext(), "文件位于: ${repo.getExcelFilePath()}", Toast.LENGTH_LONG).show()
        }

        // 导入
        view.findViewById<View>(R.id.btn_import).setOnClickListener {
            Toast.makeText(requireContext(), "请选择Excel文件导入", Toast.LENGTH_SHORT).show()
        }

        // 彩蛋: 深色模式
        view.findViewById<View>(R.id.btn_dark_mode).setOnClickListener {
            EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggDarkMode)
        }

        // 彩蛋: 密码锁
        view.findViewById<View>(R.id.btn_password).setOnClickListener {
            EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggLock)
        }

        // 意见反馈 → 情书页
        view.findViewById<View>(R.id.btn_feedback).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_love)
        }

        // 彩蛋: 版本信息连点3次
        view.findViewById<View>(R.id.btn_version).setOnClickListener {
            versionClickCount++
            if (versionClickCount >= 3) {
                versionClickCount = 0
                EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggVersion)
            }
        }
    }
}
