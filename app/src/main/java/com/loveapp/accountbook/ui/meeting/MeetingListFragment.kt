package com.loveapp.accountbook.ui.meeting

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.MeetingEntry
import com.loveapp.accountbook.ui.adapter.MeetingAdapter
import com.loveapp.accountbook.util.EasterEggManager
import kotlin.math.abs

class MeetingListFragment : Fragment() {

    private val viewModel: MeetingViewModel by activityViewModels()
    private lateinit var adapter: MeetingAdapter
    private lateinit var rvMeetings: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_meeting_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MeetingAdapter(
            onItemClick = { meeting, position ->
                if (adapter.isSwipeOpenAt(position)) {
                    closeSwipeAt(rvMeetings, position)
                } else {
                    showMeetingDetailDialog(meeting)
                }
            },
            onDayClick = { meeting ->
                if (meeting.date.endsWith("-14")) {
                    EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg214)
                }
            },
            onEditClick = { meeting ->
                val openPosition = adapter.getSwipeOpenPosition()
                closeSwipeAt(rvMeetings, openPosition)
                navigateToEdit(meeting)
            },
            onDeleteClick = { meeting ->
                val openPosition = adapter.getSwipeOpenPosition()
                closeSwipeAt(rvMeetings, openPosition)
                showDeleteConfirmDialog(meeting)
            }
        )

        rvMeetings = view.findViewById(R.id.rv_meetings)
        rvMeetings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MeetingListFragment.adapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
        attachSwipeHandler(rvMeetings)

        viewModel.meetings.observe(viewLifecycleOwner) {
            adapter.updateData(it)
            view.findViewById<View>(R.id.empty_view)?.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.rv_meetings)?.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }

        view.findViewById<EditText?>(R.id.et_search)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchMeetings(s.toString())
            }
        })

        view.findViewById<View>(R.id.fab_add).setOnClickListener {
            findNavController().navigate(R.id.action_meeting_to_add)
        }

        viewModel.loadMeetings()
    }

    // ========== Apple 备忘录风格滑动（与日记模块一致） ==========

    private fun attachSwipeHandler(recyclerView: RecyclerView) {
        val viewConfig = ViewConfiguration.get(requireContext())
        val touchSlop = viewConfig.scaledTouchSlop
        val minFlingVelocity = viewConfig.scaledMinimumFlingVelocity.toFloat()

        var downX = 0f
        var downY = 0f
        var dragging = false
        var dragPosition = RecyclerView.NO_POSITION
        var dragHolder: MeetingAdapter.ViewHolder? = null
        var dragStartTx = 0f
        var velocityTracker: VelocityTracker? = null

        fun maxSwipe() = adapter.getSwipeActionTotalWidthPx().toFloat()

        fun resetState() {
            dragging = false
            dragPosition = RecyclerView.NO_POSITION
            dragHolder = null
            velocityTracker?.recycle()
            velocityTracker = null
        }

        // 滚动时自动关闭
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    val p = adapter.getSwipeOpenPosition()
                    if (p != RecyclerView.NO_POSITION) closeSwipeAt(rv, p)
                }
            }
        })

        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {

            private var intercepted = false

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.x; downY = e.y
                        dragging = false; intercepted = false
                        dragPosition = RecyclerView.NO_POSITION; dragHolder = null
                        velocityTracker?.recycle()
                        velocityTracker = VelocityTracker.obtain()
                        velocityTracker?.addMovement(e)

                        val openPos = adapter.getSwipeOpenPosition()
                        if (openPos != RecyclerView.NO_POSITION) {
                            val child = rv.findChildViewUnder(e.x, e.y)
                            val touchedPos = child?.let { rv.getChildAdapterPosition(it) }
                                ?: RecyclerView.NO_POSITION
                            if (touchedPos != openPos) {
                                closeSwipeAt(rv, openPos)
                                intercepted = true
                                return true
                            }
                            dragPosition = openPos
                            dragHolder = rv.findViewHolderForAdapterPosition(openPos)
                                    as? MeetingAdapter.ViewHolder
                            dragHolder?.cardForeground?.animate()?.cancel()
                            dragStartTx = dragHolder?.cardForeground?.translationX ?: 0f
                            intercepted = true
                            return true
                        }

                        val child = rv.findChildViewUnder(e.x, e.y)
                        if (child != null) {
                            dragPosition = rv.getChildAdapterPosition(child)
                            dragHolder = rv.findViewHolderForAdapterPosition(dragPosition)
                                    as? MeetingAdapter.ViewHolder
                        }
                        dragStartTx = 0f
                        return false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (intercepted) return true
                        if (dragHolder == null) return false
                        velocityTracker?.addMovement(e)
                        val dx = e.x - downX
                        val dy = e.y - downY
                        if (!dragging && abs(dx) > touchSlop
                            && abs(dx) > abs(dy) * 1.5f && dx < 0
                        ) {
                            dragging = true
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                            intercepted = true
                            return true
                        }
                        return false
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (!intercepted) resetState()
                        return false
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                velocityTracker?.addMovement(e)
                when (e.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        val holder = dragHolder ?: return
                        val dx = e.x - downX
                        val dy = e.y - downY
                        if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                            if (abs(dx) > abs(dy)) {
                                dragging = true
                                holder.cardForeground.animate().cancel()
                                rv.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                        if (dragging) {
                            val newTx = (dragStartTx + dx).coerceIn(-maxSwipe(), 0f)
                            holder.cardForeground.translationX = newTx
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        val holder = dragHolder
                        val pos = dragPosition
                        if (holder != null && pos != RecyclerView.NO_POSITION
                            && pos in 0 until adapter.itemCount
                        ) {
                            if (!dragging) {
                                handleTap(rv, holder, pos, maxSwipe(), downX)
                            } else {
                                handleSwipeEnd(rv, holder, pos, maxSwipe(), minFlingVelocity, velocityTracker)
                            }
                        }
                        intercepted = false
                        resetState()
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        val holder = dragHolder
                        val pos = dragPosition
                        if (holder != null && pos != RecyclerView.NO_POSITION) {
                            if (adapter.isSwipeOpenAt(pos)) {
                                animateTo(holder, -maxSwipe())
                            } else {
                                animateTo(holder, 0f)
                            }
                        }
                        intercepted = false
                        resetState()
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun handleTap(
        rv: RecyclerView, holder: MeetingAdapter.ViewHolder,
        position: Int, maxSwipe: Float, downX: Float
    ) {
        if (!adapter.isSwipeOpenAt(position)) return
        val touchLocalX = downX - holder.itemView.left
        val cardEndX = holder.itemView.width + holder.cardForeground.translationX

        if (touchLocalX > cardEndX) {
            val offset = touchLocalX - cardEndX
            val target = adapter.getItem(position)
            closeSwipeAt(rv, position)
            if (offset < maxSwipe / 2f) {
                navigateToEdit(target)
            } else {
                showDeleteConfirmDialog(target)
            }
        } else {
            closeSwipeAt(rv, position)
        }
    }

    private fun handleSwipeEnd(
        rv: RecyclerView, holder: MeetingAdapter.ViewHolder,
        position: Int, maxSwipe: Float, minFlingVelocity: Float,
        velocityTracker: VelocityTracker?
    ) {
        velocityTracker?.computeCurrentVelocity(1000)
        val vx = velocityTracker?.xVelocity ?: 0f
        val tx = holder.cardForeground.translationX
        val isOpen = adapter.isSwipeOpenAt(position)

        if (isOpen) {
            if (tx > -maxSwipe * 0.5f || vx > minFlingVelocity) {
                closeSwipeAt(rv, position)
            } else {
                animateTo(holder, -maxSwipe)
            }
        } else {
            if (tx < -maxSwipe * 0.4f || vx < -minFlingVelocity) {
                openSwipeAt(rv, position)
            } else {
                animateTo(holder, 0f)
            }
        }
    }

    private fun animateTo(holder: MeetingAdapter.ViewHolder, targetTx: Float) {
        holder.cardForeground.animate()
            .translationX(targetTx)
            .setDuration(250L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun openSwipeAt(recyclerView: RecyclerView, position: Int) {
        val maxSwipe = adapter.getSwipeActionTotalWidthPx().toFloat()
        val previous = adapter.getSwipeOpenPosition()
        if (previous != RecyclerView.NO_POSITION && previous != position) {
            closeSwipeAt(recyclerView, previous)
        }
        adapter.setSwipeOpenPosition(position, notify = false)
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? MeetingAdapter.ViewHolder
        if (holder != null) animateTo(holder, -maxSwipe)
    }

    private fun closeSwipeAt(recyclerView: RecyclerView, position: Int) {
        if (position == RecyclerView.NO_POSITION) return
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? MeetingAdapter.ViewHolder
        if (holder != null) {
            animateTo(holder, 0f)
            adapter.clearSwipeOpenPosition(position, notify = false)
        } else {
            adapter.clearSwipeOpenPosition(position)
        }
    }

    private fun showMeetingDetailDialog(entry: MeetingEntry) {
        val highlightColor = ContextCompat.getColor(requireContext(), R.color.pink_primary)
        val details = SpannableStringBuilder().apply {
            appendDetailLine("日期：", entry.date, highlightColor, emphasized = true)
            appendDetailLine("时间：", "${entry.startTime} - ${entry.endTime}", highlightColor, emphasized = true)
            appendDetailLine("地点：", entry.location.ifBlank { "未填写" }, highlightColor, emphasized = true)
            appendDetailLine("参会人：", entry.attendees.ifBlank { "未填写" }, highlightColor, emphasized = true)
            appendDetailLine("标签：", entry.tags.ifBlank { "无" }, highlightColor, emphasized = true)
            appendLabeledBlock("会议内容：", entry.content.ifBlank { "未填写" }, highlightColor)
            append("\n")
            appendLabeledBlock("待办事项：", entry.todoItems.ifBlank { "未填写" }, highlightColor)
        }
        AlertDialog.Builder(requireContext())
            .setCustomTitle(createHighlightedDialogTitle(entry.topic.ifBlank { "会议详情" }))
            .setMessage(details)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun SpannableStringBuilder.appendDetailLine(
        label: String,
        value: String,
        highlightColor: Int,
        emphasized: Boolean = false
    ) {
        val start = length
        append(label)
        if (emphasized) {
            setSpan(ForegroundColorSpan(highlightColor), start, start + label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), start, start + label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        append(value)
        append("\n")
    }

    private fun SpannableStringBuilder.appendLabeledBlock(
        label: String,
        value: String,
        highlightColor: Int
    ) {
        val start = length
        append(label)
        setSpan(ForegroundColorSpan(highlightColor), start, start + label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(StyleSpan(Typeface.BOLD), start, start + label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        append("\n")
        append(value)
    }

    private fun createHighlightedDialogTitle(title: String): TextView {
        val horizontalPadding = (16 * resources.displayMetrics.density).toInt()
        val verticalPadding = (10 * resources.displayMetrics.density).toInt()
        return TextView(requireContext()).apply {
            text = title
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            background = ContextCompat.getDrawable(context, R.drawable.bg_meeting_detail_title)
        }
    }

    private fun navigateToEdit(entry: MeetingEntry) {
        val bundle = Bundle().apply {
            putInt("entryRowIndex", entry.rowIndex)
            putString("entryTopic", entry.topic)
            putString("entryDate", entry.date)
            putString("entryStartTime", entry.startTime)
            putString("entryEndTime", entry.endTime)
            putString("entryLocation", entry.location)
            putString("entryAttendees", entry.attendees)
            putString("entryContent", entry.content)
            putString("entryTodoItems", entry.todoItems)
            putString("entryTags", entry.tags)
        }
        findNavController().navigate(R.id.action_meeting_to_add, bundle)
    }

    private fun showDeleteConfirmDialog(entry: MeetingEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("删除会议「${entry.topic}」？")
            .setPositiveButton("删除") { _, _ -> viewModel.deleteMeeting(entry) }
            .setNegativeButton("取消", null)
            .show()
    }
}
