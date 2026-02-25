package com.loveapp.accountbook.ui.meeting

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.MeetingEntry
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.DraftManager
import com.loveapp.accountbook.util.EasterEggManager
import com.loveapp.accountbook.util.LocationHelper

class MeetingAddFragment : Fragment() {

    private val viewModel: MeetingViewModel by activityViewModels()
    private lateinit var locationHelper: LocationHelper

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) locationHelper.fetchLocation { address -> autoFillLocation(address) }
    }

    private var etLocationRef: EditText? = null

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
        etLocationRef = etLocation
        val etAttendees = view.findViewById<EditText>(R.id.et_attendees)
        val etContent = view.findViewById<EditText>(R.id.et_content)
        val etTodo = view.findViewById<EditText>(R.id.et_todo)

        // 判断是编辑模式还是新增模式
        val editRowIndex = arguments?.getInt("entryRowIndex", -1) ?: -1
        val isEditMode = editRowIndex >= 0

        if (isEditMode) {
            // 编辑模式：填充已有数据
            etTopic.setText(arguments?.getString("entryTopic", ""))
            tvDate.text = arguments?.getString("entryDate", "").takeIf { !it.isNullOrBlank() } ?: DateUtils.today()
            tvStartTime.text = arguments?.getString("entryStartTime", "").takeIf { !it.isNullOrBlank() } ?: DateUtils.currentTime()
            tvEndTime.text = arguments?.getString("entryEndTime", "").takeIf { !it.isNullOrBlank() } ?: DateUtils.currentTime()
            etLocation.setText(arguments?.getString("entryLocation", ""))
            etAttendees.setText(arguments?.getString("entryAttendees", ""))
            etContent.setText(arguments?.getString("entryContent", ""))
            etTodo.setText(arguments?.getString("entryTodoItems", ""))
        } else {
            // 新增模式：默认日期时间 + 恢复草稿
            tvDate.text = DateUtils.today()
            tvStartTime.text = DateUtils.currentTime()
            tvEndTime.text = DateUtils.currentTime()

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

            // 自动获取定位
            locationHelper = LocationHelper(requireContext())
            if (locationHelper.hasPermission()) {
                locationHelper.fetchLocation { address -> autoFillLocation(address) }
            } else {
                locationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val topic = etTopic.text.toString().trim()
            if (topic.isEmpty()) {
                Toast.makeText(requireContext(), "请输入会议主题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val entry = MeetingEntry(
                date = tvDate.text.toString(),
                topic = topic,
                startTime = tvStartTime.text.toString(),
                endTime = tvEndTime.text.toString(),
                location = etLocation.text.toString(),
                attendees = etAttendees.text.toString(),
                content = etContent.text.toString(),
                todoItems = etTodo.text.toString(),
                rowIndex = editRowIndex
            )
            if (isEditMode) {
                viewModel.updateMeeting(entry)
                Toast.makeText(requireContext(), "会议纪要已更新", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addMeeting(entry)
                DraftManager.clearDrafts(requireContext(), "draft_meeting_")
                Toast.makeText(requireContext(), "会议纪要保存成功", Toast.LENGTH_SHORT).show()
                if ((0..2).random() == 0) {
                    EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggMeetingSave)
                }
            }
            findNavController().popBackStack()
        }
    }

    private fun autoFillLocation(address: String) {
        val et = etLocationRef ?: return
        if (et.text.isNullOrBlank()) {
            et.setText(address)
        }
    }
}
