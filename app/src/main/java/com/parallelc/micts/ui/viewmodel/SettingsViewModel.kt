package com.parallelc.micts.ui.viewmodel

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.provider.Settings
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language
import com.parallelc.micts.overlay.GestureOverlayService
import com.parallelc.micts.shizuku.ShizukuManager
import com.parallelc.micts.shizuku.ShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private var appConfigPref : SharedPreferences
    private val _appConfig = MutableStateFlow<Map<String,Any>>(emptyMap())
    val appConfig: StateFlow<Map<String,Any>> = _appConfig
    private val _locale = MutableStateFlow<Locale>(Locale.ROOT)
    val locale: StateFlow<Locale> = _locale

    val shizukuStatus: StateFlow<ShizukuStatus> = ShizukuManager.status

    private val _canDrawOverlays = MutableStateFlow(Settings.canDrawOverlays(application))
    val canDrawOverlays: StateFlow<Boolean> = _canDrawOverlays

    /** Result of the last "Set Google as default assistant" action, null if not run yet. */
    private val _assistantResult = MutableStateFlow<Boolean?>(null)
    val assistantResult: StateFlow<Boolean?> = _assistantResult

    var showAboutDialog = mutableStateOf(false)
    @OptIn(ExperimentalMaterial3Api::class)
    var topAppBarState = TopAppBarState(
        -Float.MAX_VALUE,
        0f,
        0f
    )
    var menuExpanded = mutableStateOf(false)
    var languageExpanded = mutableStateOf(false)
    var scrollState = ScrollState(0)
    var triggerServiceExpanded = mutableStateOf(false)
    var gestureActionExpanded = mutableStateOf(false)

    init {
        appConfigPref = application.getSharedPreferences(AppConfig.CONFIG_NAME, MODE_PRIVATE)
        _appConfig.value = AppConfig.DEFAULT_CONFIG + appConfigPref.all.filterValues { it != null }.mapValues { it.value as Any }
        _locale.value = Language.entries[_appConfig.value[AppConfig.KEY_LANGUAGE] as Int].toLocale()
    }

    fun updateAppConfig(key: String, value: Any) {
        if (key == AppConfig.KEY_LANGUAGE) {
            _locale.value = Language.entries[value as Int].toLocale()
        } else {
            _appConfig.value = _appConfig.value.toMutableMap().apply {
                this[key] = value
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (value) {
                is Long -> appConfigPref.edit().putLong(key, value).commit()
                is Int -> appConfigPref.edit().putInt(key, value).commit()
                is Boolean -> appConfigPref.edit().putBoolean(key, value).commit()
            }
            if (key == AppConfig.KEY_CORNER_GESTURE) {
                launch(Dispatchers.Main) { GestureOverlayService.sync(getApplication()) }
            }
        }
    }

    /** Re-reads permission state, e.g. when returning from Shizuku or system settings. */
    fun refreshPermissions() {
        ShizukuManager.refresh()
        _canDrawOverlays.value = Settings.canDrawOverlays(getApplication())
        GestureOverlayService.sync(getApplication())
    }

    /**
     * Enables the corner gesture. The overlay permission is granted through Shizuku when possible,
     * otherwise [openOverlaySettings] sends the user to the system page.
     */
    fun setCornerGesture(enabled: Boolean, openOverlaySettings: () -> Unit) {
        updateAppConfig(AppConfig.KEY_CORNER_GESTURE, enabled)
        if (!enabled || _canDrawOverlays.value) return
        viewModelScope.launch {
            if (ShizukuManager.isGranted && ShizukuManager.grantOverlayPermission()) {
                refreshPermissions()
            } else {
                openOverlaySettings()
            }
        }
    }

    fun setGoogleAsAssistant() {
        viewModelScope.launch {
            _assistantResult.value = ShizukuManager.setGoogleAsAssistant()
        }
    }
}
