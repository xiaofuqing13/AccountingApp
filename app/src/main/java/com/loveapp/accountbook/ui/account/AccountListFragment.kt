package com.loveapp.accountbook.ui.account

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
import com.loveapp.accountbook.R
import com.loveapp.accountbook.ui.adapter.AccountAdapter
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.EasterEggManager

class AccountListFragment : Fragment() {

    private val viewModel: AccountViewModel by activityViewModels()
    private lateinit var adapter: AccountAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_account_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AccountAdapter { entry ->
            when {
                entry.amount == 520.0 -> EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg520)
                entry.amount == 99.0 && entry.category == "鲜花" -> EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg99)
                entry.category == "工资" -> EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggSalary)
            }
        }

        view.findViewById<RecyclerView>(R.id.rv_accounts).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AccountListFragment.adapter
        }

        val tvMonth = view.findViewById<TextView>(R.id.tv_month)
        val tvIncome = view.findViewById<TextView>(R.id.tv_income)
        val tvExpense = view.findViewById<TextView>(R.id.tv_expense)
        val tvBalance = view.findViewById<TextView>(R.id.tv_balance)

        viewModel.currentMonth.observe(viewLifecycleOwner) {
            tvMonth.text = DateUtils.formatMonth(it)
        }
        viewModel.accounts.observe(viewLifecycleOwner) { adapter.updateData(it) }
        viewModel.totalIncome.observe(viewLifecycleOwner) { tvIncome.text = "¥${String.format("%,.0f", it)}" }
        viewModel.totalExpense.observe(viewLifecycleOwner) { tvExpense.text = "¥${String.format("%,.0f", it)}" }
        viewModel.balance.observe(viewLifecycleOwner) { tvBalance.text = "¥${String.format("%,.0f", it)}" }

        view.findViewById<View>(R.id.btn_prev_month).setOnClickListener { viewModel.prevMonth() }
        view.findViewById<View>(R.id.btn_next_month).setOnClickListener { viewModel.nextMonth() }
        view.findViewById<View>(R.id.btn_stats).setOnClickListener { findNavController().navigate(R.id.action_account_to_stats) }
        view.findViewById<View>(R.id.fab_add).setOnClickListener { findNavController().navigate(R.id.action_account_to_add) }

        viewModel.loadAccounts()
    }
}
