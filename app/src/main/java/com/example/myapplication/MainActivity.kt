package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.myapplication.screens.navigation.AppNavigationRoot
import com.example.myapplication.ui.Theme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.screens.home.HomeViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var homeViewModel: HomeViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestBatteryOptimization()
        homeViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )
            .get(HomeViewModel::class.java)
        setContent {
            Theme.MyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // CHANGED: The onCheckBatteryClick lambda is no longer needed here.
                    AppNavigationRoot(
                        homeViewModel = homeViewModel
                    )
                }
            }
        }

    }

    private fun checkAndRequestBatteryOptimization() {
        val powerManager = getSystemService(PowerManager::class.java)
        val packageName = packageName

        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            // Optional: Show a toast or message saying it's already enabled.
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Reliability Setting Needed")
            .setMessage("To ensure the Shorts blocker can run reliably without being shut down by your phone, please allow the app to run without battery restrictions.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = "package:$packageName".toUri()
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}


private fun AppCompatActivity.onCreate(bundle: Bundle?) {}
