package com.loveapp.accountbook.ui.love

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_love_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }

        updateDailyCard(view)

        view.findViewById<TextView>(R.id.btn_random).setOnClickListener {
            currentDaily = EasterEggManager.loveWords.random()
            updateDailyCard(view)
        }

        view.findViewById<CardView>(R.id.card_daily).setOnClickListener {
            EasterEggManager.showLovePopup(requireContext(), currentDaily)
        }

        val categories = EasterEggManager.getLoveWordCategories()
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_categories)
        val countText = view.findViewById<TextView>(R.id.tv_count)

        categories.keys.forEachIndexed { index, name ->
            val checked = index == categories.size - 1
            val chip = Chip(requireContext()).apply {
                text = name
                isCheckable = true
                isChecked = checked
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    if (checked) ContextCompat.getColor(requireContext(), R.color.pink_primary)
                    else ContextCompat.getColor(requireContext(), R.color.pink_bg)
                )
                setTextColor(
                    if (checked) ContextCompat.getColor(requireContext(), R.color.text_white)
                    else ContextCompat.getColor(requireContext(), R.color.text_primary)
                )
                chipStrokeWidth = 1f
                chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.pink_soft)
                )
            }
            chipGroup.addView(chip)
        }

        val allWords = EasterEggManager.loveWords
        countText.text = "共 ${allWords.size} 条"
        showLoveWords(view, allWords)

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds[0]
            val checkedChip = group.findViewById<Chip>(checkedId)
            val categoryName = checkedChip?.text?.toString() ?: return@setOnCheckedStateChangeListener
            val words = categories[categoryName] ?: allWords

            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as? Chip ?: continue
                val selected = chip.id == checkedId
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    if (selected) ContextCompat.getColor(requireContext(), R.color.pink_primary)
                    else ContextCompat.getColor(requireContext(), R.color.pink_bg)
                )
                chip.setTextColor(
                    if (selected) ContextCompat.getColor(requireContext(), R.color.text_white)
                    else ContextCompat.getColor(requireContext(), R.color.text_primary)
                )
            }

            countText.text = "共 ${words.size} 条"
            showLoveWords(view, words)
        }
    }

    private fun updateDailyCard(view: View) {
        view.findViewById<ImageView>(R.id.iv_daily_icon).setImageResource(
            EasterEggManager.iconResForTag(currentDaily.tag)
        )
        view.findViewById<TextView>(R.id.tv_daily_title).text = currentDaily.title
        view.findViewById<TextView>(R.id.tv_daily_text).text = currentDaily.text
    }

    private fun showLoveWords(view: View, words: List<LoveWord>) {
        val container = view.findViewById<LinearLayout>(R.id.love_list_container)
        container.removeAllViews()

        words.forEach { word ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_love_card, container, false)

            itemView.findViewById<ImageView>(R.id.iv_icon).setImageResource(
                EasterEggManager.iconResForTag(word.tag)
            )
            itemView.findViewById<TextView>(R.id.tv_title).text = word.title
            itemView.findViewById<TextView>(R.id.tv_preview).text = word.text.replace("\n", " ")

            itemView.setOnClickListener {
                EasterEggManager.showLovePopup(requireContext(), word)
            }

            itemView.setOnLongClickListener {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("情话", "${word.title}\n\n${word.text}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                true
            }

            container.addView(itemView)
        }
    }
}
