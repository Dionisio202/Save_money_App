// HomeViewModel.kt
package com.edisoninnovations.save_money.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.edisoninnovations.save_money.models.Transaction
import com.edisoninnovations.save_money.models.TransactionRepository
import com.edisoninnovations.save_money.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
class HomeViewModel : ViewModel() {
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _eventDates = MutableLiveData<List<Pair<LocalDate, String>>>()
    val eventDates: LiveData<List<Pair<LocalDate, String>>> = _eventDates

    private val _totalIncome = MutableLiveData<Float>()
    val totalIncome: LiveData<Float> = _totalIncome

    private val _totalExpense = MutableLiveData<Float>()
    val totalExpense: LiveData<Float> = _totalExpense

    // Flag to track whether data needs to be refreshed
    private var needsRefresh: Boolean = true

    @JsonClass(generateAdapter = true)
    data class Transaction(
        val cantidad: Float,
        val tipo: String,
        val fecha: String,
        val id_account: String?,
        val title: String?
    )

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDataTransactions(userId: String) {
        if (!needsRefresh && _transactions.value != null && _eventDates.value != null && _totalIncome.value != null && _totalExpense.value != null) {
            // Data already loaded and no refresh needed, no need to fetch again
            return
        }

        if (userId.trim().isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response2 = supabase.postgrest.rpc("get_transactions_by_user", mapOf("user_id" to userId))

                    println("#############Obteniendo data HOME--"+response2.data)



                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val type = Types.newParameterizedType(List::class.java, Transaction::class.java)
                    val jsonAdapter = moshi.adapter<List<Transaction>>(type)
                    val transacciones = jsonAdapter.fromJson(response2.data)


                    if (transacciones == null) {
                        // Handle the case where transacciones is null
                        return@launch
                    }

                    val eventMap = mutableMapOf<LocalDate, MutableList<String>>()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    transacciones.forEach { transaction ->
                        val date = sdf.parse(transaction.fecha)
                        val calendar = Calendar.getInstance()
                        calendar.time = date
                        val localDate = LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))

                        if (!eventMap.containsKey(localDate)) {
                            eventMap[localDate] = mutableListOf()
                        }
                        eventMap[localDate]?.add(transaction.tipo)
                    }

                    val finalEventMap = mutableMapOf<LocalDate, String>()
                    eventMap.forEach { (date, types) ->
                        if (types.contains("income") && types.contains("expense")) {
                            finalEventMap[date] = "mixed"
                        } else if (types.contains("income")) {
                            finalEventMap[date] = "green"
                        } else if (types.contains("expense")) {
                            finalEventMap[date] = "red"
                        }
                    }

                    val incomeTransactions = transacciones.filter { it.tipo == "income" }
                    val expenseTransactions = transacciones.filter { it.tipo == "expense" }
                    val totalIncome = incomeTransactions.sumByDouble { it.cantidad.toDouble() }
                    val totalExpense = expenseTransactions.sumByDouble { it.cantidad.toDouble() }

                    val roundedTotalIncome = totalIncome.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_EVEN).toFloat()
                    val roundedTotalExpense = totalExpense.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_EVEN).toFloat()

                    _eventDates.postValue(finalEventMap.map { it.key to it.value })
                    _totalIncome.postValue(roundedTotalIncome)
                    _totalExpense.postValue(roundedTotalExpense)
                    _transactions.postValue(transacciones)
                    TransactionRepository.setTransactions(transacciones)
                    // Set the refresh flag to false
                    needsRefresh = false

                } catch (error: Exception) {
                    error.printStackTrace()
                    println("$$$$$$$$$$$$$$--"+error.message)
                }
            }
        }
    }
    @JsonClass(generateAdapter = true)
    data class Account(
        val title: String?,
        val id: String?
    )











    // Method to set the refresh flag when a new transaction is added
    fun setNeedsRefresh() {
        needsRefresh = true
    }

}
