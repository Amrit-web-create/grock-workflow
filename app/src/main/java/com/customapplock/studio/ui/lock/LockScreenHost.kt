package com.customapplock.studio.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.customapplock.studio.data.AppLockEntity
import com.customapplock.studio.domain.LockType
import com.customapplock.studio.domain.MASTER_PKG
import com.customapplock.studio.ui.common.AppIcon
import com.customapplock.studio.ui.common.BannerAd
import com.customapplock.studio.ui.common.findFragmentActivity
import com.customapplock.studio.ui.common.rememberHaptics
import com.customapplock.studio.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The shared lock screen. Used by LockActivity (with the banner ad) and as the in-app gate that
 * protects App Lock Studio itself (no ad inside the main app).
 */
@Composable
fun LockScreenHost(
    pkg: String,
    showAd: Boolean,
    onUnlocked: () -> Unit,
    vm: GateViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findFragmentActivity() }
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val settings by vm.settings.state.collectAsState()

    var entity by remember(pkg) { mutableStateOf<AppLockEntity?>(null) }
    LaunchedEffect(pkg) {
        val e = vm.entity(pkg)
        if (e == null) onUnlocked() else entity = e
    }

    val label = remember(pkg) { vm.label(pkg) }
    var resetKey by remember { mutableIntStateOf(0) }
    var failed by remember { mutableIntStateOf(0) }
    var message by remember(pkg) { mutableStateOf<String?>(null) }
    var showForgot by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    val bioAvailable = remember { Biometrics.available(ctx) }
    val canBio = settings.biometric && bioAvailable && activity != null
    LaunchedEffect(entity, canBio) {
        if (entity != null && canBio) Biometrics.prompt(activity!!, "Unlock $label") { onUnlocked() }
    }

    val handle: (String) -> Unit = { secret ->
        val e = entity
        if (e != null) scope.launch {
            if (vm.verify(e, secret)) {
                haptics.success()
                onUnlocked()
            } else {
                haptics.error()
                failed++
                message = "Wrong passcode. Try again."
                vm.onWrong(pkg, failed, activity)
                repeat(3) {
                    shake.animateTo(if (it % 2 == 0) 16f else -16f, tween(50))
                }
                shake.animateTo(0f, tween(50))
                delay(250)
                resetKey++
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Palette.Sapphire, Color(0xFF111111), Palette.Sapphire))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            AppIcon(pkg, 56.dp, vm::icon)
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            val type = entity?.let { LockType.fromId(it.lockTypeId) }
            Text(
                message ?: type?.hint.orEmpty(),
                color = if (message != null) Palette.Rose else Color.White.copy(alpha = .6f),
                fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Box(
                Modifier.weight(1f).fillMaxWidth().offset { IntOffset(shake.value.roundToInt(), 0) }
            ) {
                if (type != null) LockInput(type, resetKey, handle)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showForgot = true }) { Text("Forgot passcode?", color = Palette.Cyan) }
                if (canBio) {
                    TextButton(onClick = { Biometrics.prompt(activity!!, "Unlock $label") { onUnlocked() } }) {
                        Text("Use biometrics", color = Palette.Cyan)
                    }
                }
            }
            if (showAd && pkg != MASTER_PKG) BannerAd()
        }
    }

    if (showForgot) {
        var answer by remember { mutableStateOf("") }
        var wrong by remember { mutableStateOf(false) }
        val hasQuestion = settings.securityQuestion.isNotBlank()
        AlertDialog(
            onDismissRequest = { showForgot = false },
            title = { Text("Recover access") },
            text = {
                Column {
                    if (!hasQuestion) {
                        Text("No recovery question has been set. You can set one in Settings > Security.")
                    } else {
                        Text(settings.securityQuestion, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = answer, onValueChange = { answer = it; wrong = false },
                            singleLine = true, isError = wrong, label = { Text("Your answer") }
                        )
                        if (wrong) Text("That answer doesn't match.", color = Palette.Rose, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                if (hasQuestion) {
                    TextButton(onClick = {
                        scope.launch {
                            if (vm.checkAnswer(answer)) { showForgot = false; onUnlocked() } else wrong = true
                        }
                    }) { Text("Verify") }
                }
            },
            dismissButton = { TextButton(onClick = { showForgot = false }) { Text("Close") } }
        )
    }
}
