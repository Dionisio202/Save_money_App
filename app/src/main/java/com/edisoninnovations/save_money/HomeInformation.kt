package com.edisoninnovations.save_money

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edisoninnovations.save_money.DataManager.DateManager
import com.edisoninnovations.save_money.DataManager.obtenerImagenesPorTransacciones
import com.edisoninnovations.save_money.DataManager.obtenerTransacciones
import com.edisoninnovations.save_money.adapter.TransactionsAdapter
import com.edisoninnovations.save_money.models.Transaction
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HomeInformation : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionsAdapter
    private val transactions: MutableList<Transaction> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_information)
        // Recuperar la fecha seleccionada del Singleton
        val selectedDate = DateManager.selectedDate ?: "Fecha no seleccionada"
        val formattedDate = formatDate(selectedDate)
        val userId = supabase.auth.currentUserOrNull()?.id
        val headerTextView: TextView = findViewById(R.id.header)
        headerTextView.text = formattedDate


        // Set up RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TransactionsAdapter(this, transactions)
        recyclerView.adapter = adapter

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load transactions (example data, replace with real data fetching logic)
        if (userId != null) {
            loadTransactions(selectedDate, userId)
            println("###############################selectedDate: $selectedDate")
        }
        // Set up Add Transaction Button
        val addTransactionButton: ImageButton = findViewById(R.id.add_transaction_button)
        addTransactionButton.setOnClickListener {
            showAddTransactionDialog()
        }
    }
    private fun showAddTransactionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)

        val alertDialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<ImageButton>(R.id.add_income_button).setOnClickListener {
            // Handle add income action
            val intent = Intent(this@HomeInformation, AddTransaction::class.java)
            intent.putExtra("isIncome", true)
            intent.putExtra("tipo", "income")
            startActivity(intent)
            alertDialog.dismiss()

        }

        dialogView.findViewById<ImageButton>(R.id.add_expense_button).setOnClickListener {
            // Handle add expense action
            val intent = Intent(this@HomeInformation, AddTransaction::class.java)
            intent.putExtra("isIncome", false)
            intent.putExtra("tipo", "expense")
            startActivity(intent)
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
    private fun loadTransactions(fecha: String, idUsuario: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val fetchedTransactions = obtenerTransacciones(fecha, idUsuario)
            if (fetchedTransactions != null) {
                // Get transaction IDs to fetch images
                val idsTransacciones = fetchedTransactions.map { it.id_transaccion }
                val fetchedImages = obtenerImagenesPorTransacciones(idsTransacciones)
                // Update transactions with images
                val transactionsWithImages = fetchedTransactions.map { transaction ->
                    transaction.copy(
                        imageUrls = fetchedImages?.get(transaction.id_transaccion)
                    )
                }

                updateTransactions(transactionsWithImages)
            }
        }
    }
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-M-d", Locale("es", "ES"))
            val outputFormat = SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
            val date = inputFormat.parse(dateString)
            val formattedDate = outputFormat.format(date)

            // Capitalize only the first letter of the day of the week
            formattedDate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } catch (e: Exception) {
            "Fecha no válida"
        }
    }

    private fun updateTransactions(newTransactions: List<Transaction>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        adapter.notifyDataSetChanged()
    }
}
