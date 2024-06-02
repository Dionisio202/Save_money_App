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
import com.google.android.material.textfield.TextInputLayout
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Register : AppCompatActivity() {
    private lateinit var emailLayout: TextInputLayout
    private lateinit var nameLayout: TextInputLayout
    private lateinit var lastNameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var password2Layout: TextInputLayout
    private lateinit var btnRegister: Button
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        val emailErrorText: TextView = findViewById(R.id.email_error_text)
        val nameErrorText: TextView = findViewById(R.id.name_error_text)
        val lastNameErrorText: TextView = findViewById(R.id.lastName_error_text)
        val passwordErrorText: TextView = findViewById(R.id.password_error_text)
        val password2ErrorText: TextView = findViewById(R.id.password2_error_text)
        loadingDialog = LoadingDialog(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val register: Button = findViewById(R.id.btnRegister)
        register.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                   signUp()
            }
        }

        // Inicializa las vistas
        emailLayout= findViewById(R.id.email_input_layout)
        nameLayout = findViewById(R.id.name_input_layout)
        lastNameLayout= findViewById(R.id.lastName_input_layout)
        passwordLayout= findViewById(R.id.password_input_layout)
        password2Layout = findViewById(R.id.password2_input_layout)
        btnRegister= findViewById(R.id.btnRegister)

        // Agrega listeners para la validación
        emailLayout.editText?.addTextChangedListener(textWatcher)
        nameLayout.editText?.addTextChangedListener(textWatcher)
        lastNameLayout.editText?.addTextChangedListener(textWatcher)
        passwordLayout.editText?.addTextChangedListener(textWatcher)
        password2Layout.editText?.addTextChangedListener(textWatcher)

        emailLayout.editText?.addTextChangedListener {
            val email = emailLayout.editText?.text.toString()
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailErrorText.visibility = View.GONE
            } else {
                emailErrorText.text = "Correo electrónico no válido"
                emailErrorText.visibility = View.VISIBLE
            }
        }
        nameLayout.editText?.addTextChangedListener {
            val name = nameLayout.editText?.text.toString()
            if (name.matches(Regex("[a-zA-Z ]+"))) {
                nameErrorText.visibility = View.GONE
            } else {
                nameErrorText.text = "Nombre  inválido no ingrese números "
                nameErrorText.visibility = View.VISIBLE
            }
        }
        lastNameLayout.editText?.addTextChangedListener {
            val lastame = lastNameLayout.editText?.text.toString()
            if (lastame.matches(Regex("[a-zA-Z ]+"))) {
                lastNameErrorText.visibility = View.GONE
            } else {
                lastNameErrorText.text = "Apellido  inválido no ingrese números "
                lastNameErrorText.visibility = View.VISIBLE
            }
        }
        passwordLayout.editText?.addTextChangedListener {
            val password = passwordLayout.editText?.text.toString()
            if (password.length >= 6) {
                passwordErrorText.visibility = View.GONE
            } else {
                passwordErrorText.text = "La contraseña debe tener al menos 6 caracteres"
                passwordErrorText.visibility = View.VISIBLE
            }
        }
        password2Layout.editText?.addTextChangedListener {
            val password2 = password2Layout.editText?.text.toString()
            val password = passwordLayout.editText?.text.toString()

            val doPasswordsMatch = password == password2
            if (doPasswordsMatch) {
                password2ErrorText.visibility = View.GONE
            } else {
                password2ErrorText.text = "Las contraseñas no coinciden"
                password2ErrorText.visibility = View.VISIBLE
            }
        }
    }
    @kotlinx.serialization.Serializable
    data class Profile(
        val user_id: String,
        val name: String,
        val last_name: String
    )
    private suspend fun signUp() {
        loadingDialog.startLoading()
        try {
            val emailValue = emailLayout.editText?.text.toString()
            val passwordValue = passwordLayout.editText?.text.toString()
            val nameValue = nameLayout.editText?.text.toString()
            val lastNameValue = lastNameLayout.editText?.text.toString()

            val response = supabase.auth.signUpWith(Email) {
                email = emailValue
                password = passwordValue
            }
            val userId = response?.id ?: ""
            val profile = Profile(
                user_id = userId,
                name = nameValue,
                last_name = lastNameValue
            )
            supabase.from("profile").insert(profile)
            Toast.makeText(this, "Registro exitoso ", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@Register, MainActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al Registrarse: ${e.message}", Toast.LENGTH_SHORT).show()
            println("############################################Error al Registrarse: ${e.message}")
        } finally {
            loadingDialog.isDismiss()
        }

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

            val name = nameLayout.editText?.text.toString()
            val lastName = lastNameLayout.editText?.text.toString()
            val isNameValid = name.matches(Regex("[a-zA-Z ]+"))
            val isLastNameValid = lastName.matches(Regex("[a-zA-Z ]+"))
            if (!isNameValid) {
                nameLayout.error = "Nombre inválido no ingrese números"
            } else {
                nameLayout.error = null
            }
            if (!isLastNameValid) {
                lastNameLayout.error = "Apellido inválido no ingrese números"

            } else {
                lastNameLayout.error = null
            }

            // Validar la longitud de la contraseña
            val password = passwordLayout.editText?.text.toString()
            val isPasswordLengthValid = password.length >= 6
            if (!isPasswordLengthValid) {
                passwordLayout.error = "La contraseña debe tener al menos 6 caracteres"
            } else {
                passwordLayout.error = null
            }

            // Validar que las contraseñas coincidan
            val password2 = password2Layout.editText?.text.toString()
            val doPasswordsMatch = password == password2
            if (!doPasswordsMatch) {
                password2Layout.error = "Las contraseñas no coinciden"
            } else {
                password2Layout.error = null
            }

            btnRegister.isEnabled = isEmailValid && isNameValid && isLastNameValid && isPasswordLengthValid && doPasswordsMatch
        }
    }

}