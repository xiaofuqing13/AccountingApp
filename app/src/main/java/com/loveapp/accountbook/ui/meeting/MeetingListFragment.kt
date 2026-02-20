package com.loveapp.accountbook.ui.meeting

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.view.MotionEvent
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
import androidx.recyclerview.widget.ItemTouchHelper
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
    private var lastSwipeDx = 0f

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
                showEditTopicDialog(meeting)
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
        attachSwipeActions(rvMeetings)
        attachSwipeCloseFallback(rvMeetings)

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

    private fun attachSwipeActions(recyclerView: RecyclerView) {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                val itemWidth = viewHolder.itemView.width.takeIf { it > 0 } ?: return 0.2f
                val actionWidth = adapter.getSwipeActionTotalWidthPx().toFloat()
                return (actionWidth / itemWidth.toFloat()).coerceIn(0.12f, 0.4f)
            }

            override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 1.5f

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (viewHolder is MeetingAdapter.ViewHolder) {
                    getDefaultUIUtil().onSelected(viewHolder.cardForeground)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                if (viewHolder is MeetingAdapter.ViewHolder) {
                    getDefaultUIUtil().clearView(viewHolder.cardForeground)
                    val position = viewHolder.bindingAdapterPosition
                    val closeThreshold = recyclerView.resources.displayMetrics.density * 12f
                    if (
                        position != RecyclerView.NO_POSITION &&
                        adapter.isSwipeOpenAt(position) &&
                        lastSwipeDx > closeThreshold
                    ) {
                        closeSwipeAt(recyclerView, position)
                    }
                } else {
                    super.clearView(recyclerView, viewHolder)
                }
                lastSwipeDx = 0f
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (viewHolder !is MeetingAdapter.ViewHolder) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    return
                }
                lastSwipeDx = dX
                val maxSwipe = adapter.getSwipeActionTotalWidthPx().toFloat()
                val position = viewHolder.bindingAdapterPosition
                val isOpen = position != RecyclerView.NO_POSITION && adapter.isSwipeOpenAt(position)
                val clampedDx = when {
                    dX < 0f -> dX.coerceAtLeast(-maxSwipe)
                    isOpen -> (-maxSwipe + dX).coerceIn(-maxSwipe, 0f)
                    else -> 0f
                }
                getDefaultUIUtil().onDraw(
                    c,
                    recyclerView,
                    viewHolder.cardForeground,
                    clampedDx,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                if (direction == ItemTouchHelper.RIGHT) {
                    closeSwipeAt(recyclerView, position)
                } else {
                    openSwipeAt(recyclerView, position)
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    private fun attachSwipeCloseFallback(recyclerView: RecyclerView) {
        val closeTrigger = recyclerView.resources.displayMetrics.density * 4f
        var downX = 0f
        var downY = 0f
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val openPosition = adapter.getSwipeOpenPosition()
                if (openPosition == RecyclerView.NO_POSITION) return false
                val actionWidth = adapter.getSwipeActionTotalWidthPx().toFloat()
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.x
                        downY = e.y
                        val touchedChild = rv.findChildViewUnder(e.x, e.y)
                        val touchedPosition = touchedChild?.let { rv.getChildAdapterPosition(it) }
                        if (touchedPosition != openPosition) {
                            closeSwipeAt(rv, openPosition)
                        } else {
                            val isCardBodyArea = e.x < rv.width - actionWidth
                            if (isCardBodyArea) {
                                closeSwipeAt(rv, openPosition)
                                return true
                            }
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val dx = e.x - downX
                        val dy = e.y - downY
                        if (dx > closeTrigger && abs(dx) >= abs(dy) * 0.6f) {
                            closeSwipeAt(rv, openPosition)
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    private fun openSwipeAt(recyclerView: RecyclerView, position: Int) {
        val maxSwipe = adapter.getSwipeActionTotalWidthPx().toFloat()
        val previous = adapter.getSwipeOpenPosition()
        if (previous != RecyclerView.NO_POSITION && previous != position) {
            val previousHolder =
                recyclerView.findViewHolderForAdapterPosition(previous) as? MeetingAdapter.ViewHolder
            if (previousHolder != null) {
                previousHolder.cardForeground.animate()
                    .translationX(0f)
                    .setDuration(150L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
                adapter.clearSwipeOpenPosition(previous, notify = false)
            } else {
                adapter.clearSwipeOpenPosition(previous)
            }
        }
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? MeetingAdapter.ViewHolder
        if (holder != null) {
            holder.cardForeground.animate()
                .translationX(-maxSwipe)
                .setDuration(150L)
                .setInterpolator(DecelerateInterpolator())
                .start()
            adapter.setSwipeOpenPosition(position, notify = false)
        } else {
            adapter.setSwipeOpenPosition(position)
        }
    }

    private fun closeSwipeAt(recyclerView: RecyclerView, position: Int) {
        if (position == RecyclerView.NO_POSITION) return
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? MeetingAdapter.ViewHolder
        if (holder != null) {
            holder.cardForeground.animate()
                .translationX(0f)
                .setDuration(120L)
                .setInterpolator(DecelerateInterpolator())
                .start()
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

    private fun showEditTopicDialog(entry: MeetingEntry) {
        val etTopic = EditText(requireContext()).apply {
            setText(entry.topic)
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("编辑会议主题")
            .setView(etTopic)
            .setPositiveButton("保存") { _, _ ->
                viewModel.updateMeeting(entry.copy(topic = etTopic.text.toString()))
            }
            .setNegativeButton("取消", null)
            .show()
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
