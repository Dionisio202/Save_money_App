package com.edisoninnovations.save_money

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            startActivityForResult(intent, REQUEST_CODE_ADD_TRANSACTION)
            alertDialog.dismiss()
        }

        dialogView.findViewById<ImageButton>(R.id.add_expense_button).setOnClickListener {
            // Handle add expense action
            val intent = Intent(this@HomeInformation, AddTransaction::class.java)
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
    private fun finishWithResult() {
        val intent = Intent()
        setResult(RESULT_OK, intent)

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == REQUEST_CODE_ADD_TRANSACTION || requestCode == REQUEST_CODE_EDIT_TRANSACTION) && resultCode == RESULT_OK) {

            val selectedDate = DateManager.selectedDate ?: "Fecha no seleccionada"
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                loadTransactions(selectedDate, userId)
                finishWithResult()

            }
        }
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

    companion object {
        private const val REQUEST_CODE_ADD_TRANSACTION = 1
        private const val REQUEST_CODE_EDIT_TRANSACTION = 2

    }
    public suspend fun deleteTransaction(transactionId: Int) {
        try {
            // Eliminar referencias de imágenes en la tabla transimages
            val imagesResponse = withContext(Dispatchers.IO) {
                supabase.from("transimages").select {
                    filter {
                        eq("id_transaccion", transactionId)

                    }
                }
            }
            println("########################imagesResponse: ${imagesResponse.data}")
            // Utilizar Moshi para parsear la respuesta JSON
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, Map::class.java)
            val jsonAdapter = moshi.adapter<List<Map<String, Any>>>(type)
            val imagesData = jsonAdapter.fromJson(imagesResponse.data.toString()) ?: emptyList()

            println("########################imagesData: $imagesData")
            // Eliminar las imágenes del almacenamiento
            imagesData.forEach { image ->
                val imageUrl = image["imagen"] as? String ?: return@forEach
                val fileName = imageUrl.substringAfterLast('/')

                val deleteResponse = withContext(Dispatchers.IO) {
                    supabase.storage.from("imagenes").delete(fileName)
                }
                println("########################deleteResponse: $deleteResponse")

            }

            // Eliminar las referencias de las imágenes de la tabla transimages
            withContext(Dispatchers.IO) {
                supabase.from("transimages").delete {
                    filter {
                        eq("id_transaccion", transactionId)

                    }
                }
            }

            // Eliminar la transacción de la tabla transacciones
            withContext(Dispatchers.IO) {
                supabase.from("transacciones").delete {
                  filter {
                      eq("id_transaccion", transactionId)
                  }
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@HomeInformation, "Transacción eliminada correctamente", Toast.LENGTH_SHORT).show()
                loadTransactions(DateManager.selectedDate ?: "Fecha no seleccionada", supabase.auth.currentUserOrNull()?.id ?: "")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@HomeInformation, "Error al eliminar la transacción", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
