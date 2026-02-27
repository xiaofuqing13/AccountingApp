package com.loveapp.accountbook.ui.love

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.loveapp.accountbook.R
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.EasterEggManager

class LoveLetterFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_love_letter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }

        val days = DateUtils.getTogetherTime().days
        view.findViewById<TextView>(R.id.tv_letter_date).text =
            "${DateUtils.todayDisplay()} · 我们在一起的第${days}天"

        // 承诺卡片
        data class Promise(val text: String, val iconRes: Int)
        val promises = listOf(
            Promise("每天说晚安", R.drawable.ic_love_moon),
            Promise("永远站你这边", R.drawable.ic_promise),
            Promise("记得每个纪念日", R.drawable.ic_love_ring),
            Promise("不让你一个人淋雨", R.drawable.ic_love_rain),
            Promise("做你最温暖的依靠", R.drawable.ic_love_hands),
            Promise("生气了先道歉", R.drawable.ic_love_heart),
            Promise("存钱带你去旅行", R.drawable.ic_love_wave),
            Promise("给你做一辈子的饭", R.drawable.ic_love_cooking),
            Promise("永远觉得你最好看", R.drawable.ic_love_star),
            Promise("你的快乐我来守护", R.drawable.ic_love_sparkle),
            Promise("陪你看遍世界的日落", R.drawable.ic_love_sun),
            Promise("每天至少说三次我爱你", R.drawable.ic_love_heart),
            Promise("你冷的时候把外套给你", R.drawable.ic_love_scarf),
            Promise("永远记得你爱吃什么", R.drawable.ic_love_cake),
            Promise("吵架了绝不冷战", R.drawable.ic_love_rainbow),
            Promise("把最后一口留给你", R.drawable.ic_love_icecream)
        )
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_promises)
        promises.forEach { promise ->
            val chip = Chip(requireContext()).apply {
                text = promise.text
                chipIcon = ContextCompat.getDrawable(requireContext(), promise.iconRes)
                isChipIconVisible = true
                chipIconTint = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pink_primary))
                isClickable = true
                setChipBackgroundColorResource(R.color.pink_card)
                setChipStrokeColorResource(R.color.pink_soft)
                chipStrokeWidth = 2f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.pink_primary))
                setOnClickListener { EasterEggManager.showRandomLovePopup(requireContext()) }
            }
            chipGroup.addView(chip)
        }
    }
}
