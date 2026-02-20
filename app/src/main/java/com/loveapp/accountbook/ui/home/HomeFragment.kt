package com.loveapp.accountbook.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val greeting = DateUtils.getGreeting()
        view.findViewById<TextView>(R.id.tv_greeting).text = greeting.first
        view.findViewById<TextView>(R.id.tv_greeting_sub).text = greeting.second
        view.findViewById<TextView>(R.id.tv_date).text = DateUtils.todayDisplay()

        startCounter(view)

        viewModel.diaryCount.observe(viewLifecycleOwner) {
            view.findViewById<TextView>(R.id.tv_stat_diary).text = it.toString()
        }
        viewModel.accountCount.observe(viewLifecycleOwner) {
            view.findViewById<TextView>(R.id.tv_stat_account).text = it.toString()
        }
        view.findViewById<TextView>(R.id.tv_stat_days).text = DateUtils.getTogetherTime().days.toString()

        view.findViewById<View>(R.id.card_days).setOnClickListener {
            EasterEggManager.showRandomLovePopup(requireContext())
        }

        view.findViewById<View>(R.id.banner).setOnClickListener {
            bannerClickCount++
            if (bannerClickCount >= 5) {
                bannerClickCount = 0
                EasterEggManager.showLovePopup(
                    requireContext(),
                    LoveWord(
                        "✨",
                        "被你发现啦！",
                        "连续点击了 5 次 Banner。\n你真的很认真在看我做的小细节。\n\n谢谢你，愿今天也被温柔对待。"
                    )
                )
            }
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ bannerClickCount = 0 }, 2000)
        }

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
            android.widget.Toast.makeText(
                requireContext(),
                "文件位置: ${repo.getExcelFilePath()}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
        view.findViewById<View>(R.id.btn_quick_settings).setOnClickListener {
            findNavController().navigate(R.id.nav_settings)
        }

        viewModel.loadStats()

        val dailyLove = EasterEggManager.dailyLoveWord()
        view.findViewById<ImageView>(R.id.iv_daily_love_icon)
            .setImageResource(EasterEggManager.iconResForLoveWord(dailyLove))
        view.findViewById<TextView>(R.id.tv_daily_love_title).text = dailyLove.title
        view.findViewById<TextView>(R.id.tv_daily_love_text).text = dailyLove.text
        view.findViewById<View>(R.id.card_daily_love).setOnClickListener {
            EasterEggManager.showLovePopup(requireContext(), dailyLove)
        }
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
