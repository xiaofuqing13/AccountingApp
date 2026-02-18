package com.loveapp.accountbook.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.AccountEntry
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.DraftManager
import com.loveapp.accountbook.util.EasterEggManager

class AccountAddFragment : Fragment() {

    private val viewModel: AccountViewModel by activityViewModels()
    private var isExpense = true
    private var selectedCategory = "餐饮"
    private var amountStr = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_account_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvAmount = view.findViewById<TextView>(R.id.tv_amount)
        val tvDate = view.findViewById<TextView>(R.id.tv_date)
        val etNote = view.findViewById<EditText>(R.id.et_note)
        val tabType = view.findViewById<TabLayout>(R.id.tab_type)
        val numpad = view.findViewById<GridLayout>(R.id.numpad)

        tvDate.text = DateUtils.today()

        // 自动保存：恢复草稿
        val draftAmount = DraftManager.getDraft(requireContext(), DraftManager.KEY_ACCOUNT_AMOUNT)
        val draftNote = DraftManager.getDraft(requireContext(), DraftManager.KEY_ACCOUNT_NOTE)
        if (!draftAmount.isNullOrBlank()) {
            amountStr = draftAmount
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            tvAmount.text = "¥ ${String.format("%.2f", amount)}"
            Toast.makeText(requireContext(), "已恢复上次编辑的草稿", Toast.LENGTH_SHORT).show()
        }
        if (!draftNote.isNullOrBlank()) etNote.setText(draftNote)

        // 自动保存：绑定备注输入监听
        DraftManager.bindAutoSave(requireContext(), etNote, DraftManager.KEY_ACCOUNT_NOTE)

        // Tab: 支出/收入
        tabType.addTab(tabType.newTab().setText("支出"))
        tabType.addTab(tabType.newTab().setText("收入"))

        // 分类网格
        val rvCategories = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_categories)
        rvCategories.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 5)
        updateCategoryGrid(rvCategories)

        tabType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                isExpense = tab.position == 0
                updateCategoryGrid(rvCategories)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 数字键盘
        val keys = listOf("1","2","3","📅","4","5","6","+","7","8","9","-","·","0","⌫","✓")
        for (key in keys) {
            val btn = TextView(requireContext()).apply {
                text = key
                textSize = 20f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 24, 0, 24)
                layoutParams = GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                when (key) {
                    "+", "-" -> setBackgroundColor(resources.getColor(R.color.pink_bg, null))
                    "✓" -> { setBackgroundColor(resources.getColor(R.color.pink_primary, null)); setTextColor(resources.getColor(R.color.text_white, null)) }
                }
                setOnClickListener { onKeyPress(key, tvAmount, etNote) }
            }
            numpad.addView(btn)
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }
    }

    private fun onKeyPress(key: String, tvAmount: TextView, etNote: EditText) {
        when (key) {
            "⌫" -> { if (amountStr.isNotEmpty()) amountStr = amountStr.dropLast(1) }
            "·" -> { if (!amountStr.contains(".")) amountStr += "." }
            "✓" -> { saveAccount(etNote); return }
            "📅", "+", "-" -> return
            else -> {
                if (amountStr.contains(".") && amountStr.substringAfter(".").length >= 2) return
                amountStr += key
            }
        }
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        tvAmount.text = "¥ ${String.format("%.2f", amount)}"
        // 自动保存金额草稿
        DraftManager.saveDraft(requireContext(), DraftManager.KEY_ACCOUNT_AMOUNT, amountStr)
    }

    private fun saveAccount(etNote: EditText) {
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
            return
        }
        val entry = AccountEntry(
            date = DateUtils.today(),
            type = if (isExpense) "支出" else "收入",
            category = selectedCategory,
            amount = amount,
            note = etNote.text.toString()
        )
        viewModel.addAccount(entry)
        DraftManager.clearDrafts(requireContext(), "draft_account_")
        Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
        // 随机概率弹出保存惊喜
        if ((0..2).random() == 0) {
            EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggSaveSuccess)
        }
        findNavController().popBackStack()
    }

    private fun updateCategoryGrid(rv: androidx.recyclerview.widget.RecyclerView) {
        val categories = if (isExpense) AccountEntry.EXPENSE_CATEGORIES else AccountEntry.INCOME_CATEGORIES
        selectedCategory = categories.first().name
        rv.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            private var selected = 0
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    gravity = android.view.Gravity.CENTER
                    setPadding(8, 16, 8, 16)
                    textSize = 13f
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(tv) {}
            }
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val cat = categories[position]
                (holder.itemView as TextView).apply {
                    text = "${cat.icon}\n${cat.name}"
                    setTextColor(resources.getColor(if (position == selected) R.color.pink_primary else R.color.text_primary, null))
                    setBackgroundResource(if (position == selected) R.drawable.bg_tag_pink else 0)
                    setOnClickListener {
                        val old = selected
                        @Suppress("DEPRECATION")
                        selected = holder.adapterPosition
                        selectedCategory = cat.name
                        notifyItemChanged(old)
                        notifyItemChanged(selected)
                    }
                }
            }
            override fun getItemCount() = categories.size
        }
    }
}
