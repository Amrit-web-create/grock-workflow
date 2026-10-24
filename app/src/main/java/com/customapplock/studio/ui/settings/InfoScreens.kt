package com.customapplock.studio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customapplock.studio.BuildConfig
import com.customapplock.studio.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("About us", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) { Text("🔒", fontSize = 40.sp) }
            Spacer(Modifier.height(16.dp))
            Text("Custom AppLock Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Block("Architecture", "100% Kotlin. Jetpack Compose + Material 3, MVVM with clean layers (data / domain / ui), Hilt dependency injection, Room database, StateFlow and Coroutines. Passcodes are salted PBKDF2 hashes and settings live in encrypted preferences. Fully offline.")
            Block("How it works", "A lightweight accessibility service watches only for app-switch events and instantly opens your lock design on top of the protected app. No polling, no wake locks, nothing runs while the screen is off.")
            Block("Developer", "Designed and built by Amrit Bharadwaj.")
            Spacer(Modifier.height(16.dp))
            Text(
                "Copyright © Amrit Bharadwaj All Rights Reserved",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Block(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val md = remember { ctx.resources.openRawResource(R.raw.privacy_policy).bufferedReader().use { it.readText() } }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Privacy policy", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(20.dp)) {
            md.lines().forEach { line ->
                when {
                    line.startsWith("# ") -> Text(line.drop(2), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    line.startsWith("## ") -> Text(line.drop(3), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                    line.startsWith("- ") -> Text("•  " + line.drop(2).replace("**", ""), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    line.isBlank() -> Spacer(Modifier.height(6.dp))
                    else -> Text(line.replace("**", ""), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
