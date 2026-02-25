package com.loveapp.accountbook.ui.account

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.loveapp.accountbook.util.LocationHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AccountAddFragment : Fragment() {

    private val viewModel: AccountViewModel by activityViewModels()
    private lateinit var repo: ExcelRepository
    private var isExpense = true
    private var selectedCategory = "餐饮"
    private var amountStr = ""
    private var currentLocation = ""
    private lateinit var locationHelper: LocationHelper
    private val selectedCalendar: java.util.Calendar = java.util.Calendar.getInstance()
    private var editRowIndex = -1

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) locationHelper.fetchLocation { address ->
            currentLocation = address
            view?.findViewById<TextView>(R.id.tv_location)?.let { tv ->
                tv.text = address.ifBlank { "定位失败" }
                tv.setTextColor(ContextCompat.getColor(requireContext(),
                    if (address.isNotBlank()) R.color.text_primary else R.color.text_hint))
            }
        }
    }

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

        // 编辑模式：读取传入的记账数据
        editRowIndex = arguments?.getInt("editRowIndex", -1) ?: -1
        if (editRowIndex >= 0) {
            val editDate = arguments?.getString("editDate") ?: ""
            val editType = arguments?.getString("editType") ?: "支出"
            val editCategory = arguments?.getString("editCategory") ?: "餐饮"
            val editAmount = arguments?.getDouble("editAmount", 0.0) ?: 0.0
            val editNote = arguments?.getString("editNote") ?: ""
            currentLocation = arguments?.getString("editLocation") ?: ""

            isExpense = editType == "支出"
            selectedCategory = editCategory
            amountStr = if (editAmount == editAmount.toLong().toDouble()) editAmount.toLong().toString()
                else String.format("%.2f", editAmount).trimEnd('0').trimEnd('.')
            etNote.setText(editNote)
            tvAmount.text = "¥ ${String.format("%.2f", editAmount)}"

            // 解析日期时间
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                sdf.parse(editDate)?.let { selectedCalendar.time = it }
            } catch (_: Exception) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    sdf.parse(editDate)?.let { selectedCalendar.time = it }
                } catch (_: Exception) {}
            }
        }

        tvDate.text = DateUtils.formatDateTime(selectedCalendar)

        // 非编辑模式才恢复草稿
        if (editRowIndex < 0) {
            val draftAmount = DraftManager.getDraft(requireContext(), DraftManager.KEY_ACCOUNT_AMOUNT)
            val draftNote = DraftManager.getDraft(requireContext(), DraftManager.KEY_ACCOUNT_NOTE)
            if (!draftAmount.isNullOrBlank()) {
                amountStr = draftAmount
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                tvAmount.text = "¥ ${String.format("%.2f", amount)}"
                Toast.makeText(requireContext(), "已恢复上次编辑的草稿", Toast.LENGTH_SHORT).show()
            }
            if (!draftNote.isNullOrBlank()) etNote.setText(draftNote)
        }

        // 自动保存：绑定备注输入监听
        DraftManager.bindAutoSave(requireContext(), etNote, DraftManager.KEY_ACCOUNT_NOTE)

        // Tab: 支出/收入
        tabType.addTab(tabType.newTab().setText("支出"))
        tabType.addTab(tabType.newTab().setText("收入"))
        if (!isExpense) tabType.selectTab(tabType.getTabAt(1))

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

        // 定位显示
        val tvLocation = view.findViewById<TextView>(R.id.tv_location)
        locationHelper = LocationHelper(requireContext())

        fun updateLocationText(address: String) {
            currentLocation = address
            tvLocation.text = address.ifBlank { "定位失败" }
            tvLocation.setTextColor(ContextCompat.getColor(requireContext(),
                if (address.isNotBlank()) R.color.text_primary else R.color.text_hint))
        }

        // 编辑模式且已有位置时直接显示
        if (currentLocation.isNotBlank()) {
            updateLocationText(currentLocation)
        } else {
            if (locationHelper.hasPermission()) {
                locationHelper.fetchLocation { address -> updateLocationText(address) }
            } else {
                locationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }

        // 点击定位行刷新定位
        view.findViewById<View>(R.id.row_location).setOnClickListener {
            tvLocation.text = "定位中…"
            tvLocation.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
            if (locationHelper.hasPermission()) {
                locationHelper.fetchLocation { address -> updateLocationText(address) }
            } else {
                locationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }

        // 点击日期行弹出日期时间选择器
        tvDate.setOnClickListener { showDateTimePicker(tvDate) }
        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }
    }

    private fun showDateTimePicker(tvDate: TextView) {
        val year = selectedCalendar.get(java.util.Calendar.YEAR)
        val month = selectedCalendar.get(java.util.Calendar.MONTH)
        val day = selectedCalendar.get(java.util.Calendar.DAY_OF_MONTH)
        android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
            selectedCalendar.set(java.util.Calendar.YEAR, y)
            selectedCalendar.set(java.util.Calendar.MONTH, m)
            selectedCalendar.set(java.util.Calendar.DAY_OF_MONTH, d)
            val hour = selectedCalendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = selectedCalendar.get(java.util.Calendar.MINUTE)
            android.app.TimePickerDialog(requireContext(), { _, h, min ->
                selectedCalendar.set(java.util.Calendar.HOUR_OF_DAY, h)
                selectedCalendar.set(java.util.Calendar.MINUTE, min)
                tvDate.text = DateUtils.formatDateTime(selectedCalendar)
            }, hour, minute, true).show()
        }, year, month, day).show()
    }

    private fun onKeyPress(key: String, tvAmount: TextView, etNote: EditText) {
        when (key) {
            "BS" -> { if (amountStr.isNotEmpty()) amountStr = amountStr.dropLast(1) }
            "·" -> { if (!amountStr.contains(".")) amountStr += "." }
            "OK" -> { saveAccount(etNote); return }
            "CAL" -> { showDateTimePicker(tvAmount.rootView.findViewById(R.id.tv_date)); return }
            "+", "-" -> return
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
            date = DateUtils.formatDateTimeStore(selectedCalendar),
            type = if (isExpense) "支出" else "收入",
            category = selectedCategory,
            amount = amount,
            note = etNote.text.toString(),
            location = currentLocation,
            rowIndex = editRowIndex
        )
        if (editRowIndex >= 0) {
            viewModel.updateAccount(entry)
            Toast.makeText(requireContext(), "修改成功", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.addAccount(entry)
            Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
            if ((0..2).random() == 0) {
                EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggSaveSuccess)
            }
        }
        DraftManager.clearDrafts(requireContext(), "draft_account_")
        findNavController().popBackStack()
    }

    private fun getHiddenCategories(type: String): MutableSet<String> {
        val prefs = requireContext().getSharedPreferences("hidden_categories", android.content.Context.MODE_PRIVATE)
        return prefs.getStringSet("hidden_$type", emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun hideBuiltInCategory(type: String, name: String) {
        val prefs = requireContext().getSharedPreferences("hidden_categories", android.content.Context.MODE_PRIVATE)
        val hidden = getHiddenCategories(type)
        hidden.add(name)
        prefs.edit().putStringSet("hidden_$type", hidden).apply()
    }

    private fun updateCategoryGrid(rv: androidx.recyclerview.widget.RecyclerView) {
        lifecycleScope.launch {
            val type = if (isExpense) "支出" else "收入"
            val builtIn = if (isExpense) AccountEntry.EXPENSE_CATEGORIES else AccountEntry.INCOME_CATEGORIES
            val hidden = getHiddenCategories(type)
            val base = builtIn.filter { it.name != "更多" && it.name !in hidden }
            val custom = repo.getCustomCategories(type)
            val categories = base + custom + Category(R.drawable.ic_add, "添加")
            // 编辑模式下保留已选分类，新增模式才重置为第一个
            val existingIndex = categories.indexOfFirst { it.name == selectedCategory }
            if (existingIndex < 0) {
                selectedCategory = if (base.isNotEmpty()) base.first().name else if (custom.isNotEmpty()) custom.first().name else ""
            }

            rv.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                private var selected = if (existingIndex >= 0) existingIndex else 0
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
                    return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {}
                }
                override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                    val cat = categories[position]
                    val isAddBtn = cat.name == "添加"
                    val ivIcon = holder.itemView.findViewById<ImageView>(R.id.iv_icon)
                    val tvName = holder.itemView.findViewById<TextView>(R.id.tv_name)

                    val iconResId = cat.iconRes
                    if (iconResId != null) {
                        ivIcon.setImageResource(iconResId)
                    }
                    val tintColor = ContextCompat.getColor(requireContext(),
                        if (isAddBtn) R.color.text_hint
                        else if (position == selected) R.color.pink_primary
                        else R.color.text_secondary)
                    ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)

                    tvName.text = cat.name
                    tvName.setTextColor(ContextCompat.getColor(requireContext(),
                        if (isAddBtn) R.color.text_hint
                        else if (position == selected) R.color.pink_primary
                        else R.color.text_primary))

                    holder.itemView.setBackgroundResource(
                        if (!isAddBtn && position == selected) R.drawable.bg_tag_pink else 0)

                    holder.itemView.setOnClickListener {
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
                    // 长按删除分类
                    holder.itemView.setOnLongClickListener {
                        val adapterPos = holder.bindingAdapterPosition
                        if (adapterPos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return@setOnLongClickListener true
                        val currentCat = categories[adapterPos]
                        if (currentCat.name == "添加") return@setOnLongClickListener true
                        val isCustom = adapterPos >= base.size && adapterPos < categories.size - 1
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle("删除分类")
                            .setMessage("确定删除「${currentCat.name}」分类吗？")
                            .setPositiveButton("删除") { _, _ ->
                                lifecycleScope.launch {
                                    if (isCustom) {
                                        repo.deleteCustomCategory(currentCat.name, type)
                                    } else {
                                        hideBuiltInCategory(type, currentCat.name)
                                    }
                                    updateCategoryGrid(rv)
                                }
                            }
                            .setNegativeButton("取消", null)
                            .show()
                        true
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
