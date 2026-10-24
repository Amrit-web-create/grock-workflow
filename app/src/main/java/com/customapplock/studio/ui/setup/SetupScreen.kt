package com.customapplock.studio.ui.setup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.customapplock.studio.domain.LockType
import com.customapplock.studio.domain.MASTER_PKG
import com.customapplock.studio.ui.common.rememberHaptics
import com.customapplock.studio.ui.lock.LockInput
import com.customapplock.studio.ui.theme.AppLockTheme
import com.customapplock.studio.ui.theme.Palette
import kotlinx.coroutines.delay

private enum class Step { CHOOSE, INPUT, CONFIRM, PRACTICE, DONE }

/** Input -> Confirm -> 3x mandatory practice -> Activation. */
@Composable
fun SetupScreen(
    pkgs: List<String>,
    presetType: String,
    onClose: () -> Unit,
    vm: SetupViewModel = hiltViewModel()
) {
    AppLockTheme(darkTheme = true) {
        val haptics = rememberHaptics()
        var typeId by rememberSaveable { mutableStateOf(presetType) }
        var step by rememberSaveable { mutableStateOf(if (presetType.isEmpty()) Step.CHOOSE else Step.INPUT) }
        var first by rememberSaveable { mutableStateOf("") }
        var practice by rememberSaveable { mutableStateOf(0) }
        var resetKey by remember { mutableIntStateOf(0) }
        var error by remember { mutableStateOf<String?>(null) }
        val type = LockType.fromId(typeId)
        val isMaster = pkgs.singleOrNull() == MASTER_PKG

        val onSecret: (String) -> Unit = { s ->
            when (step) {
                Step.INPUT -> { first = s; error = null; step = Step.CONFIRM }
                Step.CONFIRM ->
                    if (s == first) { error = null; practice = 0; step = Step.PRACTICE }
                    else { error = "Those didn't match. Start again."; first = ""; resetKey++; step = Step.INPUT }
                Step.PRACTICE ->
                    if (s == first) {
                        practice++
                        haptics.success()
                        error = null
                        if (practice >= 3) step = Step.DONE else resetKey++
                    } else {
                        practice = 0; error = "Wrong. Your streak was reset."; haptics.error(); resetKey++
                    }
                else -> {}
            }
        }

        Surface(color = Palette.Sapphire, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                    Text("Set up lock", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                when (step) {
                    Step.CHOOSE -> ChooseType(pkgs.size, isMaster) { typeId = it.id; step = Step.INPUT }
                    Step.DONE -> DoneView(isMaster, pkgs.size) {
                        LaunchedEffect(Unit) {
                            vm.save(pkgs, type, first)
                            haptics.success()
                            delay(1600)
                            onClose()
                        }
                    }
                    else -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val stepNo = when (step) { Step.INPUT -> 1; Step.CONFIRM -> 2; else -> 3 }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            for (i in 1..3) {
                                Box(
                                    Modifier.size(width = 36.dp, height = 6.dp).clip(CircleShape)
                                        .background(if (i <= stepNo) Palette.Cyan else Color.White.copy(alpha = .15f))
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            when (step) {
                                Step.INPUT -> "Create your ${type.title} passcode"
                                Step.CONFIRM -> "Repeat it to confirm"
                                else -> "Practice unlock ${practice + 1} of 3"
                            },
                            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                        )
                        Text(
                            error ?: if (step == Step.PRACTICE) "Unlock 3 times in a row to activate" else type.hint,
                            color = if (error != null) Palette.Rose else Color.White.copy(alpha = .6f),
                            fontSize = 14.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            LockInput(type, step.ordinal * 1000 + resetKey, onSecret)
                        }
                        if (step == Step.INPUT && presetType.isEmpty()) {
                            TextButton(onClick = { step = Step.CHOOSE; resetKey++ }) {
                                Text("Change lock design", color = Palette.Cyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooseType(count: Int, isMaster: Boolean, onPick: (LockType) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            if (isMaster) "Choose a design to protect App Lock Studio itself"
            else "Choose a lock design for ${if (count == 1) "this app" else "$count apps"}",
            color = Color.White.copy(alpha = .75f), fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(LockType.entries.toList()) { t ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Palette.SapphireLight).clickable { onPick(t) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Palette.Cyan.copy(alpha = .15f)),
                        contentAlignment = Alignment.Center
                    ) { Text(t.emoji, fontSize = 24.sp) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(t.hint, color = Color.White.copy(alpha = .6f), fontSize = 13.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = .5f))
                }
            }
        }
    }
}

@Composable
private fun DoneView(isMaster: Boolean, count: Int, effect: @Composable () -> Unit) {
    effect()
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier.size(120.dp).scale(scale.value).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) { Text("✓", color = Color.Black, fontSize = 64.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(20.dp))
        Text("Locked and loaded!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            if (isMaster) "App Lock Studio is now protected"
            else if (count == 1) "1 app is now protected" else "$count apps are now protected",
            color = Color.White.copy(alpha = .7f), fontSize = 15.sp
        )
    }
}
