package com.customapplock.studio.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customapplock.studio.data.AppsRepository
import com.customapplock.studio.data.LockRepository
import com.customapplock.studio.domain.LockType
import com.customapplock.studio.domain.MASTER_PKG
import com.customapplock.studio.ui.common.AppIcon
import com.customapplock.studio.ui.common.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssignRow(val pkg: String, val label: String, val type: LockType, val locked: Boolean)

@HiltViewModel
class LocksViewModel @Inject constructor(
    private val repo: LockRepository,
    private val apps: AppsRepository
) : ViewModel() {
    val rows: StateFlow<List<AssignRow>> = repo.all.map { list ->
        list.filter { it.packageName != MASTER_PKG }
            .map { AssignRow(it.packageName, apps.label(it.packageName), LockType.fromId(it.lockTypeId), it.isLocked) }
            .sortedBy { it.label.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLocked(pkg: String, on: Boolean) { viewModelScope.launch { repo.setLocked(pkg, on) } }
    fun remove(pkg: String) { viewModelScope.launch { repo.remove(pkg) } }
    suspend fun icon(pkg: String) = apps.icon(pkg)
}

/** The "Lock assignment matrix": every protected app with a drop-down to switch its lock design. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocksScreen(onChange: (pkg: String, typeId: String) -> Unit, vm: LocksViewModel = hiltViewModel()) {
    val rows by vm.rows.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Locks", fontWeight = FontWeight.Bold) }) }) { pad ->
        if (rows.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize()) {
                EmptyState("🔓", "No locked apps yet", "Open the Apps tab and flip a switch to protect an app.")
            }
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rows, key = { it.pkg }) { r ->
                    var open by remember { mutableStateOf(false) }
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(r.pkg, 44.dp, vm::icon)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Box {
                                    TextButton(onClick = { open = true }, contentPadding = PaddingValues(0.dp)) {
                                        Text("${r.type.emoji} ${r.type.title}  ▾", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                        LockType.entries.forEach { t ->
                                            DropdownMenuItem(
                                                text = { Text("${t.emoji}  ${t.title}") },
                                                onClick = { open = false; onChange(r.pkg, t.id) }
                                            )
                                        }
                                    }
                                }
                            }
                            Switch(checked = r.locked, onCheckedChange = { vm.setLocked(r.pkg, it) })
                            IconButton(onClick = { vm.remove(r.pkg) }) { Icon(Icons.Default.Delete, "Remove lock") }
                        }
                    }
                }
            }
        }
    }
}
