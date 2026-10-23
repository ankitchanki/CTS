package com.parallelc.micts

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.parallelc.micts.shizuku.ShizukuManager
import com.parallelc.micts.ui.viewmodel.SettingsViewModel

class MainApplication : Application() {
    val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this)
            .create(SettingsViewModel::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        ShizukuManager.init(this)
    }
}
