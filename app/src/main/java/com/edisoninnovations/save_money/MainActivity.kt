package com.edisoninnovations.save_money

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.edisoninnovations.save_money.utils.LoadingDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.logging.Handler


class MainActivity : AppCompatActivity() {
    private lateinit var emailLayout: TextInputLayout;
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var btnLogin: Button
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        loadingDialog = LoadingDialog(this)

        val emailErrorText: TextView = findViewById(R.id.email_error_text)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btn: TextView =findViewById(R.id.register_link)
      btn.setOnClickListener{
          val intent:Intent=Intent(this,Register::class.java)
          startActivity(intent)

      }
        val btnLog:Button =findViewById(R.id.btnLogin)
        btnLog.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
               signIn()
            }
        }
        emailLayout= findViewById(R.id.email_input_layout)
        passwordLayout= findViewById(R.id.password_input_layout)
        btnLogin= findViewById(R.id.btnLogin)

        emailLayout.editText?.addTextChangedListener(textWatcher)
        passwordLayout.editText?.addTextChangedListener(textWatcher)

        emailLayout.editText?.addTextChangedListener {
            val email = emailLayout.editText?.text.toString()
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailErrorText.visibility = View.GONE
            } else {
                emailErrorText.text = "Correo electrónico no válido"
                emailErrorText.visibility = View.VISIBLE
            }
        }

    }
    private suspend fun signIn() {
        CoroutineScope(Dispatchers.Main).launch {
            loadingDialog.startLoading()
            try {
                val emailValue = emailLayout.editText?.text.toString()
                val passwordValue = passwordLayout.editText?.text.toString()
                supabase.auth.signInWith(Email) {
                    email = emailValue
                    password = passwordValue
                }
                Toast.makeText(this@MainActivity, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@MainActivity, Home::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                println("Error al iniciar sesión: ${e.message}")
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            } finally {
                loadingDialog.isDismiss()
            }
        }
    }

    private suspend fun sesion(){
        try {
            val result = supabase.auth.currentUserOrNull()
            println("Inicio de sesión exitoso: $result")
        } catch (e: Exception) {
            println("Error al iniciar sesión: ${e.message}")}
    }
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Validar el correo electrónico
            val email = emailLayout.editText?.text.toString()
            val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            if (!isEmailValid) {
                emailLayout.error = "Correo electrónico no válido"
            } else {
                emailLayout.error = null
            }

            // Validar la longitud de la contraseña
            val password = passwordLayout.editText?.text.toString()
            val isPasswordLengthValid = password.length >= 6
            if (!isPasswordLengthValid) {
                passwordLayout.error = "La contraseña debe tener al menos 6 caracteres"
            } else {
                passwordLayout.error = null
            }

            btnLogin.isEnabled = isEmailValid && isPasswordLengthValid
        }
    }
}