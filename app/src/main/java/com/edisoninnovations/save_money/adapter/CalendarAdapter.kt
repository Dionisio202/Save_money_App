import android.content.Intent
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.edisoninnovations.save_money.DataManager.DateManager
import com.edisoninnovations.save_money.Home
import com.edisoninnovations.save_money.R
import com.edisoninnovations.save_money.HomeInformation
import com.edisoninnovations.save_money.ui.home.HomeFragment
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class CalendarAdapter(
    private val daysOfMonth: ArrayList<String>,
    private val onItemListener: OnItemListener,
    private val selectedDate: LocalDate,
    private val eventDates: List<Pair<LocalDate, String>> // Lista de pares de fecha y tipo de evento (rojo, verde, mixto)
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var selectedPosition = -1

    init {
        // Initialize selectedPosition with the current day position
        val today = LocalDate.now()
        if (today.year == selectedDate.year && today.monthValue == selectedDate.monthValue) {
            selectedPosition = daysOfMonth.indexOf(today.dayOfMonth.toString())
        }
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.calendar_cell, parent, false)
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.height = (parent.height / 6).toInt()
        layoutParams.setMargins(0, 0, 0, 0)
        view.layoutParams = layoutParams
        return CalendarViewHolder(view, onItemListener)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(@NonNull holder: CalendarViewHolder, position: Int) {
        holder.dayOfMonth.text = daysOfMonth[position]

        // Check if this day has an event
        val day = daysOfMonth[position].toIntOrNull()
        val event = eventDates.find { it.first.dayOfMonth == day && it.first.monthValue == selectedDate.monthValue && it.first.year == selectedDate.year }

        if (selectedPosition == position) {
            holder.dayOfMonth.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.selected_day_background)
            holder.dayOfMonth.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
        } else {
            holder.dayOfMonth.background = null
            holder.dayOfMonth.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.black))
        }

        // Define the width for the event indicators
        val indicatorWidth = 45 // Puedes ajustar este valor según sea necesario

        when (event?.second) {
            "red" -> {
                holder.eventIndicatorLeft.visibility = View.VISIBLE
                holder.eventIndicatorRight.visibility = View.GONE
                holder.eventIndicatorLeft.layoutParams = (holder.eventIndicatorLeft.layoutParams as LinearLayout.LayoutParams).apply {
                    width = indicatorWidth
                    height = 6
                    weight = 1f
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            "green" -> {
                holder.eventIndicatorLeft.visibility = View.GONE
                holder.eventIndicatorRight.visibility = View.VISIBLE
                holder.eventIndicatorRight.layoutParams = (holder.eventIndicatorRight.layoutParams as LinearLayout.LayoutParams).apply {
                    width = indicatorWidth
                    height = 6
                    weight = 1f
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            "mixed" -> {
                holder.eventIndicatorLeft.visibility = View.VISIBLE
                holder.eventIndicatorRight.visibility = View.VISIBLE
                holder.eventIndicatorLeft.layoutParams = (holder.eventIndicatorLeft.layoutParams as LinearLayout.LayoutParams).apply {
                    width = indicatorWidth / 2
                    height = 6
                    weight = 1f
                }
                holder.eventIndicatorRight.layoutParams = (holder.eventIndicatorRight.layoutParams as LinearLayout.LayoutParams).apply {
                    width = indicatorWidth / 2
                    height = 6
                    weight = 1f
                }
            }
            else -> {
                holder.eventIndicatorLeft.visibility = View.GONE
                holder.eventIndicatorRight.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener {
            if (day != null) { // Check if the day is valid
                val previousPosition = selectedPosition
                selectedPosition = holder.adapterPosition

                if (previousPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(selectedPosition)

                // Intent to start HomeInformation activity
                val context = holder.itemView.context
                DateManager.selectedDate = LocalDate.of(selectedDate.year, selectedDate.month, day).toString()
                val intent = Intent(context, HomeInformation::class.java)
                if (context is FragmentActivity) {
                    (context as FragmentActivity).startActivityForResult(intent, HomeFragment.REQUEST_CODE_HOME_INFORMATION)

                } else {
                    // Usa una alternativa para iniciar la actividad si no es FragmentActivity
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return daysOfMonth.size
    }

    class CalendarViewHolder(@NonNull itemView: View, onItemListener: OnItemListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val dayOfMonth: TextView = itemView.findViewById(R.id.cellDayText)
        val eventIndicatorLeft: View = itemView.findViewById(R.id.eventIndicatorLeft)
        val eventIndicatorRight: View = itemView.findViewById(R.id.eventIndicatorRight)
        private val onItemListener: OnItemListener

        init {
            this.onItemListener = onItemListener
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            onItemListener.onItemClick(adapterPosition, dayOfMonth.text.toString())
        }
    }

    interface OnItemListener {
        fun onItemClick(position: Int, dayText: String)
    }
}
