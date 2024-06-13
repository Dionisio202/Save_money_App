package com.edisoninnovations.save_money

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.time.LocalDate

class CalendarPagerAdapter(fragmentActivity: FragmentActivity, private val eventDates: List<Pair<LocalDate, String>>) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return Int.MAX_VALUE // Número muy grande para permitir desplazamiento infinito
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createFragment(position: Int): Fragment {
        val date = LocalDate.now().plusMonths(position - Int.MAX_VALUE / 2L)
        return MonthFragment().apply {
            arguments = Bundle().apply {
                putSerializable("date", date)
                putSerializable("events", ArrayList(eventDates))
            }
        }
    }
}

