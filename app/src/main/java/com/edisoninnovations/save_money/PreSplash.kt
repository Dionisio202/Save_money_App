package com.edisoninnovations.save_money

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreSplash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.SplashTheme)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pre_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        CoroutineScope(Dispatchers.Main).launch {
            checkLoginStatus()
        }
    }

    private suspend fun checkLoginStatus() {
        var currentUser: SessionStatus
        do {
            currentUser = supabase.auth.sessionStatus.value
            if (currentUser == SessionStatus.LoadingFromStorage) {
                withContext(Dispatchers.IO) {
                    Thread.sleep(100)
                }
            }
        } while (currentUser == SessionStatus.LoadingFromStorage)

        val nextActivity = when (currentUser) {
            is SessionStatus.Authenticated -> Home_main::class.java
            else -> MainActivity::class.java
        }

        Handler().postDelayed({
            startActivity(Intent(this, nextActivity))
            finish()
        }, 900) // 900 milliseconds delay
    }
}