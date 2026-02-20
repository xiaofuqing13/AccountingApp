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
    private val onItemClick: ((AccountEntry) -> Unit)? = null,
    private val onLongClick: ((AccountEntry) -> Unit)? = null
) : RecyclerView.Adapter<AccountAdapter.ViewHolder>() {

    private val categoryIcons = mutableMapOf(
        "餐饮" to "🍜", "椁愰ギ" to "🍜",
        "交通" to "🚗", "浜ら€?" to "🚗",
        "购物" to "🛍️", "璐墿" to "🛍️",
        "住房" to "🏠", "浣忔埧" to "🏠",
        "通讯" to "📱", "閫氳" to "📱",
        "娱乐" to "🎮", "濞变箰" to "🎮",
        "医疗" to "💊", "鍖荤枟" to "💊",
        "教育" to "📚", "鏁欒偛" to "📚",
        "服饰" to "👗", "鏈嶉グ" to "👗",
        "工资" to "💰", "宸ヨ祫" to "💰",
        "奖金" to "🎖️", "濂栭噾" to "🎖️",
        "理财" to "📈", "鐞嗚储" to "📈",
        "兼职" to "👔", "鍏艰亴" to "👔",
        "鲜花" to "🌸", "椴滆姳" to "🌸",
        "礼物" to "🎁", "绀肩墿" to "🎁"
    )

    fun updateCustomIcons(icons: Map<String, String>) {
        categoryIcons.putAll(icons)
    }

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
        holder.tvIcon.text = categoryIcons[item.category] ?: "🧾"
        holder.tvCategory.text = item.category
        holder.tvNote.text = item.note.ifEmpty { "暂无备注" }

        val prefix = if (item.isIncome) "+" else "-"
        holder.tvAmount.text = "$prefix¥${String.format("%.2f", item.amount)}"
        holder.tvAmount.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (item.isIncome) R.color.income_green else R.color.expense_pink
            )
        )

        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(item)
            true
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tv_icon)
        val tvCategory: TextView = view.findViewById(R.id.tv_category)
        val tvNote: TextView = view.findViewById(R.id.tv_note)
        val tvAmount: TextView = view.findViewById(R.id.tv_amount)
    }
}
