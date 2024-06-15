// HomeFragment.kt
package com.edisoninnovations.save_money.ui.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.edisoninnovations.save_money.AddTransaction
import com.edisoninnovations.save_money.CalendarPagerAdapter
import com.edisoninnovations.save_money.DataManager.DateManager
import com.edisoninnovations.save_money.R
import com.edisoninnovations.save_money.supabase
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.jan.supabase.gotrue.auth
import java.time.LocalDate
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var calendarViewPager: ViewPager2
    private lateinit var monthYearText: TextView
    private lateinit var calendarAdapter: CalendarPagerAdapter
    private lateinit var pieChart: PieChart
    private val homeViewModel: HomeViewModel by viewModels()

    companion object {
        const val REQUEST_CODE_HOME_INFORMATION = 2
        private const val REQUEST_CODE_ADD_TRANSACTION = 1
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initWidgets(view)

        // Definir tus eventos aquí
        homeViewModel.eventDates.observe(viewLifecycleOwner, Observer { eventDates ->
            calendarAdapter = CalendarPagerAdapter(requireActivity(), eventDates)
            calendarViewPager.adapter = calendarAdapter
            calendarViewPager.setCurrentItem(Int.MAX_VALUE / 2, false) // Centrar el ViewPager2
        })

        // Observadores para la UI
        homeViewModel.totalIncome.observe(viewLifecycleOwner, Observer { totalIncome ->
            updateUI(totalIncome, homeViewModel.totalExpense.value ?: 0f, totalIncome - (homeViewModel.totalExpense.value ?: 0f))
            setupPieChart(pieChart, totalIncome, homeViewModel.totalExpense.value ?: 0f)
        })

        homeViewModel.totalExpense.observe(viewLifecycleOwner, Observer { totalExpense ->
            updateUI(homeViewModel.totalIncome.value ?: 0f, totalExpense, (homeViewModel.totalIncome.value ?: 0f) - totalExpense)
            setupPieChart(pieChart, homeViewModel.totalIncome.value ?: 0f, totalExpense)
        })

        calendarViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val date = LocalDate.now().plusMonths(position - Int.MAX_VALUE / 2L)
                monthYearText.text = monthYearFromDate(date)
            }
        })

        view.findViewById<ImageButton>(R.id.prevMonthButton).setOnClickListener {
            calendarViewPager.currentItem = calendarViewPager.currentItem - 1
        }

        view.findViewById<ImageButton>(R.id.nextMonthButton).setOnClickListener {
            calendarViewPager.currentItem = calendarViewPager.currentItem + 1
        }

        val userEmail = supabase.auth.currentUserOrNull()?.email
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            homeViewModel.getDataTransactions(userId)
        } else {
            println("Usuario no autenticado")
        }

        val addTransactionButton: FloatingActionButton = view.findViewById(R.id.fab)
        addTransactionButton.setOnClickListener {
            showAddTransactionDialog()
        }

        return view
    }

    private fun initWidgets(view: View) {
        calendarViewPager = view.findViewById(R.id.calendarViewPager)
        monthYearText = view.findViewById(R.id.monthYearTV)
        pieChart = view.findViewById(R.id.pieChart) // Ensure you have this ID in your layout file
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
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddTransactionDialog() {
        DateManager.selectedDate = LocalDate.now().toString()
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)

        val alertDialog = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<ImageButton>(R.id.add_income_button).setOnClickListener {
            val intent = Intent(requireContext(), AddTransaction::class.java)
            intent.putExtra("isIncome", true)
            intent.putExtra("tipo", "income")
            startActivityForResult(intent, REQUEST_CODE_ADD_TRANSACTION)
            alertDialog.dismiss()
        }

        dialogView.findViewById<ImageButton>(R.id.add_expense_button).setOnClickListener {
            val intent = Intent(requireContext(), AddTransaction::class.java)
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
        val tvIncomeValue = view?.findViewById<TextView>(R.id.tv_income_value)
        val tvExpenseValue = view?.findViewById<TextView>(R.id.tv_expense_value)
        val tvBalance = view?.findViewById<TextView>(R.id.tv_balance)

        tvIncomeValue?.text = "$$totalIncome"
        tvExpenseValue?.text = "$$totalExpense"
        tvBalance?.text = "$$truncatedTotal"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_HOME_INFORMATION && resultCode == AppCompatActivity.RESULT_OK) {
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                homeViewModel.setNeedsRefresh()
                homeViewModel.getDataTransactions(userId)
            }
        }
        if (requestCode == REQUEST_CODE_ADD_TRANSACTION && resultCode == AppCompatActivity.RESULT_OK) {
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                homeViewModel.setNeedsRefresh()
                homeViewModel.getDataTransactions(userId)
            }
        }
    }

}

