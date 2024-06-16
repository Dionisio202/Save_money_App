package com.edisoninnovations.save_money.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.edisoninnovations.save_money.ui.home.HomeViewModel

class GalleryViewModel : ViewModel() {

    private val _transactions = MutableLiveData<List<HomeViewModel.Transaction>>()
    val transactions: LiveData<List<HomeViewModel.Transaction>> = _transactions

    private val _totalIncome = MutableLiveData<Float>()
    val totalIncome: LiveData<Float> = _totalIncome

    private val _totalExpense = MutableLiveData<Float>()
    val totalExpense: LiveData<Float> = _totalExpense

    private val _finalBalance = MutableLiveData<Float>()
    val finalBalance: LiveData<Float> = _finalBalance

    private val _accountTitle = MutableLiveData<String>()
    val accountTitle: LiveData<String> = _accountTitle

    fun setTransactions(transactions: List<HomeViewModel.Transaction>) {
        println("GalleryViewModel: Setting transactions: $transactions")
        _transactions.value = transactions

        val totalIncome = transactions.filter { it.tipo == "income" }.sumByDouble { it.cantidad.toDouble() }.toFloat()
        val totalExpense = transactions.filter { it.tipo == "expense" }.sumByDouble { it.cantidad.toDouble() }.toFloat()
        val finalBalance = totalIncome - totalExpense

        println("GalleryViewModel: Calculated total income: $totalIncome, total expense: $totalExpense, final balance: $finalBalance")

        _totalIncome.value = totalIncome
        _totalExpense.value = totalExpense
        _finalBalance.value = finalBalance

        if (transactions.isNotEmpty()) {
            _accountTitle.value = transactions[0].title ?: "Unknown"
            println("GalleryViewModel: Setting account title: ${transactions[0].title}")
        }
    }
}
