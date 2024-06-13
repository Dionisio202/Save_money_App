package com.edisoninnovations.save_money

import CalendarAdapter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.YearMonth

class MonthFragment : Fragment() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var selectedDate: LocalDate
    private lateinit var eventDates: List<Pair<LocalDate, String>> // Añadir lista de eventos

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_month, container, false)
        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView)

        selectedDate = arguments?.getSerializable("date") as LocalDate? ?: LocalDate.now()
        eventDates = arguments?.getSerializable("events") as List<Pair<LocalDate, String>>? ?: emptyList() // Recuperar lista de eventos
        setMonthView()

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setMonthView() {
        val daysInMonth = daysInMonthArray(selectedDate)

        val calendarAdapter = CalendarAdapter(daysInMonth, object : CalendarAdapter.OnItemListener {
            override fun onItemClick(position: Int, dayText: String) {
                // Manejar clics en los días del calendario
            }
        }, selectedDate, eventDates)
        val layoutManager = GridLayoutManager(requireContext(), 7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun daysInMonthArray(date: LocalDate): ArrayList<String> {
        val daysInMonthArray = ArrayList<String>()
        val yearMonth = YearMonth.from(date)

        val daysInMonth = yearMonth.lengthOfMonth()
        val firstOfMonth = date.withDayOfMonth(1)
        val dayOfWeek = firstOfMonth.dayOfWeek.value

        for (i in 1..42) {
            if (i <= dayOfWeek || i > daysInMonth + dayOfWeek) {
                daysInMonthArray.add("")
            } else {
                daysInMonthArray.add((i - dayOfWeek).toString())
            }
        }
        return daysInMonthArray
    }
}

