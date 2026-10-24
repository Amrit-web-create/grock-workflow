package com.customapplock.studio.ui.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customapplock.studio.data.AppsRepository
import com.customapplock.studio.data.InstalledApp
import com.customapplock.studio.data.LockRepository
import com.customapplock.studio.data.SettingsStore
import com.customapplock.studio.domain.LockType
import com.customapplock.studio.domain.MASTER_PKG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppFilter(val title: String) { ALL("All"), LOCKED("Locked"), UNLOCKED("Unlocked") }

data class AppRow(
    val pkg: String,
    val label: String,
    val lockType: LockType?,
    val locked: Boolean,
    val hasConfig: Boolean
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appsRepo: AppsRepository,
    private val repo: LockRepository,
    val settings: SettingsStore
) : ViewModel() {

    private val installed = MutableStateFlow<List<InstalledApp>>(emptyList())
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(AppFilter.ALL)
    val loading = MutableStateFlow(true)

    val rows: StateFlow<List<AppRow>> = combine(installed, repo.all, query, filter) { apps, locks, q, f ->
        val byPkg = locks.filter { it.packageName != MASTER_PKG }.associateBy { it.packageName }
        apps.asSequence()
            .map { a ->
                val l = byPkg[a.packageName]
                AppRow(a.packageName, a.label, l?.let { LockType.fromId(it.lockTypeId) }, l?.isLocked == true, l != null)
            }
            .filter { q.isBlank() || it.label.contains(q.trim(), ignoreCase = true) }
            .filter {
                when (f) {
                    AppFilter.ALL -> true
                    AppFilter.LOCKED -> it.locked
                    AppFilter.UNLOCKED -> !it.locked
                }
            }
            .toList()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lockedCount: StateFlow<Int> = repo.all
        .map { l -> l.count { it.isLocked && it.packageName != MASTER_PKG } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            installed.value = appsRepo.loadApps()
            loading.value = false
        }
    }

    fun toggle(pkg: String, on: Boolean) { viewModelScope.launch { repo.setLocked(pkg, on) } }
    suspend fun icon(pkg: String): ImageBitmap? = appsRepo.icon(pkg)
}
