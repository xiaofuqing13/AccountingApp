package com.loveapp.accountbook.ui.love

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.loveapp.accountbook.R
import com.loveapp.accountbook.util.DateUtils

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
        val promises = listOf(
            "每天说晚安 🌙", "永远站你这边 🛡️", "记得每个纪念日 📅",
            "不让你一个人淋雨 🌂", "做你最温暖的依靠 🤗", "生气了先道歉 🙇",
            "存钱带你去旅行 ✈️", "给你做一辈子的饭 🍳"
        )
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_promises)
        promises.forEach { promise ->
            val chip = Chip(requireContext()).apply {
                text = promise
                isClickable = false
                setChipBackgroundColorResource(R.color.pink_card)
                setChipStrokeColorResource(R.color.pink_soft)
                chipStrokeWidth = 2f
                setTextColor(resources.getColor(R.color.pink_primary, null))
            }
            chipGroup.addView(chip)
        }
    }
}
