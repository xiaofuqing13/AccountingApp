package com.loveapp.accountbook.ui.meeting

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.MeetingEntry
import com.loveapp.accountbook.ui.adapter.MeetingAdapter
import com.loveapp.accountbook.util.EasterEggManager

class MeetingListFragment : Fragment() {

    private val viewModel: MeetingViewModel by activityViewModels()
    private lateinit var adapter: MeetingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_meeting_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MeetingAdapter(
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

        val etSearch = view.findViewById<EditText?>(R.id.et_search)
        etSearch?.addTextChangedListener(object : TextWatcher {
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

    private fun showEditDeleteDialog(entry: MeetingEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle(entry.topic)
            .setItems(arrayOf("✏️ 编辑主题", "🗑️ 删除")) { _, which ->
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
