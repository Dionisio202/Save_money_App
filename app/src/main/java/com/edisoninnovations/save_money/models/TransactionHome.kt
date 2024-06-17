package com.edisoninnovations.save_money.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.edisoninnovations.save_money.ui.home.HomeViewModel

object TransactionRepository {
    private val _transactions = MutableLiveData<List<HomeViewModel.Transaction>>()
    val transactions: LiveData<List<HomeViewModel.Transaction>> = _transactions

    private val _needsRefresh = MutableLiveData<Boolean>(true)
    val needsRefresh: LiveData<Boolean> = _needsRefresh

    fun setTransactions(transactions: List<HomeViewModel.Transaction>) {
        _transactions.postValue(transactions)
    }

    fun setNeedsRefresh(needsRefresh: Boolean) {
        _needsRefresh.postValue(needsRefresh)
    }
}
