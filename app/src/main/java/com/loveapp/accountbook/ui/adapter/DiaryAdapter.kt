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
    private val onItemClick: ((DiaryEntry, Int) -> Unit)? = null,
    private val onEditClick: ((DiaryEntry) -> Unit)? = null,
    private val onDeleteClick: ((DiaryEntry) -> Unit)? = null
) : RecyclerView.Adapter<DiaryAdapter.ViewHolder>() {

    private var swipeOpenPosition: Int = RecyclerView.NO_POSITION
    private var swipeActionTotalWidthPx: Int = 0

    fun updateData(newItems: List<DiaryEntry>) {
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diary, parent, false)
        if (swipeActionTotalWidthPx == 0) {
            swipeActionTotalWidthPx =
                view.resources.getDimensionPixelSize(R.dimen.diary_swipe_action_width) * 2
        }
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

            holder.cardForeground.translationX =
                if (position == swipeOpenPosition) -getSwipeActionTotalWidthPx().toFloat() else 0f
            holder.ivMood.setOnClickListener { onMoodClick?.invoke(position) }
            holder.cardForeground.setOnClickListener { onItemClick?.invoke(item, position) }
            holder.btnSwipeEdit.setOnClickListener { onEditClick?.invoke(item) }
            holder.btnSwipeDelete.setOnClickListener { onDeleteClick?.invoke(item) }
        }.onFailure {
            holder.tvTitle.text = "日记加载异常"
            holder.tvPreview.text = "该条数据异常，请尝试编辑或删除"
            holder.tvWeather.text = ""
            holder.tvDate.text = ""
            holder.tvLocationTag.visibility = View.GONE
            holder.ivMood.setImageResource(R.drawable.ic_mood)
            holder.ivMood.setOnClickListener(null)
            holder.cardForeground.setOnClickListener(null)
            holder.btnSwipeEdit.setOnClickListener(null)
            holder.btnSwipeDelete.setOnClickListener(null)
        }
    }

    override fun getItemCount() = items.size

    fun getItem(position: Int): DiaryEntry = items[position]

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardForeground: CardView = view.findViewById(R.id.card_foreground)
        val btnSwipeEdit: TextView = view.findViewById(R.id.btn_swipe_edit)
        val btnSwipeDelete: TextView = view.findViewById(R.id.btn_swipe_delete)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvWeather: TextView = view.findViewById(R.id.tv_weather)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvPreview: TextView = view.findViewById(R.id.tv_preview)
        val ivMood: ImageView = view.findViewById(R.id.iv_mood)
        val tvLocationTag: TextView = view.findViewById(R.id.tv_location_tag)
    }

}
