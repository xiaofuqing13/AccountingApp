package com.loveapp.accountbook.ui.meeting

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.MeetingEntry
import com.loveapp.accountbook.ui.adapter.MeetingAdapter
import com.loveapp.accountbook.util.EasterEggManager

class MeetingListFragment : Fragment() {

    private val viewModel: MeetingViewModel by activityViewModels()
    private lateinit var adapter: MeetingAdapter

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
            onItemClick = { meeting -> showMeetingDetailDialog(meeting) },
            onDayClick = { meeting ->
                if (meeting.date.endsWith("-14")) {
                    EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg214)
                }
            },
            onLongClick = { meeting -> showEditDeleteDialog(meeting) }
        )

        view.findViewById<RecyclerView>(R.id.rv_meetings).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MeetingListFragment.adapter
        }

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

    private fun showEditDeleteDialog(entry: MeetingEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle(entry.topic)
            .setItems(arrayOf("编辑主题", "删除")) { _, which ->
                when (which) {
                    0 -> showEditTopicDialog(entry)
                    1 -> showDeleteConfirmDialog(entry)
                }
            }
            .show()
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
