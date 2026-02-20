package com.loveapp.accountbook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.DiaryContentRenderer
import com.loveapp.accountbook.util.EasterEggManager

class DiaryAdapter(
    private var items: List<DiaryEntry> = emptyList(),
    private val onMoodClick: ((Int) -> Unit)? = null,
    private val onItemClick: ((DiaryEntry) -> Unit)? = null,
    private val onLongClick: ((DiaryEntry) -> Unit)? = null
) : RecyclerView.Adapter<DiaryAdapter.ViewHolder>() {

    fun updateData(newItems: List<DiaryEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        runCatching {
            val item = items[position]
            holder.tvDate.text = DateUtils.formatDateDisplay(item.date)
            holder.tvWeather.text = item.weather
            holder.tvTitle.text = item.title
            holder.tvPreview.text = DiaryContentRenderer.getPlainPreview(item.content)
            holder.ivMood.setImageResource(EasterEggManager.iconResForMood(item.mood))

            if (item.location.isNotEmpty()) {
                holder.tvLocationTag.text = item.location
                holder.tvLocationTag.visibility = View.VISIBLE
            } else {
                holder.tvLocationTag.visibility = View.GONE
            }

            holder.ivMood.setOnClickListener { onMoodClick?.invoke(position) }
            holder.cardForeground.setOnClickListener { onItemClick?.invoke(item) }
            holder.cardForeground.setOnLongClickListener { onLongClick?.invoke(item); true }
        }.onFailure {
            holder.tvTitle.text = "日记加载异常"
            holder.tvPreview.text = "该条数据异常，请尝试编辑或删除"
            holder.tvWeather.text = ""
            holder.tvDate.text = ""
            holder.tvLocationTag.visibility = View.GONE
            holder.ivMood.setImageResource(R.drawable.ic_mood)
            holder.ivMood.setOnClickListener(null)
            holder.cardForeground.setOnClickListener(null)
            holder.cardForeground.setOnLongClickListener(null)
        }
    }

    override fun getItemCount() = items.size

    fun getItem(position: Int): DiaryEntry = items[position]

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardForeground: CardView = view.findViewById(R.id.card_foreground)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvWeather: TextView = view.findViewById(R.id.tv_weather)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvPreview: TextView = view.findViewById(R.id.tv_preview)
        val ivMood: ImageView = view.findViewById(R.id.iv_mood)
        val tvLocationTag: TextView = view.findViewById(R.id.tv_location_tag)
    }
}
