package com.loveapp.accountbook.ui.account

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.loveapp.accountbook.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class AccountStatsFragment : Fragment() {

    private val viewModel: AccountViewModel by activityViewModels()
    private var filterDays = 1
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_account_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }

        val rvStats = view.findViewById<RecyclerView>(R.id.rv_category_stats)
        rvStats.layoutManager = LinearLayoutManager(requireContext())

        viewModel.totalExpense.observe(viewLifecycleOwner) {
            view.findViewById<TextView>(R.id.tv_total_expense).text = "¥${String.format("%,.0f", it)}"
            val avg = if (filterDays > 0) it / filterDays else 0.0
            view.findViewById<TextView>(R.id.tv_daily_avg).text = "¥${String.format("%,.0f", avg)}"
        }

        viewModel.accounts.observe(viewLifecycleOwner) {
            val stats = viewModel.getCategoryStats()
            rvStats.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
                    return object : RecyclerView.ViewHolder(itemView) {}
                }
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val (category, amount) = stats[position]
                    holder.itemView.findViewById<TextView>(R.id.tv_category).text = category
                    holder.itemView.findViewById<TextView>(R.id.tv_note).text = "共${viewModel.accounts.value?.count { e -> e.isExpense && e.category == category } ?: 0}笔"
                    holder.itemView.findViewById<TextView>(R.id.tv_amount).text = "¥${String.format("%,.0f", amount)}"
                    holder.itemView.findViewById<TextView>(R.id.tv_amount).setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_pink))
                }
                override fun getItemCount() = stats.size
            }
        }

        setupDateFilter(view)
    }

    private fun setupDateFilter(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_date_filter)
        val tvRange = view.findViewById<TextView>(R.id.tv_date_range)
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density
        val pinkColor = ContextCompat.getColor(ctx, R.color.pink_primary)
        val chipBg = ContextCompat.getColor(ctx, R.color.tag_chip_bg)
        val strokeColor = ContextCompat.getColor(ctx, R.color.tag_chip_stroke)
        val whiteColor = ContextCompat.getColor(ctx, R.color.text_white)
        val textColor = ContextCompat.getColor(ctx, R.color.text_primary)

        val labels = listOf("本月", "近三月", "本年", "全部", "自定义")
        val chips = mutableListOf<Chip>()

        fun styleChip(chip: Chip, selected: Boolean) {
            if (selected) {
                chip.chipBackgroundColor = ColorStateList.valueOf(pinkColor)
                chip.setTextColor(whiteColor)
                chip.chipStrokeWidth = 0f
            } else {
                chip.chipBackgroundColor = ColorStateList.valueOf(chipBg)
                chip.setTextColor(textColor)
                chip.chipStrokeWidth = 1f * dp
                chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
            }
        }

        fun applyFilter(startDate: String, endDate: String, label: String) {
            val start = sdf.parse(startDate)
            val end = sdf.parse(endDate)
            filterDays = if (start != null && end != null) {
                max(1, ((end.time - start.time) / 86400000 + 1).toInt())
            } else 1
            tvRange.text = "$startDate ~ $endDate"
            viewModel.loadAccountsByRange(startDate, endDate)
        }

        for (label in labels) {
            val chip = Chip(ctx).apply {
                text = label
                isCheckable = false
                textSize = 13f
                chipMinHeight = 32f * dp
            }
            styleChip(chip, label == "本月")
            chip.setOnClickListener {
                chips.forEach { c -> styleChip(c, c == chip) }
                when (label) {
                    "本月" -> {
                        val cal = Calendar.getInstance()
                        val end = sdf.format(cal.time)
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        applyFilter(sdf.format(cal.time), end, label)
                    }
                    "近三月" -> {
                        val cal = Calendar.getInstance()
                        val end = sdf.format(cal.time)
                        cal.add(Calendar.MONTH, -3)
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                        applyFilter(sdf.format(cal.time), end, label)
                    }
                    "本年" -> {
                        val cal = Calendar.getInstance()
                        val end = sdf.format(cal.time)
                        cal.set(Calendar.MONTH, Calendar.JANUARY)
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        applyFilter(sdf.format(cal.time), end, label)
                    }
                    "全部" -> {
                        tvRange.text = "所有记录"
                        filterDays = max(1, Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
                        viewModel.loadAccountsByRange("0000-00-00", "9999-99-99")
                    }
                    "自定义" -> showDateRangePicker(chips, ::styleChip, ::applyFilter)
                }
            }
            chips.add(chip)
            chipGroup.addView(chip)
        }

        // 默认加载本月
        chips.first().performClick()
    }

    private fun showDateRangePicker(
        chips: List<Chip>,
        styleChip: (Chip, Boolean) -> Unit,
        applyFilter: (String, String, String) -> Unit
    ) {
        val ctx = requireContext()
        val calStart = Calendar.getInstance()
        val calEnd = Calendar.getInstance()

        // 先选开始日期
        DatePickerDialog(ctx, { _, y, m, d ->
            calStart.set(y, m, d)
            // 再选结束日期
            DatePickerDialog(ctx, { _, y2, m2, d2 ->
                calEnd.set(y2, m2, d2)
                if (calEnd.before(calStart)) calEnd.time = calStart.time
                val startStr = sdf.format(calStart.time)
                val endStr = sdf.format(calEnd.time)
                applyFilter(startStr, endStr, "自定义")
            }, calEnd.get(Calendar.YEAR), calEnd.get(Calendar.MONTH), calEnd.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("选择结束日期")
                show()
            }
        }, calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH), calStart.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("选择开始日期")
            show()
        }
    }
}
