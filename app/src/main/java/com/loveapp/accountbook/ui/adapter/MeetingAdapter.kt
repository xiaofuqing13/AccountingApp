package com.loveapp.accountbook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.MeetingEntry

class MeetingAdapter(
    private var items: List<MeetingEntry> = emptyList(),
    private val onDayClick: ((MeetingEntry) -> Unit)? = null
) : RecyclerView.Adapter<MeetingAdapter.ViewHolder>() {

    fun updateData(newItems: List<MeetingEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meeting, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val dateParts = item.date.split("-")
        holder.tvDay.text = if (dateParts.size >= 3) dateParts[2].trimStart('0') else ""
        holder.tvMonth.text = if (dateParts.size >= 2) "${dateParts[1].trimStart('0')}月" else ""
        holder.tvTitle.text = item.topic
        holder.tvTimeLocation.text = "⏰ ${item.startTime}-${item.endTime} | ❤️ ${item.location}"
        holder.tvAttendees.text = "👥 ${item.attendees}"

        // 标签
        holder.chipTags.removeAllViews()
        item.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
            val chip = Chip(holder.itemView.context).apply {
                text = tag.trim()
                textSize = 10f
                isClickable = false
                chipMinHeight = 0f
            }
            holder.chipTags.addView(chip)
        }

        // 彩蛋: 14号点击
        holder.tvDay.setOnClickListener { onDayClick?.invoke(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tv_day)
        val tvMonth: TextView = view.findViewById(R.id.tv_month)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvTimeLocation: TextView = view.findViewById(R.id.tv_time_location)
        val tvAttendees: TextView = view.findViewById(R.id.tv_attendees)
        val chipTags: ChipGroup = view.findViewById(R.id.chip_tags)
    }
}
