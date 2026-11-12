package com.example

import android.app.Application
import com.example.ai.NaraAiClient
import com.example.data.local.NaraDatabase
import com.example.data.prefs.SecureKeyStore
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.repo.BackupAndExportManager
import com.example.data.repo.NaraRepository

class AppContainer(application: Application) {
    val database: NaraDatabase by lazy { NaraDatabase.getInstance(application) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(application) }
    val secureKeyStore: SecureKeyStore by lazy { SecureKeyStore(application) }
    val backupManager: BackupAndExportManager by lazy { BackupAndExportManager(application) }
    val repository: NaraRepository by lazy {
        NaraRepository(
            db = database,
            prefsRepo = preferencesRepository,
            backupManager = backupManager
        )
    }
    val aiClient: NaraAiClient by lazy {
        NaraAiClient(
            context = application,
            secureKeyStore = secureKeyStore
        )
    }
}

class NaraApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
