package com.loveapp.accountbook.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loveapp.accountbook.R
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

        adapter = MeetingAdapter { meeting ->
            // 彩蛋: 14号
            if (meeting.date.endsWith("-14")) {
                EasterEggManager.showLovePopup(requireContext(), EasterEggManager.egg214)
            }
        }

        view.findViewById<RecyclerView>(R.id.rv_meetings).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MeetingListFragment.adapter
        }

        viewModel.meetings.observe(viewLifecycleOwner) { adapter.updateData(it) }

        view.findViewById<View>(R.id.fab_add).setOnClickListener {
            findNavController().navigate(R.id.action_meeting_to_add)
        }

        viewModel.loadMeetings()
    }
}
