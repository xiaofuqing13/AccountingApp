package com.loveapp.accountbook.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.AccountEntry
import com.loveapp.accountbook.data.model.Category
import com.loveapp.accountbook.data.repository.ExcelRepository
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.DraftManager
import com.loveapp.accountbook.util.EasterEggManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AccountAddFragment : Fragment() {

    private val viewModel: AccountViewModel by activityViewModels()
    private lateinit var repo: ExcelRepository
    private var isExpense = true
    private var selectedCategory = "餐饮"
    private var amountStr = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_account_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = ExcelRepository(requireContext())

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
        val keys = listOf("1","2","3","CAL","4","5","6","+","7","8","9","-","·","0","BS","OK")
        val iconKeys = mapOf("CAL" to R.drawable.ic_calendar, "BS" to R.drawable.ic_key_backspace, "OK" to R.drawable.ic_key_confirm)
        for (key in keys) {
            val cellParams = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            val iconRes = iconKeys[key]
            if (iconRes != null) {
                // 图标按钮
                val iv = ImageView(requireContext()).apply {
                    setImageResource(iconRes)
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), if (key == "OK") R.color.text_white else R.color.text_primary)
                    )
                    scaleType = ImageView.ScaleType.CENTER
                    setPadding(0, 28, 0, 28)
                    layoutParams = cellParams
                    if (key == "OK") setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pink_primary))
                    setOnClickListener { onKeyPress(key, tvAmount, etNote) }
                }
                numpad.addView(iv)
            } else {
                val btn = TextView(requireContext()).apply {
                    text = key
                    textSize = 20f
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 24, 0, 24)
                    layoutParams = cellParams
                    when (key) {
                        "+", "-" -> setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pink_bg))
                    }
                    setOnClickListener { onKeyPress(key, tvAmount, etNote) }
                }
                numpad.addView(btn)
            }
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }
    }

    private fun onKeyPress(key: String, tvAmount: TextView, etNote: EditText) {
        when (key) {
            "BS" -> { if (amountStr.isNotEmpty()) amountStr = amountStr.dropLast(1) }
            "·" -> { if (!amountStr.contains(".")) amountStr += "." }
            "OK" -> { saveAccount(etNote); return }
            "CAL", "+", "-" -> return
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
        lifecycleScope.launch {
            val type = if (isExpense) "支出" else "收入"
            val builtIn = if (isExpense) AccountEntry.EXPENSE_CATEGORIES else AccountEntry.INCOME_CATEGORIES
            // 内置分类去掉"更多"，末尾追加自定义分类 + 添加按钮
            val base = builtIn.filter { it.name != "更多" }
            val custom = repo.getCustomCategories(type)
            val categories = base + custom + Category(R.drawable.ic_add, "添加")
            selectedCategory = base.first().name

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
                    val isAddBtn = cat.name == "添加"
                    (holder.itemView as TextView).apply {
                        text = "${cat.icon}\n${cat.name}"
                        setTextColor(ContextCompat.getColor(requireContext(),
                            if (isAddBtn) R.color.text_hint
                            else if (position == selected) R.color.pink_primary
                            else R.color.text_primary))
                        setBackgroundResource(if (!isAddBtn && position == selected) R.drawable.bg_tag_pink else 0)
                        setOnClickListener {
                            if (isAddBtn) {
                                showAddCategoryDialog(rv)
                            } else {
                                val adapterPos = holder.bindingAdapterPosition
                                if (adapterPos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return@setOnClickListener
                                val old = selected
                                @Suppress("DEPRECATION")
                                selected = adapterPos
                                selectedCategory = cat.name
                                notifyItemChanged(old)
                                notifyItemChanged(selected)
                            }
                        }
                        // 长按删除自定义分类
                        setOnLongClickListener {
                            val adapterPos = holder.bindingAdapterPosition
                            if (adapterPos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return@setOnLongClickListener true
                            val currentCat = categories[adapterPos]
                            val isCurrentAddBtn = currentCat.name == "添加"
                            if (isCurrentAddBtn) {
                                Toast.makeText(requireContext(), "请点击“添加”按钮新增分类", Toast.LENGTH_SHORT).show()
                                return@setOnLongClickListener true
                            }
                            val isCustomCategory = adapterPos >= base.size && adapterPos < categories.size - 1
                            if (!isCustomCategory) {
                                Toast.makeText(requireContext(), "内置分类不支持删除，请长按自定义分类", Toast.LENGTH_SHORT).show()
                                return@setOnLongClickListener true
                            }
                            android.app.AlertDialog.Builder(requireContext())
                                .setTitle("删除分类")
                                .setMessage("确定删除「${currentCat.name}」分类吗？")
                                .setPositiveButton("删除") { _, _ ->
                                    lifecycleScope.launch {
                                        repo.deleteCustomCategory(currentCat.name, type)
                                        updateCategoryGrid(rv)
                                    }
                                }
                                .setNegativeButton("取消", null)
                                .show()
                            true
                        }
                    }
                }
                override fun getItemCount() = categories.size
            }
        }
    }

    private fun showAddCategoryDialog(rv: androidx.recyclerview.widget.RecyclerView) {
        val type = if (isExpense) "支出" else "收入"

        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }
        val etName = EditText(requireContext()).apply {
            hint = "分类名称（如：零食）"
            textSize = 15f
        }
        layout.addView(etName)

        var selectedIcon = R.drawable.ic_cat_food.toString()
        val defaultIcons = listOf(
            R.drawable.ic_cat_food, R.drawable.ic_cat_transport, R.drawable.ic_cat_shopping,
            R.drawable.ic_cat_house, R.drawable.ic_cat_phone, R.drawable.ic_cat_game,
            R.drawable.ic_cat_medical, R.drawable.ic_cat_education, R.drawable.ic_cat_clothes,
            R.drawable.ic_cat_salary, R.drawable.ic_cat_bonus, R.drawable.ic_cat_invest,
            R.drawable.ic_cat_parttime, R.drawable.ic_cat_other, R.drawable.ic_cat_more
        )

        val tvIconLabel = TextView(requireContext()).apply {
            text = "选择图标："
            textSize = 14f
            setPadding(0, 24, 0, 8)
        }
        layout.addView(tvIconLabel)

        val iconGrid = GridLayout(requireContext()).apply {
            columnCount = 5
            setPadding(0, 8, 0, 16)
        }

        defaultIcons.forEach { iconRes ->
            val iv = ImageView(requireContext()).apply {
                setImageResource(iconRes)
                imageTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.pink_primary)
                )
                setPadding(16, 12, 16, 12)
                layoutParams = ViewGroup.LayoutParams(144, 120)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setOnClickListener {
                    selectedIcon = iconRes.toString()
                    for (i in 0 until iconGrid.childCount) {
                        iconGrid.getChildAt(i).alpha = 0.5f
                    }
                    alpha = 1f
                }
                alpha = if (iconRes.toString() == selectedIcon) 1f else 0.5f
            }
            iconGrid.addView(iv)
        }
        layout.addView(iconGrid)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("添加${type}分类")
            .setView(layout)
            .setPositiveButton("添加") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入分类名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    repo.addCustomCategory(name, type, selectedIcon)
                    updateCategoryGrid(rv)
                    Toast.makeText(requireContext(), "已添加分类「$name」", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }
}
