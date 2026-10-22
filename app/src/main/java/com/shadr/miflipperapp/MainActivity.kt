package com.shadr.miflipperapp

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.shadr.miflipperapp.databinding.ActivityMainBinding
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: FlipViewModel by lazy {
        (application as MiFlipperApplication).mainViewModel
    }

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LocaleHelper.getPersistedLocaleCode(newBase)

        val contextWithLocale = LocaleHelper.setLocale(newBase, languageCode)

        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavView.setupWithNavController(navController)

        viewModel.statusText.observe(this) { text ->
            binding.statusBar.text = text
        }

        viewModel.isServiceRunning.observe(this) { isRunning ->
            if (isRunning) {
                binding.statusBar.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            } else {
                binding.statusBar.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}