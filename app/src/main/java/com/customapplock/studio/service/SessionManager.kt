package com.customapplock.studio.service

import com.customapplock.studio.domain.RelockMode
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory unlock state. unlocked[pkg] = 0 while inside the app, otherwise the time we left it. */
@Singleton
class SessionManager @Inject constructor() {
    private val unlocked = ConcurrentHashMap<String, Long>()

    @Volatile var lockShowingFor: String? = null
    @Volatile var lockShowingSince: Long = 0L

    val masterUnlocked = MutableStateFlow(false)
    @Volatile private var masterLeftAt = 0L

    fun markUnlocked(pkg: String) { unlocked[pkg] = 0L }

    fun onLeave(pkg: String, mode: RelockMode) {
        if (!unlocked.containsKey(pkg)) return
        if (mode == RelockMode.IMMEDIATE) unlocked.remove(pkg) else unlocked[pkg] = System.currentTimeMillis()
    }

    fun isValid(pkg: String, mode: RelockMode, graceMs: Long): Boolean {
        val left = unlocked[pkg] ?: return false
        if (left == 0L) return true
        val ok = when (mode) {
            RelockMode.SCREEN_OFF -> true
            RelockMode.GRACE -> System.currentTimeMillis() - left <= graceMs
            RelockMode.IMMEDIATE -> false
        }
        if (ok) unlocked[pkg] = 0L else unlocked.remove(pkg)
        return ok
    }

    fun clearAll() { unlocked.clear() }

    fun setMasterUnlocked() { masterUnlocked.value = true; masterLeftAt = 0L }
    fun markMasterLeft() { if (masterUnlocked.value) masterLeftAt = System.currentTimeMillis() }
    fun checkMasterTimeout(timeoutMs: Long) {
        if (masterLeftAt != 0L && System.currentTimeMillis() - masterLeftAt > timeoutMs) {
            masterUnlocked.value = false
        }
        masterLeftAt = 0L
    }
}
