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
                when {
                    entry.amount == 520.0 -> EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg520)
                    entry.amount == 1314.0 -> EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg1314)
                    entry.amount == 777.0 -> EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg777)
                    isCategory(entry.category, "鲜花", "椴滆姳") && entry.amount == 99.0 -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg99)
                    }
                    isCategory(entry.category, "鲜花", "礼物", "椴滆姳", "绀肩墿") -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggFlower)
                    }
                    isCategory(entry.category, "工资", "宸ヨ祫") -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggSalary)
                    }
                    entry.note.contains("礼物") || entry.note.contains("生日") ||
                        entry.note.contains("绀肩墿") || entry.note.contains("鐢熸棩") -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggGift)
                    }
                }
            },
            onLongClick = { entry -> showEditDeleteDialog(entry) }
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

    private fun showEditDeleteDialog(entry: AccountEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("${entry.category}  ¥${String.format("%.2f", entry.amount)}")
            .setItems(arrayOf("编辑备注", "删除")) { _, which ->
                when (which) {
                    0 -> showEditNoteDialog(entry)
                    1 -> showDeleteConfirmDialog(entry)
                }
            }
            .show()
    }

    private fun showEditNoteDialog(entry: AccountEntry) {
        val etNote = EditText(requireContext()).apply {
            setText(entry.note)
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("编辑备注")
            .setView(etNote)
            .setPositiveButton("保存") { _, _ ->
                viewModel.updateAccount(entry.copy(note = etNote.text.toString()))
            }
            .setNegativeButton("取消", null)
            .show()
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
