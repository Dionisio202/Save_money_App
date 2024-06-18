package com.edisoninnovations.save_money

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.edisoninnovations.save_money.models.AccountData
import com.edisoninnovations.save_money.models.TransactionRepository
import com.edisoninnovations.save_money.utils.LoadingDialog
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditAccount : AppCompatActivity() {
    private lateinit var nameInput: EditText
    private var accountId: String? = null
    private lateinit var saveButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var deleteButton: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_account)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        loadingDialog = LoadingDialog(this)
        nameInput = findViewById(R.id.name_input)
        saveButton = findViewById(R.id.save_button)
        cancelButton = findViewById(R.id.cancel_button)
        deleteButton = findViewById(R.id.delete_button)
        // Retrieve account details from the intent
        accountId = intent.getStringExtra("id_account")
        val accountTitle = intent.getStringExtra("title")

        // Populate the UI fields with account details
        nameInput.setText(accountTitle)

        saveButton.setOnClickListener {
            lifecycleScope.launch {
                editarAccount()
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }
        deleteButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmar eliminación")
            builder.setMessage("¿Estás seguro de que deseas eliminar esta cuenta? Esta acción  eliminara la cuenta  pero los datos de las transacciones no se eliminarán.")
            builder.setPositiveButton("Eliminar") { dialog, which ->
                lifecycleScope.launch {
                    deleteAccount()
                }
            }
            builder.setNegativeButton("Cancelar") { dialog, which ->
                // Cerrar el diálogo y no hacer nada
                dialog.dismiss()
            }
            builder.show()
        }

    }

    private suspend fun deleteAccount() {
        loadingDialog.startLoading()
        try {
            if (accountId != null) {
                val response = withContext(Dispatchers.IO) {
                    supabase.postgrest.rpc("eliminar_cuenta_y_actualizar_transacciones", mapOf("account_id_to_delete" to accountId!!))
                }
                showToast("Cuenta eliminada exitosamente")
                val resultIntent = Intent().apply {
                    putExtra("deleted_account_id", accountId)
                }
                setResult(RESULT_OK, resultIntent)
                TransactionRepository.setNeedsRefresh(true) // Trigger the refresh
                finish()
            } else {
                showToast("ID de cuenta no válido")
            }
        } catch (e: Exception) {
            runOnUiThread {
                showToast("Error al eliminar la cuenta: ${e.message}")
                println("YYYYYYYYYYYYYYYYYYYYYYYYDELETE"+e.message)
            }
        } finally {
            loadingDialog.isDismiss()
        }
    }

    private suspend fun editarAccount() {
        loadingDialog.startLoading()
        try {
            val newTitle = nameInput.text.toString().trim()
            if (accountId != null && newTitle.isNotEmpty()) {
                if (isValidTitle(newTitle)) {

                    val response = withContext(Dispatchers.IO) {
                        supabase.from("accounts").update(
                            mapOf("title" to newTitle)
                        ) {
                            filter {
                                eq("id_account", accountId!!)
                            }
                            select()
                        }
                    }
                    showToast("Cuenta actualizada exitosamente")
                    val resultIntent = Intent().apply {
                        putExtra("updated_account_id", accountId)
                        putExtra("updated_account_title", newTitle)
                    }
                    setResult(RESULT_OK, resultIntent)
                    TransactionRepository.setNeedsRefresh(true) // Trigger the refresh
                    finish()
                } else {
                    showToast("Por favor ingresa más de 3 caracteres ")
                }
            } else {
                showToast("Por favor ingresa un título correcto")
            }
        } catch (e: Exception) {
            runOnUiThread {
                showToast("Error al actualizar la cuenta: ${e.message}")
            }
        } finally {
            loadingDialog.isDismiss()
        }
    }

    private fun isValidTitle(title: String): Boolean {
        return title.length > 3 && !title.all { it.isDigit() }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
