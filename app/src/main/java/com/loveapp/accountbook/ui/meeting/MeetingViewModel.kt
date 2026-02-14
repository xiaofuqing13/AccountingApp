package com.loveapp.accountbook.ui.meeting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.loveapp.accountbook.data.model.MeetingEntry
import com.loveapp.accountbook.data.repository.ExcelRepository
import kotlinx.coroutines.launch

class MeetingViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ExcelRepository(application)

    private val _meetings = MutableLiveData<List<MeetingEntry>>()
    val meetings: LiveData<List<MeetingEntry>> = _meetings

    fun loadMeetings() {
        viewModelScope.launch {
            _meetings.value = repo.getMeetings()
        }
    }

    fun addMeeting(entry: MeetingEntry) {
        viewModelScope.launch {
            repo.addMeeting(entry)
            loadMeetings()
        }
    }
}
