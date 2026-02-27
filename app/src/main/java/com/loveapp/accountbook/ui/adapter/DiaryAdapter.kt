package com.loveapp.accountbook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.DiaryContentRenderer
import com.loveapp.accountbook.util.EasterEggManager

class DiaryAdapter(
    private var items: List<DiaryEntry> = emptyList(),
    private val onItemClick: ((DiaryEntry, Int) -> Unit)? = null,
    private val onMoodClick: ((DiaryEntry) -> Unit)? = null,
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

    companion object {
        private val WEATHER_ICON_MAP = mapOf(
            "晴" to R.drawable.ic_weather_sunny,
            "多云" to R.drawable.ic_weather_cloudy,
            "雨" to R.drawable.ic_weather_rainy,
            "雪" to R.drawable.ic_weather_snowy,
            "雾" to R.drawable.ic_weather_foggy,
            "彩虹" to R.drawable.ic_weather_rainbow
        )
        private val MOOD_ICON_MAP = mapOf(
            "开心" to R.drawable.ic_mood_happy,
            "愉快" to R.drawable.ic_mood_pleasant,
            "平静" to R.drawable.ic_mood_calm,
            "难过" to R.drawable.ic_mood_sad,
            "生气" to R.drawable.ic_mood_angry,
            "疲惫" to R.drawable.ic_mood_tired,
            "思考" to R.drawable.ic_mood_thinking,
            "充实" to R.drawable.ic_mood_fulfilled
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        holder.tvDate.text = DateUtils.formatDateDisplay(item.date)
        holder.tvWeather.text = item.weather
        holder.tvTitle.text = item.title
        holder.tvPreview.text = DiaryContentRenderer.getPlainPreview(item.content)

        // 天气图标
        val weatherIconRes = WEATHER_ICON_MAP[item.weather]
        if (weatherIconRes != null) {
            val drawable = ContextCompat.getDrawable(ctx, weatherIconRes)?.apply {
                val size = (12 * ctx.resources.displayMetrics.density).toInt()
                setBounds(0, 0, size, size)
            }
            holder.tvWeather.setCompoundDrawables(drawable, null, null, null)
            holder.tvWeather.compoundDrawablePadding = (2 * ctx.resources.displayMetrics.density).toInt()
        } else {
            holder.tvWeather.setCompoundDrawables(null, null, null, null)
        }

        // 心情图标
        val moodIconRes = MOOD_ICON_MAP[item.mood] ?: R.drawable.ic_mood
        holder.ivMood.setImageResource(moodIconRes)

        if (item.location.isNotEmpty()) {
            holder.tvLocationTag.text = item.location
            holder.tvLocationTag.visibility = View.VISIBLE
        } else {
            holder.tvLocationTag.visibility = View.GONE
        }

        // 标签
        if (item.tags.isNotEmpty()) {
            holder.tvTags.text = item.tags.split(",").joinToString(" ") { "#$it" }
            holder.tvTags.visibility = View.VISIBLE
        } else {
            holder.tvTags.visibility = View.GONE
        }

        // 滑动状态视觉同步
        val maxPx = getSwipeActionTotalWidthPx().toFloat()
        val isSwipe = (position == swipeOpenPosition)
        holder.cardForeground.translationX = if (isSwipe) -maxPx else 0f
        
        // 动态控制底部动作栏的透明度，避免透底穿帮
        holder.diaryActions.alpha = if (isSwipe) 1f else 0f
        holder.diaryActions.visibility = if (isSwipe) View.VISIBLE else View.INVISIBLE

        holder.cardForeground.scaleX = 1f
        holder.cardForeground.scaleY = 1f

        // 点击监听器 - 与会议模块完全一致
        holder.cardForeground.setOnClickListener { onItemClick?.invoke(item, position) }
        holder.ivMood.setOnClickListener { onMoodClick?.invoke(item) }
        holder.btnSwipeEdit.setOnClickListener { onEditClick?.invoke(item) }
        holder.btnSwipeDelete.setOnClickListener { onDeleteClick?.invoke(item) }
    }

    override fun getItemCount() = items.size

    fun getItem(position: Int): DiaryEntry = items[position]

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardForeground: MaterialCardView = view.findViewById(R.id.card_foreground)
        val diaryActions: View = view.findViewById(R.id.diary_actions)
        val btnSwipeEdit: View = view.findViewById(R.id.btn_swipe_edit)
        val btnSwipeDelete: View = view.findViewById(R.id.btn_swipe_delete)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvWeather: TextView = view.findViewById(R.id.tv_weather)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvPreview: TextView = view.findViewById(R.id.tv_preview)
        val ivMood: ImageView = view.findViewById(R.id.iv_mood)
        val tvLocationTag: TextView = view.findViewById(R.id.tv_location_tag)
        val tvTags: TextView = view.findViewById(R.id.tv_tags)
    }
}
