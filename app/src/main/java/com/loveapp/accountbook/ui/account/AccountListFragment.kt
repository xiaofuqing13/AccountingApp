package com.loveapp.accountbook.ui.account

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.AccountEntry
import com.loveapp.accountbook.data.repository.ExcelRepository
import com.loveapp.accountbook.ui.adapter.AccountAdapter
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.EasterEggManager
import kotlinx.coroutines.launch

class AccountListFragment : Fragment() {

    private val viewModel: AccountViewModel by activityViewModels()
    private lateinit var adapter: AccountAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_account_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AccountAdapter(
            onItemClick = { entry ->
                // 彩蛋触发
                when {
                    entry.amount == 520.0 -> EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg520)
                    entry.amount == 1314.0 -> EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg1314)
                    entry.amount == 777.0 -> EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg777)
                    isCategory(entry.category, "鲜花", "椮譬花") && entry.amount == 99.0 -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg99)
                    }
                    isCategory(entry.category, "鲜花", "礼物", "椮譬花", "纼肩墨") -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggFlower)
                    }
                    isCategory(entry.category, "工资", "宇ヨ祥") -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggSalary)
                    }
                    entry.note.contains("礼物") || entry.note.contains("生日") ||
                        entry.note.contains("纼肩墨") || entry.note.contains("鐐生棨") -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggGift)
                    }
                }
                showAccountDetailDialog(entry)
            },
            onLongClick = { entry -> showAccountDetailDialog(entry) }
        )

        view.findViewById<RecyclerView>(R.id.rv_accounts).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AccountListFragment.adapter
        }

        val tvMonth = view.findViewById<TextView>(R.id.tv_month)
        val tvIncome = view.findViewById<TextView>(R.id.tv_income)
        val tvExpense = view.findViewById<TextView>(R.id.tv_expense)
        val tvBalance = view.findViewById<TextView>(R.id.tv_balance)

        viewModel.currentMonth.observe(viewLifecycleOwner) { tvMonth.text = DateUtils.formatMonth(it) }
        viewModel.accounts.observe(viewLifecycleOwner) {
            adapter.updateData(it)
            view.findViewById<View>(R.id.empty_view)?.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.rv_accounts)?.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.totalIncome.observe(viewLifecycleOwner) { tvIncome.text = "¥${String.format("%,.0f", it)}" }
        viewModel.totalExpense.observe(viewLifecycleOwner) { tvExpense.text = "¥${String.format("%,.0f", it)}" }
        viewModel.balance.observe(viewLifecycleOwner) { tvBalance.text = "¥${String.format("%,.0f", it)}" }

        view.findViewById<View>(R.id.btn_prev_month).setOnClickListener { viewModel.prevMonth() }
        view.findViewById<View>(R.id.btn_next_month).setOnClickListener { viewModel.nextMonth() }
        view.findViewById<View>(R.id.btn_stats).setOnClickListener {
            findNavController().navigate(R.id.action_account_to_stats)
        }
        view.findViewById<View>(R.id.fab_add).setOnClickListener {
            findNavController().navigate(R.id.action_account_to_add)
        }

        view.findViewById<EditText?>(R.id.et_search)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { viewModel.searchAccounts(s.toString()) }
        })

        viewModel.loadAccounts()

        lifecycleScope.launch {
            val repo = ExcelRepository(requireContext())
            val customExpense = repo.getCustomCategories("支出") + repo.getCustomCategories("鏀嚭")
            val customIncome = repo.getCustomCategories("收入") + repo.getCustomCategories("鏀跺叆")
            val iconMap = (customExpense + customIncome).associate { it.name to it.icon }
            adapter.updateCustomIcons(iconMap)
        }
    }

    private fun showAccountDetailDialog(entry: AccountEntry) {
        val prefix = if (entry.isIncome) "+" else "-"
        val title = "${entry.category}  ${prefix}¥${String.format("%.2f", entry.amount)}"
        val details = StringBuilder().apply {
            appendLine("日期：${DateUtils.formatDateDisplay(entry.date)}")
            appendLine("类型：${entry.type}")
            appendLine("分类：${entry.category}")
            appendLine("备注：${entry.note.ifEmpty { "暂无" }}")
            if (entry.location.isNotBlank()) appendLine("位置：${entry.location}")
        }.toString().trimEnd()
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(details)
            .setPositiveButton("修改") { _, _ -> navigateToEdit(entry) }
            .setNegativeButton("关闭", null)
            .setNeutralButton("删除") { _, _ -> showDeleteConfirmDialog(entry) }
            .show()
    }

    private fun navigateToEdit(entry: AccountEntry) {
        val bundle = Bundle().apply {
            putInt("editRowIndex", entry.rowIndex)
            putString("editDate", entry.date)
            putString("editType", entry.type)
            putString("editCategory", entry.category)
            putDouble("editAmount", entry.amount)
            putString("editNote", entry.note)
            putString("editLocation", entry.location)
        }
        findNavController().navigate(R.id.action_account_to_add, bundle)
    }

    private fun showDeleteConfirmDialog(entry: AccountEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("删除「${entry.category}  ¥${String.format("%.2f", entry.amount)}」？")
            .setPositiveButton("删除") { _, _ -> viewModel.deleteAccount(entry) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun isCategory(value: String, vararg candidates: String): Boolean {
        return candidates.any { value.equals(it, ignoreCase = false) }
    }
}
