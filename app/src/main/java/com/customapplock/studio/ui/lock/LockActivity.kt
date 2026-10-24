package com.customapplock.studio.ui.lock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import com.customapplock.studio.service.SessionManager
import com.customapplock.studio.ui.theme.AppLockTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Full-screen lock shown on top of a protected app. Own task, hidden from recents, screenshots blocked. */
@AndroidEntryPoint
class LockActivity : FragmentActivity() {

    @Inject lateinit var session: SessionManager
    private val pkgState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        pkgState.value = intent.getStringExtra(EXTRA_PKG).orEmpty()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goHome() }
        })
        setContent {
            AppLockTheme(darkTheme = true) {
                val pkg by pkgState
                if (pkg.isNotEmpty()) {
                    LockScreenHost(pkg = pkg, showAd = true, onUnlocked = { unlock(pkg) })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pkgState.value = intent.getStringExtra(EXTRA_PKG).orEmpty()
    }

    private fun unlock(pkg: String) {
        session.markUnlocked(pkg)
        finish()
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isFinishing) finish()
    }

    override fun onDestroy() {
        session.lockShowingFor = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PKG = "extra_pkg"
        fun intent(ctx: Context, pkg: String): Intent =
            Intent(ctx, LockActivity::class.java)
                .putExtra(EXTRA_PKG, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
