package com.loveapp.accountbook.ui.diary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.data.repository.ExcelRepository
import kotlinx.coroutines.launch

class DiaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ExcelRepository(application)

    private val _diaries = MutableLiveData<List<DiaryEntry>>()
    val diaries: LiveData<List<DiaryEntry>> = _diaries

    fun loadDiaries() {
        viewModelScope.launch {
            _diaries.value = repo.getDiaries()
        }
    }

    fun searchDiaries(keyword: String) {
        viewModelScope.launch {
            val all = repo.getDiaries()
            _diaries.value = if (keyword.isBlank()) all
            else all.filter { it.title.contains(keyword) || it.content.contains(keyword) }
        }
    }

    fun addDiary(entry: DiaryEntry) {
        viewModelScope.launch {
            repo.addDiary(entry)
            loadDiaries()
        }
    }
}
