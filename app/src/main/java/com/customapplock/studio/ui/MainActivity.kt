package com.customapplock.studio.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customapplock.studio.data.LockRepository
import com.customapplock.studio.domain.MASTER_PKG
import com.customapplock.studio.service.GuardService
import com.customapplock.studio.service.SessionManager
import com.customapplock.studio.ui.lock.LockScreenHost
import com.customapplock.studio.ui.theme.AppLockTheme
import com.customapplock.studio.util.Permissions
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(repo: LockRepository, val session: SessionManager) : ViewModel() {
    /** null while loading, then whether App Lock Studio itself is protected. */
    val hasMaster: StateFlow<Boolean?> = repo.all.map<List<com.customapplock.studio.data.AppLockEntity>, Boolean?> { l ->
        l.any { it.packageName == MASTER_PKG }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppLockTheme { Root() } }
    }

    override fun onStart() {
        super.onStart()
        session.checkMasterTimeout(30_000)
        if (Permissions.isAccessibilityOn(this)) GuardService.start(this)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) session.markMasterLeft()
    }
}

@Composable
private fun Root(vm: MainViewModel = hiltViewModel()) {
    val hasMaster by vm.hasMaster.collectAsState()
    val unlocked by vm.session.masterUnlocked.collectAsState()
    when {
        hasMaster == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        hasMaster == true && !unlocked -> AppLockTheme(darkTheme = true) {
            LockScreenHost(pkg = MASTER_PKG, showAd = false, onUnlocked = { vm.session.setMasterUnlocked() })
        }
        else -> AppNav()
    }
}
