package com.example.myapplication.screens.home

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.example.myapplication.MyAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import com.example.myapplication.utils.TAG_MAIN

object AppPreferences {
    private const val PREFS_NAME = "blocker_prefs"
    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAppEnabled(context: Context, packageName: String): Boolean {
        return getPrefs(context).getBoolean(packageName, true) // Default to enabled
    }

    fun setAppEnabled(context: Context, packageName: String, isEnabled: Boolean) {
        getPrefs(context).edit { putBoolean(packageName, isEnabled) }
    }
}

data class UiState(
    val isAccessibilityEnabled: Boolean = false,
    val appSelection: Map<String, Boolean> = mapOf(
        "com.google.android.youtube" to true,
        "com.facebook.katana" to true,
        "com.instagram.android" to true
    )
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        val currentSelections = _uiState.value.appSelection.keys.associateWith {
            AppPreferences.isAppEnabled(application.applicationContext, it)
        }
        _uiState.update { it.copy(appSelection = currentSelections) }
    }

    fun checkPermissionsStatus() {

        // Check for Accessibility Service setting
        val isAccessibilityOn = isAccessibilityServiceEnabled()

        // Update the state with the latest values
        _uiState.update { currentState ->
            currentState.copy(
                isAccessibilityEnabled = isAccessibilityOn
            )
        }
    }

    fun onAppSelectionChanged(packageName: String, isEnabled: Boolean) {
        AppPreferences.setAppEnabled(application.applicationContext, packageName, isEnabled)
        val updatedSelections = _uiState.value.appSelection.toMutableMap()
        updatedSelections[packageName] = isEnabled
        _uiState.update { it.copy(appSelection = updatedSelections) }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service =
            "${application.applicationContext.packageName}/${MyAccessibilityService::class.java.canonicalName}"
        try {
            val enabledServices = Settings.Secure.getString(
                application.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                if (colonSplitter.next().equals(service, ignoreCase = true)) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Exception while enabling accessibility service: $e")
        }
        return false
    }
}