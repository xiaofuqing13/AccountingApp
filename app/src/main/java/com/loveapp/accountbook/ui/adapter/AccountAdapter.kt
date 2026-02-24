package com.loveapp.accountbook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        "餐饮" to R.drawable.ic_cat_food, "椁愰ギ" to R.drawable.ic_cat_food,
        "交通" to R.drawable.ic_cat_transport, "浜ら€?" to R.drawable.ic_cat_transport,
        "购物" to R.drawable.ic_cat_shopping, "璐墿" to R.drawable.ic_cat_shopping,
        "住房" to R.drawable.ic_cat_house, "浣忔埧" to R.drawable.ic_cat_house,
        "通讯" to R.drawable.ic_cat_phone, "閫氳" to R.drawable.ic_cat_phone,
        "娱乐" to R.drawable.ic_cat_game, "濞变箰" to R.drawable.ic_cat_game,
        "医疗" to R.drawable.ic_cat_medical, "鍖荤枟" to R.drawable.ic_cat_medical,
        "教育" to R.drawable.ic_cat_education, "鏁欒偛" to R.drawable.ic_cat_education,
        "服饰" to R.drawable.ic_cat_clothes, "鏈嶉グ" to R.drawable.ic_cat_clothes,
        "工资" to R.drawable.ic_cat_salary, "宸ヨ祫" to R.drawable.ic_cat_salary,
        "奖金" to R.drawable.ic_cat_bonus, "濂栭噾" to R.drawable.ic_cat_bonus,
        "理财" to R.drawable.ic_cat_invest, "鐞嗚储" to R.drawable.ic_cat_invest,
        "兼职" to R.drawable.ic_cat_parttime, "鍏艰亴" to R.drawable.ic_cat_parttime,
        "鲜花" to R.drawable.ic_cat_bonus, "椴滆姳" to R.drawable.ic_cat_bonus,
        "礼物" to R.drawable.ic_cat_bonus, "绀肩墿" to R.drawable.ic_cat_bonus
    )

    fun updateCustomIcons(icons: Map<String, String>) {
        icons.forEach { (name, icon) ->
            icon.toIntOrNull()?.let { categoryIcons[name] = it }
        }
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
        holder.tvIcon.setImageResource(categoryIcons[item.category] ?: R.drawable.ic_cat_other)
        holder.tvCategory.text = item.category
        // 从日期字段提取时间 (格式: yyyy-MM-dd HH:mm)
        val time = if (item.date.length > 10) item.date.substring(11) else ""
        holder.tvTime.text = time
        holder.tvTime.visibility = if (time.isNotEmpty()) View.VISIBLE else View.GONE
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
        val tvIcon: ImageView = view.findViewById(R.id.tv_icon)
        val tvCategory: TextView = view.findViewById(R.id.tv_category)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val tvNote: TextView = view.findViewById(R.id.tv_note)
        val tvAmount: TextView = view.findViewById(R.id.tv_amount)
    }
}
