package com.customapplock.studio.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.customapplock.studio.domain.RelockMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val lockEnabled: Boolean = true,
    val relockMode: RelockMode = RelockMode.GRACE,
    val graceSeconds: Int = 30,
    val intruderSelfie: Boolean = false,
    val intruderAttempts: Int = 3,
    val biometric: Boolean = false,
    val securityQuestion: String = "",
    val securityAnswerHash: String = ""
)

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private val prefs: SharedPreferences by lazy {
        try {
            val key = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                ctx, "applock_secure_prefs", key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            ctx.getSharedPreferences("applock_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    init {
        scope.launch { _state.value = load() }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        _state.value = next
        scope.launch { save(next) }
    }

    private fun load(): AppSettings {
        val p = prefs
        return AppSettings(
            lockEnabled = p.getBoolean("lockEnabled", true),
            relockMode = runCatching { RelockMode.valueOf(p.getString("relockMode", "GRACE") ?: "GRACE") }
                .getOrDefault(RelockMode.GRACE),
            graceSeconds = p.getInt("graceSeconds", 30),
            intruderSelfie = p.getBoolean("intruderSelfie", false),
            intruderAttempts = p.getInt("intruderAttempts", 3),
            biometric = p.getBoolean("biometric", false),
            securityQuestion = p.getString("securityQuestion", "") ?: "",
            securityAnswerHash = p.getString("securityAnswerHash", "") ?: ""
        )
    }

    private fun save(s: AppSettings) {
        prefs.edit()
            .putBoolean("lockEnabled", s.lockEnabled)
            .putString("relockMode", s.relockMode.name)
            .putInt("graceSeconds", s.graceSeconds)
            .putBoolean("intruderSelfie", s.intruderSelfie)
            .putInt("intruderAttempts", s.intruderAttempts)
            .putBoolean("biometric", s.biometric)
            .putString("securityQuestion", s.securityQuestion)
            .putString("securityAnswerHash", s.securityAnswerHash)
            .apply()
    }
}
