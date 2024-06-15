package com.edisoninnovations.save_money

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
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
import com.edisoninnovations.save_money.DataManager.DateManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class Home : AppCompatActivity() {

    private lateinit var calendarViewPager: ViewPager2
    private lateinit var monthYearText: TextView
    private lateinit var calendarAdapter: CalendarPagerAdapter
    private lateinit var eventDates: List<Pair<LocalDate, String>> // Lista de eventos

    companion object {
         const val REQUEST_CODE_HOME_INFORMATION = 2
        private const val REQUEST_CODE_ADD_TRANSACTION = 1
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

        initWidgets()

        // Definir tus eventos aquí
        eventDates = listOf(
            LocalDate.of(2019, 6, 10) to "red",

        )

        calendarAdapter = CalendarPagerAdapter(this, eventDates)
        calendarViewPager.adapter = calendarAdapter
        calendarViewPager.setCurrentItem(Int.MAX_VALUE / 2, false) // Centrar el ViewPager2

        // Actualizar el TextView con la fecha actual
        val currentDate = LocalDate.now()
        monthYearText.text = monthYearFromDate(currentDate)

        calendarViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val date = LocalDate.now().plusMonths(position - Int.MAX_VALUE / 2L)
                monthYearText.text = monthYearFromDate(date)
            }
        })

        findViewById<ImageButton>(R.id.prevMonthButton).setOnClickListener {
            calendarViewPager.currentItem = calendarViewPager.currentItem - 1
        }

        findViewById<ImageButton>(R.id.nextMonthButton).setOnClickListener {
            calendarViewPager.currentItem = calendarViewPager.currentItem + 1
        }

        val userEmail = supabase.auth.currentUserOrNull()?.email
        val tvEmail = findViewById<TextView>(R.id.tv_email)
        tvEmail.text = userEmail ?: "Email no disponible"
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            getDataTransacctions(userId)
        } else {
            println("Usuario no autenticado")
        }

        val addTransactionButton: FloatingActionButton = findViewById(R.id.fab)
        addTransactionButton.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun initWidgets() {
        calendarViewPager = findViewById(R.id.calendarViewPager)
        monthYearText = findViewById(R.id.monthYearTV)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun monthYearFromDate(date: LocalDate): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale("es", "ES"))
        val formattedDate = date.format(formatter)
        val monthYear = formattedDate.split(" ")
        val month = monthYear[0].replaceFirstChar { it.uppercase() }
        val year = monthYear[1]
        return "$month de $year"
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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDataTransacctions(userId: String) {
        if (userId.trim().isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    print("################################Obteniendo dataHome################################")
                    val response = supabase.from("transacciones").select(columns = Columns.list("cantidad", "tipo", "fecha")) {
                        filter {
                            eq("id_usuario", userId)
                        }
                    }
                    println("################################Obteniendo dataHome#${response.data}")
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val type = Types.newParameterizedType(List::class.java, Transaction::class.java)
                    val jsonAdapter = moshi.adapter<List<Transaction>>(type)
                    val transacciones = jsonAdapter.fromJson(response.data.toString())

                    val eventMap = mutableMapOf<LocalDate, MutableList<String>>()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    transacciones?.forEach { transaction ->
                        val date = sdf.parse(transaction.fecha)
                        val calendar = Calendar.getInstance()
                        calendar.time = date
                        val localDate = LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))

                        if (!eventMap.containsKey(localDate)) {
                            eventMap[localDate] = mutableListOf()
                        }
                        eventMap[localDate]?.add(transaction.tipo)
                    }

                    // Process eventMap to create final eventDates
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

                    eventDates = finalEventMap.map { it.key to it.value }

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

                        // Actualizar el calendario con los nuevos eventos
                        calendarAdapter = CalendarPagerAdapter(this@Home, eventDates)
                        calendarViewPager.adapter = calendarAdapter
                        calendarViewPager.setCurrentItem(Int.MAX_VALUE / 2, false)
                    }

                } catch (error: Exception) {
                    error.printStackTrace()
                    println("#####################Error al obtener datos: ${error.message}")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddTransactionDialog() {
        DateManager.selectedDate= LocalDate.now().toString()
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)

        val alertDialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<ImageButton>(R.id.add_income_button).setOnClickListener {
            // Handle add income action
            val intent = Intent(this@Home, AddTransaction::class.java)
            intent.putExtra("isIncome", true)
            intent.putExtra("tipo", "income")
            startActivityForResult(intent, REQUEST_CODE_ADD_TRANSACTION)
            alertDialog.dismiss()
        }

        dialogView.findViewById<ImageButton>(R.id.add_expense_button).setOnClickListener {
            // Handle add expense action
            val intent = Intent(this@Home, AddTransaction::class.java)
            intent.putExtra("isIncome", false)
            intent.putExtra("tipo", "expense")
            startActivityForResult(intent, REQUEST_CODE_ADD_TRANSACTION)
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun updateUI(totalIncome: Float, totalExpense: Float, truncatedTotal: Float) {
        val tvIncomeValue = findViewById<TextView>(R.id.tv_income_value)
        val tvExpenseValue = findViewById<TextView>(R.id.tv_expense_value)
        val tvBalance = findViewById<TextView>(R.id.tv_balance)

        tvIncomeValue.text = "$$totalIncome"
        tvExpenseValue.text = "$$totalExpense"
        tvBalance.text = "$$truncatedTotal"
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_HOME_INFORMATION && resultCode == RESULT_OK) {
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                getDataTransacctions(userId)
            }
        }
        if (requestCode == REQUEST_CODE_ADD_TRANSACTION && resultCode == RESULT_OK) {
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                getDataTransacctions(userId)
            }
        }
    }
}
