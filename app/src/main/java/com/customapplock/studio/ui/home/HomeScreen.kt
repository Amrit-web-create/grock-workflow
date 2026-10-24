package com.customapplock.studio.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.customapplock.studio.ui.common.AppIcon
import com.customapplock.studio.util.Permissions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onSetup: (List<String>) -> Unit,
    onPermissions: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val rows by vm.rows.collectAsState()
    val loading by vm.loading.collectAsState()
    val query by vm.query.collectAsState()
    val filter by vm.filter.collectAsState()
    val lockedCount by vm.lockedCount.collectAsState()
    val settings by vm.settings.state.collectAsState()
    var perms by remember { mutableStateOf(Permissions.check(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { perms = Permissions.check(ctx) }

    val selected = remember { mutableStateListOf<String>() }
    val selecting = selected.isNotEmpty()
    BackHandler(selecting) { selected.clear() }

    Scaffold(
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${selected.size} selected") },
                    navigationIcon = { IconButton(onClick = { selected.clear() }) { Icon(Icons.Default.Close, "Cancel") } },
                    actions = {
                        TextButton(onClick = { selected.clear(); selected.addAll(rows.map { it.pkg }) }) { Text("All") }
                        TextButton(onClick = { onSetup(selected.toList()); selected.clear() }) { Text("Lock") }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("App Lock Studio", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(22.dp)
                ) {
                    Column {
                        Text(
                            when {
                                !perms.required -> "Setup needed"
                                !settings.lockEnabled -> "Protection paused"
                                else -> "Protection active"
                            },
                            color = MaterialTheme.colorScheme.onPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            if (lockedCount == 1) "1 app locked" else "$lockedCount apps locked",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .7f), fontSize = 15.sp
                        )
                        if (!perms.required) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = onPermissions,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)
                            ) { Text("Grant permissions", fontWeight = FontWeight.Bold) }
                        }
                    }
                    Text("🔒", fontSize = 44.sp, modifier = Modifier.align(Alignment.CenterEnd))
                }
            }
            item {
                OutlinedTextField(
                    value = query, onValueChange = { vm.query.value = it },
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true, shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFilter.entries.forEach {
                        FilterChip(selected = filter == it, onClick = { vm.filter.value = it }, label = { Text(it.title) })
                    }
                }
            }
            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
            items(rows, key = { it.pkg }) { row ->
                val isSel = row.pkg in selected
                val toggle: (Boolean) -> Unit = { on ->
                    if (on && !row.hasConfig) onSetup(listOf(row.pkg)) else vm.toggle(row.pkg, on)
                }
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .combinedClickable(
                            onClick = {
                                if (selecting) { if (isSel) selected.remove(row.pkg) else selected.add(row.pkg) }
                                else toggle(!row.locked)
                            },
                            onLongClick = { if (!isSel) selected.add(row.pkg) }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(row.pkg, 44.dp, vm::icon)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            row.lockType?.let { "${it.emoji} ${it.title}" } ?: "Not locked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = row.locked, onCheckedChange = toggle)
                }
            }
        }
    }
}
