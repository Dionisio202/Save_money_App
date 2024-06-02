package com.edisoninnovations.save_money

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryDialogFragment : DialogFragment() {

    interface CategoryDialogListener {
        fun onCategorySelected(category: String, categoryKey: Int)
    }

    private lateinit var listener: CategoryDialogListener
    private var isIncome: Boolean = true

    private val incomeCategories = mapOf(
        1 to "Ahorros",
        2 to "Trabajo",
        3 to "Diario",
        4 to "Regalo",
        5 to "Inversion",
        20 to "Prestamo"
    )

    private val expenseCategories = mapOf(
        6 to "Fiesta",
        7 to "Comida",
        8 to "Ropa",
        9 to "Salud",
        10 to "Carro",
        11 to "Gasolina",
        12 to "Vacaciones",
        13 to "Hijos",
        14 to "Deporte",
        15 to "Belleza",
        16 to "Entretenimiento",
        17 to "Facturas",
        18 to "Compras",
        19 to "Casa",
        4 to "Regalo",
        21 to "Deuda"
    )

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as CategoryDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement CategoryDialogListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categories = if (isIncome) incomeCategories else expenseCategories

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = CategoryAdapter(categories.toList())

        view.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dismiss()
        }
    }

    fun setIsIncome(isIncome: Boolean) {
        this.isIncome = isIncome
    }

    inner class CategoryAdapter(private val categories: List<Pair<Int, String>>) :
        RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryText: TextView = view.findViewById(R.id.category_text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (key, category) = categories[position]
            holder.categoryText.text = category
            holder.itemView.setOnClickListener {
                listener.onCategorySelected(category, key)
                dismiss()
            }
        }

        override fun getItemCount() = categories.size
    }
}
