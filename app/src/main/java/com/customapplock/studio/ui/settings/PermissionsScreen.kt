package com.customapplock.studio.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.customapplock.studio.util.Permissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(onDone: () -> Unit, onBack: (() -> Unit)?) {
    val ctx = LocalContext.current
    var st by remember { mutableStateOf(Permissions.check(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { st = Permissions.check(ctx) }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { st = Permissions.check(ctx) }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { st = Permissions.check(ctx) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                Button(onClick = onDone, enabled = st.required, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Two permissions are required so the lock can appear instantly. Everything stays on your device.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PermCard(
                "♿", "Accessibility service",
                "Detects which app just opened so the lock can appear. It never reads screen content. If the switch is greyed out: App info > ⋮ > Allow restricted settings.",
                st.accessibility, true
            ) { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            PermCard(
                "🪟", "Draw over other apps", "Lets the lock screen open instantly on top of a protected app.",
                st.overlay, true
            ) {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            PermCard("🔔", "Notifications", "Keeps the protection service running reliably.", st.notifications, false) {
                if (Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            PermCard("📷", "Camera", "Optional. Only for the intruder selfie feature.", st.camera, false) {
                cam.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

@Composable
private fun PermCard(emoji: String, title: String, desc: String, granted: Boolean, required: Boolean, onGrant: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (granted) "Granted" else if (required) "Required" else "Optional",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (granted) Text("✓", fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            if (!granted) {
                OutlinedButton(onClick = onGrant, modifier = Modifier.padding(top = 10.dp)) { Text("Grant") }
            }
        }
    }
}
