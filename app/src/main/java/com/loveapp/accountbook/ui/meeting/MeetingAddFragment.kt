package com.loveapp.accountbook.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.MeetingEntry
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.DraftManager
import com.loveapp.accountbook.util.EasterEggManager

class MeetingAddFragment : Fragment() {

    private val viewModel: MeetingViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_meeting_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTopic = view.findViewById<EditText>(R.id.et_topic)
        val tvDate = view.findViewById<TextView>(R.id.tv_date)
        val tvStartTime = view.findViewById<TextView>(R.id.tv_start_time)
        val tvEndTime = view.findViewById<TextView>(R.id.tv_end_time)
        val etLocation = view.findViewById<EditText>(R.id.et_location)
        val etAttendees = view.findViewById<EditText>(R.id.et_attendees)
        val etContent = view.findViewById<EditText>(R.id.et_content)
        val etTodo = view.findViewById<EditText>(R.id.et_todo)

        tvDate.text = DateUtils.today()
        tvStartTime.text = DateUtils.currentTime()
        tvEndTime.text = DateUtils.currentTime()

        // 自动保存：恢复草稿
        val hasDraft = DraftManager.restoreDraft(requireContext(), etTopic, DraftManager.KEY_MEETING_TOPIC)
        DraftManager.restoreDraft(requireContext(), etLocation, DraftManager.KEY_MEETING_LOCATION)
        DraftManager.restoreDraft(requireContext(), etAttendees, DraftManager.KEY_MEETING_ATTENDEES)
        DraftManager.restoreDraft(requireContext(), etContent, DraftManager.KEY_MEETING_CONTENT)
        DraftManager.restoreDraft(requireContext(), etTodo, DraftManager.KEY_MEETING_TODO)
        if (hasDraft) Toast.makeText(requireContext(), "已恢复上次编辑的草稿", Toast.LENGTH_SHORT).show()

        // 自动保存：绑定输入监听
        DraftManager.bindAutoSave(requireContext(), etTopic, DraftManager.KEY_MEETING_TOPIC)
        DraftManager.bindAutoSave(requireContext(), etLocation, DraftManager.KEY_MEETING_LOCATION)
        DraftManager.bindAutoSave(requireContext(), etAttendees, DraftManager.KEY_MEETING_ATTENDEES)
        DraftManager.bindAutoSave(requireContext(), etContent, DraftManager.KEY_MEETING_CONTENT)
        DraftManager.bindAutoSave(requireContext(), etTodo, DraftManager.KEY_MEETING_TODO)

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val topic = etTopic.text.toString().trim()
            if (topic.isEmpty()) {
                Toast.makeText(requireContext(), "请输入会议主题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addMeeting(MeetingEntry(
                date = tvDate.text.toString(),
                topic = topic,
                startTime = tvStartTime.text.toString(),
                endTime = tvEndTime.text.toString(),
                location = etLocation.text.toString(),
                attendees = etAttendees.text.toString(),
                content = etContent.text.toString(),
                todoItems = etTodo.text.toString()
            ))
            DraftManager.clearDrafts(requireContext(), "draft_meeting_")
            Toast.makeText(requireContext(), "会议纪要保存成功", Toast.LENGTH_SHORT).show()
            // 随机概率弹出保存惊喜
            if ((0..2).random() == 0) {
                EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggMeetingSave)
            }
            findNavController().popBackStack()
        }
    }
}
