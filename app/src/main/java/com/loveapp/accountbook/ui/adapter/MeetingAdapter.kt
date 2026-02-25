package com.loveapp.accountbook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.MeetingEntry

class MeetingAdapter(
    private var items: List<MeetingEntry> = emptyList(),
    private val onItemClick: ((MeetingEntry, Int) -> Unit)? = null,
    private val onDayClick: ((MeetingEntry) -> Unit)? = null,
    private val onEditClick: ((MeetingEntry) -> Unit)? = null,
    private val onDeleteClick: ((MeetingEntry) -> Unit)? = null
) : RecyclerView.Adapter<MeetingAdapter.ViewHolder>() {

    private var swipeOpenPosition: Int = RecyclerView.NO_POSITION
    private var swipeActionTotalWidthPx: Int = 0

    fun updateData(newItems: List<MeetingEntry>) {
        items = newItems
        if (swipeOpenPosition !in items.indices) {
            swipeOpenPosition = RecyclerView.NO_POSITION
        }
        notifyDataSetChanged()
    }

    fun getSwipeOpenPosition(): Int = swipeOpenPosition

    fun setSwipeOpenPosition(position: Int, notify: Boolean = true) {
        if (position == swipeOpenPosition) {
            if (notify && position != RecyclerView.NO_POSITION) notifyItemChanged(position)
            return
        }
        val previous = swipeOpenPosition
        swipeOpenPosition = position
        if (!notify) return
        if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
        if (position != RecyclerView.NO_POSITION) notifyItemChanged(position)
    }

    fun clearSwipeOpenPosition(position: Int = swipeOpenPosition, notify: Boolean = true) {
        if (position == RecyclerView.NO_POSITION) return
        if (position == swipeOpenPosition) {
            swipeOpenPosition = RecyclerView.NO_POSITION
        }
        if (notify) notifyItemChanged(position)
    }

    fun isSwipeOpenAt(position: Int): Boolean = swipeOpenPosition == position

    fun getSwipeActionTotalWidthPx(): Int = swipeActionTotalWidthPx

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meeting, parent, false)
        if (swipeActionTotalWidthPx == 0) {
            swipeActionTotalWidthPx =
                view.resources.getDimensionPixelSize(R.dimen.meeting_swipe_action_width) * 2
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val dateParts = item.date.split("-")
        holder.tvDay.text = if (dateParts.size >= 3) dateParts[2].trimStart('0') else ""
        holder.tvMonth.text = if (dateParts.size >= 2) "${dateParts[1].trimStart('0')}月" else ""
        holder.tvTitle.text = item.topic
        holder.tvTimeLocation.text = "${item.startTime}-${item.endTime}  ·  ${item.location}"
        holder.tvAttendees.text = "参会：${item.attendees}"

        holder.chipTags.removeAllViews()
        item.tags.split(",")
            .filter { it.isNotBlank() }
            .forEach { tag ->
                val chip = Chip(holder.itemView.context).apply {
                    text = tag.trim()
                    textSize = 10f
                    isClickable = false
                    isCheckable = false
                    chipMinHeight = 0f
                }
                holder.chipTags.addView(chip)
            }

        holder.cardForeground.translationX = if (position == swipeOpenPosition)
            -getSwipeActionTotalWidthPx().toFloat() else 0f
        holder.cardForeground.scaleX = 1f
        holder.cardForeground.scaleY = 1f
        holder.cardForeground.setOnClickListener { onItemClick?.invoke(item, position) }
        holder.tvDay.setOnClickListener { onDayClick?.invoke(item) }
        holder.btnSwipeEdit.setOnClickListener { onEditClick?.invoke(item) }
        holder.btnSwipeDelete.setOnClickListener { onDeleteClick?.invoke(item) }
    }

    override fun getItemCount() = items.size

    fun getItem(position: Int): MeetingEntry = items[position]

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardForeground: MaterialCardView = view.findViewById(R.id.card_foreground)
        val btnSwipeEdit: View = view.findViewById(R.id.btn_swipe_edit)
        val btnSwipeDelete: View = view.findViewById(R.id.btn_swipe_delete)
        val tvDay: TextView = view.findViewById(R.id.tv_day)
        val tvMonth: TextView = view.findViewById(R.id.tv_month)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvTimeLocation: TextView = view.findViewById(R.id.tv_time_location)
        val tvAttendees: TextView = view.findViewById(R.id.tv_attendees)
        val chipTags: ChipGroup = view.findViewById(R.id.chip_tags)
    }
}
