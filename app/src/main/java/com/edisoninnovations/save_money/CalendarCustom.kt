package com.edisoninnovations.save_money

import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import java.time.LocalDate
class CalendarCustom : AppCompatActivity() {

    private lateinit var monthYearText: TextView
    private lateinit var calendarViewPager: ViewPager2
    private lateinit var calendarAdapter: CalendarPagerAdapter
    private lateinit var eventDates: List<Pair<LocalDate, String>> // Lista de eventos

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initWidgets()

        // Definir tus eventos aquí
        eventDates = listOf(
            LocalDate.of(2024, 6, 10) to "red",
            LocalDate.of(2024, 6, 13) to "green",
            LocalDate.of(2024, 6, 20) to "mixed",
            LocalDate.of(2024, 7, 3) to "mixed"
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
}
