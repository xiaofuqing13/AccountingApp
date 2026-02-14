package com.loveapp.accountbook.ui.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.loveapp.accountbook.data.model.AccountEntry
import com.loveapp.accountbook.data.repository.ExcelRepository
import com.loveapp.accountbook.util.DateUtils
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ExcelRepository(application)

    private val _accounts = MutableLiveData<List<AccountEntry>>()
    val accounts: LiveData<List<AccountEntry>> = _accounts

    private val _currentMonth = MutableLiveData(DateUtils.currentYearMonth())
    val currentMonth: LiveData<String> = _currentMonth

    private val _totalIncome = MutableLiveData(0.0)
    val totalIncome: LiveData<Double> = _totalIncome

    private val _totalExpense = MutableLiveData(0.0)
    val totalExpense: LiveData<Double> = _totalExpense

    private val _balance = MutableLiveData(0.0)
    val balance: LiveData<Double> = _balance

    fun loadAccounts() {
        viewModelScope.launch {
            val month = _currentMonth.value ?: return@launch
            val list = repo.getAccountsByMonth(month)
            _accounts.value = list
            _totalIncome.value = list.filter { it.isIncome }.sumOf { it.amount }
            _totalExpense.value = list.filter { it.isExpense }.sumOf { it.amount }
            _balance.value = (_totalIncome.value ?: 0.0) - (_totalExpense.value ?: 0.0)
        }
    }

    fun prevMonth() {
        _currentMonth.value = DateUtils.prevMonth(_currentMonth.value ?: return)
        loadAccounts()
    }

    fun nextMonth() {
        _currentMonth.value = DateUtils.nextMonth(_currentMonth.value ?: return)
        loadAccounts()
    }

    fun addAccount(entry: AccountEntry) {
        viewModelScope.launch {
            repo.addAccount(entry)
            loadAccounts()
        }
    }

    fun getCategoryStats(): List<Pair<String, Double>> {
        val list = _accounts.value?.filter { it.isExpense } ?: emptyList()
        return list.groupBy { it.category }
            .map { it.key to it.value.sumOf { e -> e.amount } }
            .sortedByDescending { it.second }
    }
}
