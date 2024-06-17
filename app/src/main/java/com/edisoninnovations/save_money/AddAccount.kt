package com.edisoninnovations.save_money

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.edisoninnovations.save_money.models.AccountData
import com.edisoninnovations.save_money.models.TransactionData
import com.edisoninnovations.save_money.models.TransactionRepository
import com.edisoninnovations.save_money.utils.LoadingDialog
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddAccount : AppCompatActivity() {
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var saveButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var nameInput: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_account)
        loadingDialog = LoadingDialog(this)
        saveButton = findViewById(R.id.save_button)
        cancelButton = findViewById(R.id.cancel_button)
        nameInput = findViewById(R.id.name_input)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val userId = supabase.auth.currentUserOrNull()?.id
        saveButton.setOnClickListener {
            val title = nameInput.text.toString()
            if (isValidTitle(title)) {
                lifecycleScope.launch {
                    if (userId != null) {
                        createAccount(userId, title)
                    } else {
                        Toast.makeText(this@AddAccount, "User not logged in", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@AddAccount, "El nombre debe tener más de 3 caracteres y no puede ser solo números", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }
    private fun isValidTitle(title: String): Boolean {
        return title.length > 3 && !title.all { it.isDigit() }
    }
    private  suspend  fun createAccount(usuario: String, title: String) {
        loadingDialog.startLoading()
        try {
            val accountData = AccountData(
                id_usuario = usuario,
                title = title,
            )
            val response = withContext(Dispatchers.IO) {
                supabase.from("accounts").insert(accountData) {
                    select()
                }
            }
            // Manejar la respuesta si es necesario
            Toast.makeText(this@AddAccount, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
            TransactionRepository.setNeedsRefresh(true)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this@AddAccount, e.message, Toast.LENGTH_SHORT).show()
        } finally {
            loadingDialog.isDismiss()
        }
    }
}