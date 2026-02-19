package com.loveapp.accountbook.ui.love

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.loveapp.accountbook.R
import com.loveapp.accountbook.util.EasterEggManager
import com.loveapp.accountbook.util.LoveWord

class LoveMenuFragment : Fragment() {

    private var currentDaily: LoveWord = EasterEggManager.dailyLoveWord()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_love_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 返回按钮
        view.findViewById<TextView>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }

        // 每日情话
        updateDailyCard(view)

        // 换一句按钮
        view.findViewById<TextView>(R.id.btn_random).setOnClickListener {
            currentDaily = EasterEggManager.loveWords.random()
            updateDailyCard(view)
        }

        // 点击每日卡片弹出完整情话
        view.findViewById<CardView>(R.id.card_daily).setOnClickListener {
            EasterEggManager.showLovePopup(requireContext(), currentDaily)
        }

        // 分类标签
        val categories = EasterEggManager.getLoveWordCategories()
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_categories)
        val countText = view.findViewById<TextView>(R.id.tv_count)

        categories.keys.forEachIndexed { index, name ->
            val chip = Chip(requireContext()).apply {
                text = name
                isCheckable = true
                isChecked = index == categories.size - 1 // 默认选中"全部情话"
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    if (isChecked) ContextCompat.getColor(requireContext(), R.color.pink_primary)
                    else ContextCompat.getColor(requireContext(), R.color.pink_bg)
                )
                setTextColor(
                    if (isChecked) ContextCompat.getColor(requireContext(), R.color.text_white)
                    else ContextCompat.getColor(requireContext(), R.color.text_primary)
                )
                chipStrokeWidth = 1f
                chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.pink_soft)
                )
            }
            chipGroup.addView(chip)
        }

        // 默认显示全部情话
        val allWords = EasterEggManager.loveWords
        countText.text = "共 ${allWords.size} 条"
        showLoveWords(view, allWords)

        // 分类切换
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val checkedChip = group.findViewById<Chip>(checkedIds[0])
            val categoryName = checkedChip?.text?.toString() ?: return@setOnCheckedStateChangeListener
            val words = categories[categoryName] ?: allWords

            // 更新芯片颜色
            for (i in 0 until group.childCount) {
                val c = group.getChildAt(i) as? Chip ?: continue
                val isSelected = c.id == checkedIds[0]
                c.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    if (isSelected) ContextCompat.getColor(requireContext(), R.color.pink_primary)
                    else ContextCompat.getColor(requireContext(), R.color.pink_bg)
                )
                c.setTextColor(
                    if (isSelected) ContextCompat.getColor(requireContext(), R.color.text_white)
                    else ContextCompat.getColor(requireContext(), R.color.text_primary)
                )
            }

            countText.text = "共 ${words.size} 条"
            showLoveWords(view, words)
        }
    }

    private fun updateDailyCard(view: View) {
        view.findViewById<TextView>(R.id.tv_daily_emoji).text = currentDaily.emoji
        view.findViewById<TextView>(R.id.tv_daily_title).text = currentDaily.title
        view.findViewById<TextView>(R.id.tv_daily_text).text = currentDaily.text
    }

    private fun showLoveWords(view: View, words: List<LoveWord>) {
        val container = view.findViewById<LinearLayout>(R.id.love_list_container)
        container.removeAllViews()

        words.forEach { word ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_love_card, container, false)

            itemView.findViewById<TextView>(R.id.tv_emoji).text = word.emoji
            itemView.findViewById<TextView>(R.id.tv_title).text = word.title
            itemView.findViewById<TextView>(R.id.tv_preview).text = word.text.replace("\n", " ")

            itemView.setOnClickListener {
                EasterEggManager.showLovePopup(requireContext(), word)
            }

            itemView.setOnLongClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("情话", "${word.emoji} ${word.title}\n\n${word.text}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), getString(R.string.love_menu_copy_success), Toast.LENGTH_SHORT).show()
                true
            }

            container.addView(itemView)
        }
    }
}
