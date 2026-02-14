package com.loveapp.accountbook.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.loveapp.accountbook.R
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.EasterEggManager
import com.loveapp.accountbook.util.LoveWord

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private var bannerClickCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 问候语
        val greeting = DateUtils.getGreeting()
        view.findViewById<TextView>(R.id.tv_greeting).text = greeting.first
        view.findViewById<TextView>(R.id.tv_greeting_sub).text = greeting.second
        view.findViewById<TextView>(R.id.tv_date).text = DateUtils.todayDisplay()

        // 实时计时器
        startCounter(view)

        // 统计卡片
        viewModel.diaryCount.observe(viewLifecycleOwner) {
            view.findViewById<TextView>(R.id.tv_stat_diary).text = it.toString()
        }
        viewModel.accountCount.observe(viewLifecycleOwner) {
            view.findViewById<TextView>(R.id.tv_stat_account).text = it.toString()
        }

        // 在一起天数
        val days = DateUtils.getTogetherTime().days
        view.findViewById<TextView>(R.id.tv_stat_days).text = days.toString()

        // 彩蛋: 点击"在一起的日子"卡片
        view.findViewById<View>(R.id.card_days).setOnClickListener {
            EasterEggManager.showRandomLovePopup(requireContext())
        }

        // 彩蛋: Banner连点5次
        view.findViewById<View>(R.id.banner).setOnClickListener {
            bannerClickCount++
            if (bannerClickCount >= 5) {
                bannerClickCount = 0
                EasterEggManager.showLovePopup(requireContext(),
                    LoveWord("🎉", "被你发现了！", "连续点了5次，\n说明你很认真在看对不对？\n\n那我告诉你一个秘密：\n我真的真的很喜欢你。"))
            }
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ bannerClickCount = 0 }, 2000)
        }

        // 快捷操作导航
        view.findViewById<View>(R.id.btn_quick_account).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_account_add)
        }
        view.findViewById<View>(R.id.btn_quick_diary).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_diary_add)
        }
        view.findViewById<View>(R.id.btn_quick_meeting).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_meeting_add)
        }
        view.findViewById<View>(R.id.btn_quick_stats).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_account_stats)
        }
        view.findViewById<View>(R.id.btn_quick_export).setOnClickListener {
            val repo = com.loveapp.accountbook.data.repository.ExcelRepository(requireContext())
            android.widget.Toast.makeText(requireContext(), "文件位于: ${repo.getExcelFilePath()}", android.widget.Toast.LENGTH_LONG).show()
        }
        view.findViewById<View>(R.id.btn_quick_settings).setOnClickListener {
            findNavController().navigate(R.id.nav_settings)
        }

        viewModel.loadStats()
    }

    private fun startCounter(view: View) {
        val tvDays = view.findViewById<TextView>(R.id.tv_counter_days)
        val tvHours = view.findViewById<TextView>(R.id.tv_counter_hours)
        val tvMins = view.findViewById<TextView>(R.id.tv_counter_mins)
        val tvSecs = view.findViewById<TextView>(R.id.tv_counter_secs)

        val runnable = object : Runnable {
            override fun run() {
                val t = DateUtils.getTogetherTime()
                tvDays.text = t.days.toString()
                tvHours.text = t.hours.toString().padStart(2, '0')
                tvMins.text = t.minutes.toString().padStart(2, '0')
                tvSecs.text = t.seconds.toString().padStart(2, '0')
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}
