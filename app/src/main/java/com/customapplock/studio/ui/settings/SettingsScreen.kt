package com.customapplock.studio.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customapplock.studio.data.AppSettings
import com.customapplock.studio.data.LockRepository
import com.customapplock.studio.data.SecretCodec
import com.customapplock.studio.data.SettingsStore
import com.customapplock.studio.domain.MASTER_PKG
import com.customapplock.studio.domain.RelockMode
import com.customapplock.studio.service.GuardService
import com.customapplock.studio.ui.common.SettingItem
import com.customapplock.studio.ui.common.SettingsGroup
import com.customapplock.studio.ui.lock.Biometrics
import com.customapplock.studio.util.Permissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val repo: LockRepository
) : ViewModel() {
    val settings: StateFlow<AppSettings> = store.state
    val hasMaster: StateFlow<Boolean> = repo.all.map { l -> l.any { it.packageName == MASTER_PKG } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun update(t: (AppSettings) -> AppSettings) = store.update(t)
    fun removeMaster() { viewModelScope.launch { repo.remove(MASTER_PKG) } }
    fun saveQuestion(q: String, a: String) {
        viewModelScope.launch {
            val hash = withContext(Dispatchers.Default) { SecretCodec.hash(a) }
            store.update { it.copy(securityQuestion = q.trim(), securityAnswerHash = hash) }
        }
    }
}

fun rateApp(ctx: Context) {
    val pkg = ctx.packageName
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onPermissions: () -> Unit,
    onMasterSetup: () -> Unit,
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val hasMaster by vm.hasMaster.collectAsState()
    var dialog by remember { mutableStateOf<String?>(null) }
    val bioOk = remember { Biometrics.available(ctx) }
    val camLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.update { it.copy(intruderSelfie = true) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            SettingsGroup("General") {
                SettingItem(
                    "App Lock",
                    if (s.lockEnabled) "On. Selected apps are protected" else "Off. Nothing is locked",
                    trailing = {
                        Switch(checked = s.lockEnabled, onCheckedChange = { on ->
                            vm.update { it.copy(lockEnabled = on) }
                            if (on) GuardService.start(ctx) else GuardService.stop(ctx)
                        })
                    }
                )
                SettingItem(
                    "Relock timing",
                    if (s.relockMode == RelockMode.GRACE) "${s.relockMode.title} · ${s.graceSeconds}s" else s.relockMode.title,
                    onClick = { dialog = "relock" }
                )
            }
            SettingsGroup("Security") {
                SettingItem(
                    "Biometric unlock",
                    if (bioOk) "Fingerprint or face as a shortcut. The lock design stays as fallback" else "Not available on this device",
                    trailing = {
                        Switch(checked = s.biometric && bioOk, enabled = bioOk, onCheckedChange = { on -> vm.update { it.copy(biometric = on) } })
                    }
                )
                SettingItem(
                    "Lock App Lock Studio",
                    if (hasMaster) "On. Tap to remove" else "Protect this app with its own lock",
                    onClick = { if (hasMaster) dialog = "removeMaster" else onMasterSetup() }
                )
                SettingItem(
                    "Recovery question",
                    if (s.securityQuestion.isBlank()) "Not set. Set one so you never get locked out" else s.securityQuestion,
                    onClick = { dialog = "question" }
                )
            }
            SettingsGroup("Intruder alerts") {
                SettingItem(
                    "Intruder selfie",
                    "Take a silent photo after wrong attempts",
                    trailing = {
                        Switch(checked = s.intruderSelfie, onCheckedChange = { on ->
                            if (!on) vm.update { it.copy(intruderSelfie = false) }
                            else if (Permissions.check(ctx).camera) vm.update { it.copy(intruderSelfie = true) }
                            else camLauncher.launch(Manifest.permission.CAMERA)
                        })
                    }
                )
                SettingItem("Attempts before capture", "${s.intruderAttempts} wrong tries", onClick = { dialog = "attempts" })
            }
            SettingsGroup("More") {
                SettingItem("Permissions", "Accessibility, overlay, notifications, camera", onClick = onPermissions)
                SettingItem("About us", "Version, architecture, credits", onClick = onAbout)
                SettingItem("Rate us", "Leave a rating on Google Play", onClick = { rateApp(ctx) })
                SettingItem("Privacy policy", null, onClick = onPrivacy)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    when (dialog) {
        "relock" -> RelockDialog(s.relockMode, s.graceSeconds, { dialog = null }) { m, g ->
            vm.update { it.copy(relockMode = m, graceSeconds = g) }; dialog = null
        }
        "attempts" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Attempts before capture") },
            text = {
                Column {
                    (2..5).forEach { n ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.update { it.copy(intruderAttempts = n) }; dialog = null }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = s.intruderAttempts == n, onClick = { vm.update { it.copy(intruderAttempts = n) }; dialog = null })
                            Text("$n wrong tries")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Close") } }
        )
        "question" -> QuestionDialog({ dialog = null }) { q, a -> vm.saveQuestion(q, a); dialog = null }
        "removeMaster" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Remove app lock?") },
            text = { Text("App Lock Studio will open without a lock.") },
            confirmButton = { TextButton(onClick = { vm.removeMaster(); dialog = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RelockDialog(mode: RelockMode, grace: Int, onDismiss: () -> Unit, onSave: (RelockMode, Int) -> Unit) {
    var m by remember { mutableStateOf(mode) }
    var g by remember { mutableIntStateOf(grace) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Relock apps") },
        text = {
            Column {
                RelockMode.entries.forEach { opt ->
                    Row(Modifier.fillMaxWidth().clickable { m = opt }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = m == opt, onClick = { m = opt })
                        Column {
                            Text(opt.title)
                            Text(opt.desc, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (m == RelockMode.GRACE) {
                    Text("Grace period: ${g}s", modifier = Modifier.padding(top = 8.dp))
                    Slider(value = g.toFloat(), onValueChange = { g = ((it / 5).roundToInt() * 5).coerceAtLeast(5) }, valueRange = 5f..300f)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(m, g) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun QuestionDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var q by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recovery question") },
        text = {
            Column {
                OutlinedTextField(q, { q = it }, label = { Text("Question (e.g. First pet's name?)") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(a, { a = it }, label = { Text("Answer") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = q.isNotBlank() && a.isNotBlank(), onClick = { onSave(q, a) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
