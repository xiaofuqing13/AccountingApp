package com.loveapp.accountbook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.util.DateUtils

class DiaryAdapter(
    private var items: List<DiaryEntry> = emptyList(),
    private val onMoodClick: ((Int) -> Unit)? = null
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
        val item = items[position]
        holder.tvDate.text = DateUtils.formatDateDisplay(item.date)
        holder.tvWeather.text = item.weather
        holder.tvTitle.text = item.title
        holder.tvPreview.text = item.content
        holder.tvMood.text = item.mood

        holder.tvMood.setOnClickListener { onMoodClick?.invoke(position) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvWeather: TextView = view.findViewById(R.id.tv_weather)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvPreview: TextView = view.findViewById(R.id.tv_preview)
        val tvMood: TextView = view.findViewById(R.id.tv_mood)
    }
}
