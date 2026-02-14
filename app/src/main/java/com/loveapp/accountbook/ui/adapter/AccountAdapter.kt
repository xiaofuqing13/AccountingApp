package com.loveapp.accountbook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.AccountEntry

class AccountAdapter(
    private var items: List<AccountEntry> = emptyList(),
    private val onItemClick: ((AccountEntry) -> Unit)? = null
) : RecyclerView.Adapter<AccountAdapter.ViewHolder>() {

    private val categoryIcons = mapOf(
        "餐饮" to "🍔", "交通" to "🚗", "购物" to "🛒", "住房" to "🏠",
        "通讯" to "📱", "娱乐" to "🎮", "医疗" to "💊", "教育" to "📚",
        "服饰" to "👗", "工资" to "💼", "奖金" to "🎁", "理财" to "💰",
        "兼职" to "🤝", "鲜花" to "🌷", "礼物" to "🎀"
    )

    fun updateData(newItems: List<AccountEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvIcon.text = categoryIcons[item.category] ?: "📌"
        holder.tvCategory.text = item.category
        holder.tvNote.text = item.note.ifEmpty { item.category }

        val prefix = if (item.isIncome) "+" else "-"
        holder.tvAmount.text = "${prefix}¥${String.format("%.2f", item.amount)}"
        holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.context,
            if (item.isIncome) R.color.income_green else R.color.expense_pink))

        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tv_icon)
        val tvCategory: TextView = view.findViewById(R.id.tv_category)
        val tvNote: TextView = view.findViewById(R.id.tv_note)
        val tvAmount: TextView = view.findViewById(R.id.tv_amount)
    }
}
