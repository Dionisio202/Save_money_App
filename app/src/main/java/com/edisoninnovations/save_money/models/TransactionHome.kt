package com.edisoninnovations.save_money.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.edisoninnovations.save_money.ui.home.HomeViewModel

object TransactionRepository {
    private val _transactions = MutableLiveData<List<HomeViewModel.Transaction>>()
    val transactions: LiveData<List<HomeViewModel.Transaction>> = _transactions

    fun setTransactions(transactions: List<HomeViewModel.Transaction>) {
        _transactions.postValue(transactions) // Usar postValue
    }
}
