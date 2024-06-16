package com.edisoninnovations.save_money

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navOptions
import com.edisoninnovations.save_money.databinding.ActivityHomeMainBinding
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

class Home_main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarHomeMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_home_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)

        val userEmail = supabase.auth.currentUserOrNull()?.email

if(userEmail==null){
    Snackbar.make(binding.root, "Error de conexión , porfavor intentelo de nuevo", Snackbar.LENGTH_LONG).show()

}
        val emailTextView: TextView = navView.getHeaderView(0).findViewById(R.id.tv_email)
        emailTextView.text = userEmail ?: getString(R.string.nav_header_subtitle)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_home_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Pasar el resultado al fragmento
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_home_main)
        val fragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
        fragment?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_home_main)
        val navOptions = navOptions {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        when (item.itemId) {
            R.id.nav_home -> {
                navController.navigate(R.id.nav_home, null, navOptions) // Navegar al fragmento Home
                binding.drawerLayout.closeDrawers()
                return true
            }
            R.id.nav_gallery -> {
                navController.navigate(R.id.nav_gallery, null, navOptions) // Navegar al fragmento Gallery
                binding.drawerLayout.closeDrawers()
                return true
            }
            R.id.close_session -> {
                logout()
                return true
            }
            else -> return false
        }
    }


    private fun logout() {
        lifecycleScope.launch {
            supabase.auth.signOut()
            val intent = Intent(this@Home_main, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
