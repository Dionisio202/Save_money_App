package com.edisoninnovations.save_money

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.CalendarView
import android.widget.Button
import android.widget.TextView
import com.edisoninnovations.save_money.DataManager.DateManager
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import java.util.*

class Home : AppCompatActivity() {
    private lateinit var calendarView: CalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Configura los WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // Configurar la localización del CalendarView
        calendarView = findViewById(R.id.calendarView)
        val locale = Locale("es", "ES")
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        createConfigurationContext(config)

        val prevYearButton = findViewById<Button>(R.id.prev_year_button)
        val nextYearButton = findViewById<Button>(R.id.next_year_button)

        prevYearButton.setOnClickListener { changeYear(-1) }
        nextYearButton.setOnClickListener { changeYear(1) }

        val userEmail = supabase.auth.currentUserOrNull()?.email
        val tvEmail = findViewById<TextView>(R.id.tv_email)
        tvEmail.text = userEmail ?: "Email no disponible"
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            getDataTransacctions(userId)
        } else {
            println("Usuario no autenticado")
        }

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$year-${month + 1}-$dayOfMonth"
            DateManager.selectedDate = selectedDate
            println("Fecha seleccionada: $selectedDate")
            val intent = Intent(this@Home, CalendarCustom::class.java)
            startActivityForResult(intent, REQUEST_CODE_HOME_INFORMATION)

        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_HOME_INFORMATION && resultCode == RESULT_OK) {
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                getDataTransacctions(userId)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_HOME_INFORMATION = 2
    }

    private fun changeYear(offset: Int) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = calendarView.date
        calendar.add(Calendar.YEAR, offset)
        calendarView.date = calendar.timeInMillis
    }

    private fun setupPieChart(pieChart: PieChart, totalIncome: Float, totalExpense: Float) {
        val total = totalIncome + totalExpense
        val incomePercentage = if (total > 0) (totalIncome / total) * 100 else 0f
        val expensePercentage = if (total > 0) (totalExpense / total) * 100 else 0f

        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(incomePercentage))
        entries.add(PieEntry(expensePercentage))

        val dataSet = PieDataSet(entries, "")
        val colors = listOf(
            ContextCompat.getColor(this, android.R.color.holo_green_dark),
            ContextCompat.getColor(this, android.R.color.holo_red_dark)
        )
        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setValueTextSize(12f)
        data.setValueTextColor(android.graphics.Color.WHITE)
        data.setValueFormatter(PercentageFormatter())

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.invalidate()
    }

    private inner class PercentageFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return String.format(Locale.getDefault(), "%.1f%%", value)
        }
    }

    @JsonClass(generateAdapter = true)
    data class Transaction(
        val cantidad: Float,
        val tipo: String,
        val fecha: String
    )

    private fun getDataTransacctions(userId: String) {
        if (userId.trim().isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    print("################################Obteniendo dataHome################################")
                    val response = supabase.from("transacciones").select(columns = Columns.list("cantidad", "tipo","fecha")) {
                        filter {
                            eq("id_usuario", userId)
                        }
                    }
                        println("################################Obteniendo dataHome#"+response.data)
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val type = Types.newParameterizedType(List::class.java, Transaction::class.java)
                    val jsonAdapter = moshi.adapter<List<Transaction>>(type)
                    val transacciones = jsonAdapter.fromJson(response.data.toString())

                    val incomeTransactions = transacciones?.filter { it.tipo == "income" } ?: emptyList()
                    val expenseTransactions = transacciones?.filter { it.tipo == "expense" } ?: emptyList()
                    val totalIncome = incomeTransactions.sumByDouble { it.cantidad.toDouble() }
                    val totalExpense = expenseTransactions.sumByDouble { it.cantidad.toDouble() }

                    val roundedTotalIncome = totalIncome.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_EVEN).toFloat()
                    val roundedTotalExpense = totalExpense.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_EVEN).toFloat()

                    val total = totalIncome - totalExpense
                    val truncatedTotal = total.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_EVEN).toFloat()

                    withContext(Dispatchers.Main) {
                        updateUI(roundedTotalIncome, roundedTotalExpense, truncatedTotal)
                        val pieChart = findViewById<PieChart>(R.id.pieChart)
                        setupPieChart(pieChart, roundedTotalIncome, roundedTotalExpense)
                    }

                } catch (error: Exception) {
                    error.printStackTrace()
                    println("#####################Error al obtener datos: ${error.message}")
                }
            }
        }
    }

    private fun updateUI(totalIncome: Float, totalExpense: Float, truncatedTotal: Float) {
        val tvIncomeValue = findViewById<TextView>(R.id.tv_income_value)
        val tvExpenseValue = findViewById<TextView>(R.id.tv_expense_value)
        val tvBalance = findViewById<TextView>(R.id.tv_balance)

        tvIncomeValue.text = "$$totalIncome"
        tvExpenseValue.text = "$$totalExpense"
        tvBalance.text = "$$truncatedTotal"
    }
}
