package com.customapplock.studio.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.customapplock.studio.data.AppLockEntity
import com.customapplock.studio.data.LockRepository
import com.customapplock.studio.data.SettingsStore
import com.customapplock.studio.domain.MASTER_PKG
import com.customapplock.studio.ui.lock.LockActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Detects the foreground app (window-state events only, no content access) and pops the lock
 * screen. Lock data is cached in memory so the decision takes microseconds.
 */
@AndroidEntryPoint
class LockMonitorService : AccessibilityService() {

    @Inject lateinit var repo: LockRepository
    @Inject lateinit var settings: SettingsStore
    @Inject lateinit var session: SessionManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var locked: Map<String, AppLockEntity> = emptyMap()
    @Volatile private var current: String? = null
    private var neutral: Set<String> = emptySet()
    private var homes: Set<String> = emptySet()
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == Intent.ACTION_SCREEN_OFF) {
                session.clearAll()
                current = null
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        neutral = computeNeutral()
        homes = computeHomes()
        if (!receiverRegistered) {
            registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
            receiverRegistered = true
        }
        scope.launch {
            repo.all.collect { list ->
                locked = list.filter { it.isLocked && it.packageName != MASTER_PKG }.associateBy { it.packageName }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in neutral) return
        val cls = event.className?.toString()
        if (cls != null && cls.startsWith("android.widget.")) return // popups / menus

        val s = settings.state.value
        if (pkg in homes) {
            current?.let { session.onLeave(it, s.relockMode) }
            current = null
            return
        }
        if (pkg != current) {
            current?.let { session.onLeave(it, s.relockMode) }
            current = pkg
        }
        if (!s.lockEnabled) return
        if (locked[pkg] == null) return
        if (session.isValid(pkg, s.relockMode, s.graceSeconds * 1000L)) return
        val now = System.currentTimeMillis()
        if (session.lockShowingFor == pkg && now - session.lockShowingSince < 5000) return
        showLock(pkg, now)
    }

    private fun showLock(pkg: String, now: Long) {
        session.lockShowingFor = pkg
        session.lockShowingSince = now
        try {
            startActivity(LockActivity.intent(this, pkg))
        } catch (e: Exception) {
            session.lockShowingFor = null
        }
    }

    private fun computeNeutral(): Set<String> {
        val s = mutableSetOf(packageName, "com.android.systemui", "android")
        runCatching {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.enabledInputMethodList.forEach { s += it.packageName }
        }
        return s
    }

    private fun computeHomes(): Set<String> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return runCatching {
            packageManager.queryIntentActivities(i, 0).map { it.activityInfo.packageName }.toSet()
        }.getOrDefault(emptySet())
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (receiverRegistered) runCatching { unregisterReceiver(screenReceiver) }
        receiverRegistered = false
        scope.cancel()
        super.onDestroy()
    }
}
