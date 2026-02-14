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

class AccountStatsFragment : Fragment() {

    private val viewModel: AccountViewModel by activityViewModels()

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
            val days = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
            view.findViewById<TextView>(R.id.tv_daily_avg).text = "¥${String.format("%,.0f", it / days)}"
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
                    holder.itemView.findViewById<TextView>(R.id.tv_amount).setTextColor(resources.getColor(R.color.expense_pink, null))
                }
                override fun getItemCount() = stats.size
            }
        }

        viewModel.loadAccounts()
    }
}
