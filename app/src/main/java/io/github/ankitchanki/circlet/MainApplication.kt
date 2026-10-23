package io.github.ankitchanki.circlet

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import io.github.ankitchanki.circlet.shizuku.ShizukuManager
import io.github.ankitchanki.circlet.ui.viewmodel.SettingsViewModel

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
