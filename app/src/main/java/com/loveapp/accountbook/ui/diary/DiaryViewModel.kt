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
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadDiaries() {
        viewModelScope.launch {
            runCatching { repo.getDiaries() }
                .onSuccess {
                    _diaries.value = it
                    _errorMessage.value = null
                }
                .onFailure {
                    _diaries.value = emptyList()
                    _errorMessage.value = "加载日记失败，请重试"
                }
        }
    }

    fun searchDiaries(keyword: String) {
        viewModelScope.launch {
            runCatching {
                val all = repo.getDiaries()
                if (keyword.isBlank()) all
                else all.filter { it.title.contains(keyword) || it.content.contains(keyword) }
            }.onSuccess {
                _diaries.value = it
                _errorMessage.value = null
            }.onFailure {
                _errorMessage.value = "搜索日记失败，请重试"
            }
        }
    }

    fun addDiary(entry: DiaryEntry) {
        viewModelScope.launch {
            runCatching { repo.addDiary(entry) }
                .onSuccess {
                    _errorMessage.value = null
                    loadDiaries()
                }
                .onFailure {
                    _errorMessage.value = "保存日记失败，请重试"
                }
        }
    }

    fun deleteDiary(entry: DiaryEntry) {
        viewModelScope.launch {
            runCatching { repo.deleteDiary(entry) }
                .onSuccess {
                    _errorMessage.value = null
                    loadDiaries()
                }
                .onFailure {
                    _errorMessage.value = "删除日记失败，请重试"
                }
        }
    }

    fun updateDiary(entry: DiaryEntry) {
        viewModelScope.launch {
            runCatching { repo.updateDiary(entry) }
                .onSuccess {
                    _errorMessage.value = null
                    loadDiaries()
                }
                .onFailure {
                    _errorMessage.value = "更新日记失败，请重试"
                }
        }
    }
}
