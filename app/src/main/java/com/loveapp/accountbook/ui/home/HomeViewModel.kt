package com.loveapp.accountbook.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.loveapp.accountbook.data.repository.ExcelRepository
import com.loveapp.accountbook.util.DateUtils
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ExcelRepository(application)

    private val _diaryCount = MutableLiveData(0)
    val diaryCount: LiveData<Int> = _diaryCount

    private val _accountCount = MutableLiveData(0)
    val accountCount: LiveData<Int> = _accountCount

    fun loadStats() {
        viewModelScope.launch {
            try {
                val yearMonth = DateUtils.currentYearMonth()
                _accountCount.value = repo.getAccountsByMonth(yearMonth).size
                _diaryCount.value = repo.getDiaries().count { it.date.startsWith(yearMonth) }
            } catch (_: Exception) {
                // 存储权限不足时优雅降级，显示默认值
                _accountCount.value = 0
                _diaryCount.value = 0
            }
        }
    }
}
