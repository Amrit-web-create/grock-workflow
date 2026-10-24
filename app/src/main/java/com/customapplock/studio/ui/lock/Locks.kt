package com.customapplock.studio.ui.lock

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customapplock.studio.domain.LockType
import com.customapplock.studio.ui.common.rememberHaptics
import com.customapplock.studio.ui.theme.Palette
import com.customapplock.studio.util.evalExpression
import com.customapplock.studio.util.formatResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Every lock composable collects an input and reports it once as a secret string via onComplete.
 * The host decides whether it is a setup, a confirmation, a practice run or a real unlock.
 */
@Composable
fun LockInput(type: LockType, resetKey: Int, onComplete: (String) -> Unit, modifier: Modifier = Modifier) {
    val cb by rememberUpdatedState(onComplete)
    key(type, resetKey) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val f: (String) -> Unit = { cb(it) }
            when (type) {
                LockType.JIGSAW -> JigsawLock(f)
                LockType.NUMBER_PATTERN -> NumberPatternLock(f)
                LockType.SCROLL_WHEEL -> ScrollWheelLock(f)
                LockType.COLOR_SEQUENCE -> ColorSequenceLock(f)
                LockType.SHAPE_MATCH -> ShapeMatchLock(f)
                LockType.CLOCK_HAND -> ClockHandLock(f)
                LockType.EMOJI_MATRIX -> EmojiMatrixLock(f)
                LockType.CALCULATOR -> CalculatorLock(f)
                LockType.BUBBLE_POP -> BubblePopLock(f)
                LockType.SWIPE -> SwipeLock(f)
            }
        }
    }
}

private fun textPaint(color: Int, size: Float) = Paint().apply {
    isAntiAlias = true
    this.color = color
    textSize = size
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
}

private fun DrawScope.text(s: String, x: Float, y: Float, paint: Paint) {
    drawContext.canvas.nativeCanvas.drawText(s, x, y - (paint.ascent() + paint.descent()) / 2f, paint)
}

private val White = Color.White

// ───────────────────────── 1. Jigsaw ─────────────────────────
@Composable
fun JigsawLock(onComplete: (String) -> Unit) {
    val h = rememberHaptics()
    val scope = rememberCoroutineScope()
    val picks = remember { mutableStateListOf<Int>() }
    val cols = 4
    val rows = 3
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        val dn = LocalDensity.current
        val availW = with(dn) { maxWidth.toPx() }
        val availH = with(dn) { maxHeight.toPx() } - 60f
        val cell = min(availW / cols, availH / (rows + 2.4f))
        val boardW = cell * cols
        val totalH = cell * (rows + 2.2f)
        val tray = Offset(boardW / 2f, cell * rows + cell * 1.3f)
        var piece by remember(boardW) { mutableStateOf(tray) }
        var grabbing by remember { mutableStateOf(false) }
        var busy by remember { mutableStateOf(false) }
        val white = remember { textPaint(android.graphics.Color.WHITE, 44f) }
        val dark = remember { textPaint(0xFF0F172A.toInt(), 52f) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Drop the piece in 3 secret spots  •  ${picks.size}/3", color = White.copy(alpha = .7f), fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Canvas(
                Modifier
                    .size(with(dn) { boardW.toDp() }, with(dn) { totalH.toDp() })
                    .pointerInput(boardW) {
                        detectDragGestures(
                            onDragStart = { o -> grabbing = !busy && (o - piece).getDistance() < cell * 0.8f },
                            onDrag = { change, amount ->
                                if (grabbing) { change.consume(); piece += amount }
                            },
                            onDragEnd = {
                                if (grabbing) {
                                    grabbing = false
                                    val c = (piece.x / cell).toInt()
                                    val r = (piece.y / cell).toInt()
                                    val inside = piece.x >= 0f && piece.y >= 0f && c in 0 until cols && r in 0 until rows
                                    val start = piece
                                    busy = true
                                    scope.launch {
                                        if (inside) {
                                            val target = Offset((c + .5f) * cell, (r + .5f) * cell)
                                            animate(0f, 1f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f)) { v, _ ->
                                                piece = lerp(start, target, v)
                                            }
                                            h.snap()
                                            picks.add(r * cols + c)
                                            delay(220)
                                            if (picks.size >= 3) {
                                                onComplete(picks.joinToString("-"))
                                                return@launch
                                            }
                                        }
                                        val s2 = piece
                                        animate(0f, 1f, animationSpec = tween(250)) { v, _ -> piece = lerp(s2, tray, v) }
                                        busy = false
                                    }
                                }
                            },
                            onDragCancel = { grabbing = false; piece = tray }
                        )
                    }
            ) {
                drawRoundRect(Palette.SapphireLight, size = Size(boardW, cell * rows), cornerRadius = CornerRadius(28f))
                for (r in 0 until rows) for (c in 0 until cols) {
                    val i = r * cols + c
                    val g = 0.16f + 0.07f * ((r * 2 + c * 3) % 5)
                    val col = Color(g, g, g)
                    drawRoundRect(
                        col, topLeft = Offset(c * cell + 4f, r * cell + 4f),
                        size = Size(cell - 8f, cell - 8f), cornerRadius = CornerRadius(18f)
                    )
                    val n = picks.indexOf(i)
                    if (n >= 0) text("${n + 1}", (c + .5f) * cell, (r + .5f) * cell, white)
                }
                val s = cell * 0.82f
                drawCircle(Palette.Cyan.copy(alpha = .25f), s * 0.75f, piece)
                drawRoundRect(Palette.Cyan, piece - Offset(s / 2, s / 2), Size(s, s), CornerRadius(18f))
                drawCircle(Palette.Cyan, s * 0.13f, piece + Offset(0f, -s / 2))
                drawCircle(Palette.Cyan, s * 0.13f, piece + Offset(s / 2, 0f))
                text("?", piece.x, piece.y, dark)
            }
        }
    }
}

// ───────────────────────── 2. Numbered pattern ─────────────────────────
/** Classic 3x3 dots, numbers 1-9 written inside every dot, breathing halos, ripples and a glowing trail. */
@Composable
fun NumberPatternLock(onComplete: (String) -> Unit) {
    val h = rememberHaptics()
    val scope = rememberCoroutineScope()
    val picked = remember { mutableStateListOf<Int>() } // node index 0..8, its number is index + 1
    var finger by remember { mutableStateOf<Offset?>(null) }
    var hint by remember { mutableStateOf("Connect at least 4 dots") }
    val intro = remember { List(9) { Animatable(0f) } }
    val sel = remember { List(9) { Animatable(0f) } }
    val ripple = remember { List(9) { Animatable(1f) } }
    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b"
    )
    LaunchedEffect(Unit) {
        intro.forEachIndexed { i, a -> launch { delay(i * 55L); a.animateTo(1f, spring(0.55f, 140f)) } }
    }
    val light = remember { textPaint(android.graphics.Color.WHITE, 40f) }
    val darkP = remember { textPaint(android.graphics.Color.BLACK, 40f) }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth - 24.dp, maxHeight - 56.dp, 380.dp).coerceAtLeast(200.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(hint, color = White.copy(alpha = .6f), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Canvas(
                Modifier.size(side).pointerInput(Unit) {
                    val w = size.width.toFloat()
                    fun center(i: Int) = Offset((i % 3 + .5f) * w / 3f, (i / 3 + .5f) * w / 3f)
                    fun ready() = intro.all { it.value >= 0.9f }
                    fun hit(p: Offset) {
                        val r = w / 10f * 1.7f
                        for (i in 0 until 9) if (i !in picked && (p - center(i)).getDistance() < r) {
                            picked.add(i); h.tick()
                            scope.launch { sel[i].animateTo(1f, spring(0.45f, 420f)) }
                            scope.launch { ripple[i].snapTo(0f); ripple[i].animateTo(1f, tween(650)) }
                            break
                        }
                    }
                    fun finish() {
                        finger = null
                        if (picked.size >= 4) {
                            onComplete(picked.joinToString("-") { (it + 1).toString() })
                        } else {
                            if (picked.isNotEmpty()) hint = "Too short, connect at least 4"
                            picked.clear()
                            sel.forEach { a -> scope.launch { a.animateTo(0f, tween(200)) } }
                        }
                    }
                    detectDragGestures(
                        onDragStart = { if (ready()) hit(it) },
                        onDrag = { c, _ -> c.consume(); finger = c.position; if (ready()) hit(c.position) },
                        onDragEnd = { finish() },
                        onDragCancel = { finish() }
                    )
                }
            ) {
                val w = size.width
                fun center(i: Int) = Offset((i % 3 + .5f) * w / 3f, (i / 3 + .5f) * w / 3f)
                val r = w / 10f
                for (k in 1 until picked.size) {
                    drawLine(White.copy(alpha = .10f), center(picked[k - 1]), center(picked[k]), 40f, StrokeCap.Round)
                    drawLine(White.copy(alpha = .95f), center(picked[k - 1]), center(picked[k]), 7f, StrokeCap.Round)
                }
                val f = finger
                if (f != null && picked.isNotEmpty()) {
                    drawLine(White.copy(alpha = .45f), center(picked.last()), f, 5f, StrokeCap.Round)
                    drawCircle(White.copy(alpha = .08f), 48f, f)
                    drawCircle(White.copy(alpha = .22f), 16f, f)
                }
                for (i in 0 until 9) {
                    val s = intro[i].value
                    val q = sel[i].value
                    val p = center(i)
                    val pulse = if (i % 2 == 0) breathe else 1f - breathe
                    drawCircle(White.copy(alpha = (0.05f + 0.06f * pulse) * (1f - q)), r * (1.5f + 0.18f * pulse) * s, p)
                    drawCircle(Palette.SapphireLight, r * s * (1f + 0.12f * q), p)
                    drawCircle(White.copy(alpha = 0.35f + 0.65f * q), r * s * (1f + 0.12f * q), p, style = Stroke(3.5f))
                    if (q > 0f) drawCircle(White.copy(alpha = q), r * s * (1f + 0.12f * q) * 0.96f, p)
                    val paint = if (q > 0.5f) darkP else light
                    paint.textSize = r * 0.95f
                    text((i + 1).toString(), p.x, p.y, paint)
                    val rp = ripple[i].value
                    if (rp < 1f) drawCircle(White.copy(alpha = (1f - rp) * .6f), r * (1f + rp * 1.7f), p, style = Stroke(4f))
                }
            }
        }
    }
}

// ───────────────────────── 3. Scroll wheel ─────────────────────────
private val ITEM_H = 56.dp

@Composable
fun ScrollWheelLock(onComplete: (String) -> Unit) {
    val digits = remember { mutableStateListOf(0, 0, 0, 0) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.width(300.dp).height(ITEM_H)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.Cyan.copy(alpha = .12f))
                    .border(1.5.dp, Palette.Cyan.copy(alpha = .6f), RoundedCornerShape(16.dp))
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in 0 until 4) Wheel { d -> digits[i] = d }
            }
        }
        Button(
            onClick = { onComplete(digits.joinToString("")) },
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Cyan, contentColor = Palette.Sapphire),
            modifier = Modifier.width(220.dp).height(52.dp)
        ) { Text("Confirm", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
    }
}

@Composable
private fun Wheel(onDigit: (Int) -> Unit) {
    val h = rememberHaptics()
    val dn = LocalDensity.current
    val itemPx = with(dn) { ITEM_H.toPx() }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = 4999)
    LaunchedEffect(state) {
        snapshotFlow {
            state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
        }.distinctUntilChanged().drop(1).collect { h.tick(); onDigit((it + 1) % 10) }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.filter { !it }.collect {
            val off = state.firstVisibleItemScrollOffset
            if (off != 0) {
                state.animateScrollToItem(state.firstVisibleItemIndex + if (off > itemPx / 2) 1 else 0)
            }
        }
    }
    LazyColumn(state = state, modifier = Modifier.width(66.dp).height(ITEM_H * 3)) {
        items(10000) { idx ->
            Box(
                Modifier.fillMaxWidth().height(ITEM_H).graphicsLayer {
                    val c = state.firstVisibleItemIndex + 1 + state.firstVisibleItemScrollOffset / itemPx
                    val d = idx - c
                    val a = abs(d).coerceAtMost(1.6f)
                    rotationX = -d * 38f
                    scaleX = 1f - 0.18f * a
                    scaleY = scaleX
                    alpha = 1f - 0.4f * a
                    cameraDistance = 14f * density
                },
                contentAlignment = Alignment.Center
            ) { Text("${idx % 10}", fontSize = 34.sp, fontWeight = FontWeight.Black, color = White) }
        }
    }
}

// ───────────────────────── 4. Color sequence ─────────────────────────
private val orbColors = listOf(Color(0xFF22D3EE), Color(0xFFA78BFA), Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFFFB7185), Color(0xFF60A5FA))

@Composable
fun ColorSequenceLock(onComplete: (String) -> Unit) {
    val h = rememberHaptics()
    val seq = remember { mutableStateListOf<Int>() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(26.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in 0 until 4) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape)
                        .background(if (i < seq.size) orbColors[seq[i]] else White.copy(alpha = .15f))
                )
            }
        }
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    Orb(orbColors[idx]) {
                        if (seq.size < 4) {
                            seq.add(idx); h.tick()
                            if (seq.size == 4) onComplete(seq.joinToString("-"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Orb(color: Color, onTap: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "p").animateFloat(
        0.85f, 1.05f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pp"
    )
    val ripple = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Canvas(
        Modifier.size(92.dp).pointerInput(Unit) {
            detectTapGestures {
                onTap()
                scope.launch { ripple.snapTo(0f); ripple.animateTo(1f, tween(550)) }
            }
        }
    ) {
        val r = size.minDimension / 2 * 0.62f
        drawCircle(color.copy(alpha = .22f), r * 1.35f * pulse)
        drawCircle(
            Brush.radialGradient(listOf(color, color.copy(alpha = .7f)), center = center - Offset(r * .3f, r * .3f), radius = r * 1.4f),
            r
        )
        drawCircle(White.copy(alpha = .35f), r * .22f, center - Offset(r * .4f, r * .4f))
        if (ripple.value < 1f) {
            drawCircle(color.copy(alpha = 1f - ripple.value), r * (1f + ripple.value * 0.7f), style = Stroke(6f))
        }
    }
}

// ───────────────────────── 5. Shape match ─────────────────────────
private val shapeColors = listOf(Palette.Cyan, Palette.Purple, Palette.Emerald, Palette.Amber)

private fun DrawScope.drawShape(kind: Int, c: Offset, r: Float, color: Color) {
    when (kind) {
        0 -> drawCircle(color, r, c)
        1 -> drawRoundRect(color, c - Offset(r, r), Size(2 * r, 2 * r), CornerRadius(r * .25f))
        2 -> {
            val p = Path().apply {
                moveTo(c.x, c.y - r); lineTo(c.x + r, c.y + r * .85f); lineTo(c.x - r, c.y + r * .85f); close()
            }
            drawPath(p, color)
        }
        else -> {
            val p = Path()
            for (k in 0 until 10) {
                val ang = -PI / 2 + k * PI / 5
                val rad = if (k % 2 == 0) r else r * 0.45f
                val x = c.x + (rad * cos(ang)).toFloat()
                val y = c.y + (rad * sin(ang)).toFloat()
                if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close()
            drawPath(p, color)
        }
    }
}

@Composable
fun ShapeMatchLock(onComplete: (String) -> Unit) {
    val h = rememberHaptics()
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        val dn = LocalDensity.current
        val hDp = minOf(maxHeight, 420.dp)
        val w = with(dn) { maxWidth.toPx() }
        val hh = with(dn) { hDp.toPx() }
        val slotR = w / 4f * 0.42f
        fun slotC(i: Int) = Offset((i + .5f) * w / 4f, hh * 0.25f)
        fun trayC(i: Int) = Offset((i + .5f) * w / 4f, hh * 0.78f)
        val pos = remember(w) { mutableStateListOf(trayC(0), trayC(1), trayC(2), trayC(3)) }
        val slotOf = remember { mutableStateListOf(-1, -1, -1, -1) }
        var drag by remember { mutableIntStateOf(-1) }
        var hover by remember { mutableIntStateOf(-1) }
        val paint = remember { textPaint(0x66FFFFFF, 44f) }
        Canvas(
            Modifier.size(maxWidth, hDp).pointerInput(w) {
                detectDragGestures(
                    onDragStart = { o ->
                        drag = (0 until 4).filter { (o - pos[it]).getDistance() < slotR * 1.1f }
                            .minByOrNull { (o - pos[it]).getDistance() } ?: -1
                        if (drag >= 0) slotOf[drag] = -1
                    },
                    onDrag = { c, a ->
                        if (drag >= 0) {
                            c.consume()
                            pos[drag] = pos[drag] + a
                            hover = (0 until 4).firstOrNull {
                                (pos[drag] - slotC(it)).getDistance() < slotR * 1.2f && slotOf.indexOf(it) == -1
                            } ?: -1
                        }
                    },
                    onDragEnd = {
                        val d = drag
                        if (d >= 0) {
                            val s = hover
                            drag = -1; hover = -1
                            val from = pos[d]
                            if (s >= 0) {
                                slotOf[d] = s
                                val to = slotC(s)
                                scope.launch {
                                    animate(0f, 1f, animationSpec = spring(0.6f, 600f)) { v, _ -> pos[d] = lerp(from, to, v) }
                                    h.snap()
                                    if (slotOf.all { it >= 0 }) onComplete(slotOf.joinToString("-"))
                                }
                            } else {
                                val to = trayC(d)
                                scope.launch { animate(0f, 1f, animationSpec = tween(250)) { v, _ -> pos[d] = lerp(from, to, v) } }
                            }
                        }
                    },
                    onDragCancel = { drag = -1; hover = -1 }
                )
            }
        ) {
            for (i in 0 until 4) {
                val c = slotC(i)
                drawRoundRect(
                    if (hover == i) Palette.Cyan else White.copy(alpha = .25f),
                    c - Offset(slotR, slotR), Size(slotR * 2, slotR * 2), CornerRadius(22f), style = Stroke(4f)
                )
                if (slotOf.indexOf(i) == -1) text("${i + 1}", c.x, c.y, paint)
            }
            for (k in 0 until 4) drawShape(k, pos[k], slotR * 0.7f, shapeColors[k])
        }
    }
}

// ───────────────────────── 6. Clock hands ─────────────────────────
@Composable
fun ClockHandLock(onComplete: (String) -> Unit) {
    val h = rememberHaptics()
    var hour by remember { mutableIntStateOf(0) }
    var minute by remember { mutableIntStateOf(0) }
    var active by remember { mutableIntStateOf(0) }
    val paint = remember { textPaint(android.graphics.Color.WHITE, 38f) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "%d:%02d".format(if (hour == 0) 12 else hour, minute),
            fontSize = 34.sp, fontWeight = FontWeight.Black, color = White
        )
        Canvas(
            Modifier.size(270.dp)
                .pointerInput(Unit) {
                    fun upd(p: Offset) {
                        val d = p - Offset(size.width / 2f, size.height / 2f)
                        var ang = Math.toDegrees(atan2(d.x.toDouble(), -d.y.toDouble())).toFloat()
                        if (ang < 0) ang += 360f
                        if (active == 0) {
                            val nh = (ang / 30f).roundToInt() % 12
                            if (nh != hour) { hour = nh; h.tick() }
                        } else {
                            val nm = ((ang / 30f).roundToInt() * 5) % 60
                            if (nm != minute) { minute = nm; h.tick() }
                        }
                    }
                    detectTapGestures { upd(it) }
                }
                .pointerInput(Unit) {
                    fun upd(p: Offset) {
                        val d = p - Offset(size.width / 2f, size.height / 2f)
                        var ang = Math.toDegrees(atan2(d.x.toDouble(), -d.y.toDouble())).toFloat()
                        if (ang < 0) ang += 360f
                        if (active == 0) {
                            val nh = (ang / 30f).roundToInt() % 12
                            if (nh != hour) { hour = nh; h.tick() }
                        } else {
                            val nm = ((ang / 30f).roundToInt() * 5) % 60
                            if (nm != minute) { minute = nm; h.tick() }
                        }
                    }
                    detectDragGestures(onDragStart = { upd(it) }, onDrag = { c, _ -> c.consume(); upd(c.position) })
                }
        ) {
            val c = center
            val r = size.minDimension / 2
            drawCircle(White.copy(alpha = .06f), r)
            drawCircle(Palette.Cyan.copy(alpha = .5f), r, style = Stroke(4f))
            for (n in 1..12) {
                val a = Math.toRadians(n * 30.0 - 90.0)
                text(n.toString(), c.x + (r * .78f * cos(a)).toFloat(), c.y + (r * .78f * sin(a)).toFloat(), paint)
            }
            fun tip(deg: Double, len: Float): Offset {
                val a = Math.toRadians(deg - 90.0)
                return Offset(c.x + (len * cos(a)).toFloat(), c.y + (len * sin(a)).toFloat())
            }
            val hTip = tip(hour * 30.0, r * .5f)
            val mTip = tip(minute * 6.0, r * .72f)
            drawLine(Palette.Cyan, c, hTip, 16f, StrokeCap.Round)
            drawLine(Palette.Purple, c, mTip, 10f, StrokeCap.Round)
            drawCircle((if (active == 0) Palette.Cyan else Palette.Purple).copy(alpha = .35f), 26f, if (active == 0) hTip else mTip)
            drawCircle(White, 10f, c)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = active == 0, onClick = { active = 0 }, label = { Text("Hour") })
            FilterChip(selected = active == 1, onClick = { active = 1 }, label = { Text("Minute") })
        }
        Button(
            onClick = { onComplete("$hour:$minute") },
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Cyan, contentColor = Palette.Sapphire),
            modifier = Modifier.width(220.dp).height(48.dp)
        ) { Text("Confirm", fontWeight = FontWeight.ExtraBold) }
    }
}

// ───────────────────────── 7. Emoji matrix ─────────────────────────
private val emojis = listOf("😀", "😎", "🤖", "👻", "🦄", "🐱", "🍕", "🚀", "🎈")

@Composable
fun EmojiMatrixLock(onComplete: (String) -> Unit) {
    val h = rememberHaptics()
    val seq = remember { mutableStateListOf<Int>() }
    val inf = rememberInfiniteTransition(label = "emoji")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("${seq.size}/4", color = White.copy(alpha = .7f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        for (r in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (c in 0 until 3) {
                    val i = r * 3 + c
                    val s by inf.animateFloat(
                        0.92f, 1.08f,
                        infiniteRepeatable(tween(900 + i * 60, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(i * 110)),
                        label = "s$i"
                    )
                    val used = seq.withIndex().filter { it.value == i }.joinToString(",") { (it.index + 1).toString() }
                    Box(
                        Modifier.size(86.dp).scale(s).clip(RoundedCornerShape(24.dp))
                            .background(if (used.isNotEmpty()) Palette.Cyan.copy(alpha = .28f) else White.copy(alpha = .08f))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                if (seq.size < 4) {
                                    seq.add(i); h.tick()
                                    if (seq.size == 4) onComplete(seq.joinToString("-"))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emojis[i], fontSize = 40.sp)
                        if (used.isNotEmpty()) {
                            Text(
                                used, color = Palette.Cyan, fontWeight = FontWeight.Black, fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── 8. Calculator ─────────────────────────
@Composable
fun CalculatorLock(onComplete: (String) -> Unit) {
    val h = rememberHaptics()
    var expr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val keys = listOf(
        listOf("C", "(", ")", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"), listOf("0", ".", "⌫", "=")
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.fillMaxWidth().weight(0.75f).clip(RoundedCornerShape(24.dp))
                .background(White.copy(alpha = .06f)).padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                if (error) "Error" else expr.ifEmpty { "0" },
                fontSize = 36.sp, fontWeight = FontWeight.Black, maxLines = 1,
                color = if (error) Palette.Rose else White, textAlign = TextAlign.End
            )
        }
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { k ->
                    val isOp = k in listOf("÷", "×", "−", "+")
                    val bg = when {
                        k == "=" -> Palette.Emerald
                        isOp -> Palette.Cyan
                        k == "C" || k == "⌫" -> Palette.Purple.copy(alpha = .55f)
                        else -> White.copy(alpha = .1f)
                    }
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(bg).clickable {
                            h.tick(); error = false
                            when (k) {
                                "C" -> expr = ""
                                "⌫" -> expr = expr.dropLast(1)
                                "=" -> {
                                    val r = evalExpression(expr)
                                    if (r == null) { error = true; expr = "" } else onComplete(formatResult(r))
                                }
                                else -> if (expr.length < 24) expr += k
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            k, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (isOp || k == "=") Palette.Sapphire else White
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────── 9. Bubble pop ─────────────────────────
private val bubbleColors = listOf(Palette.Cyan, Palette.Purple, Palette.Emerald, Palette.Amber)

@Composable
fun BubblePopLock(onComplete: (String) -> Unit) {
    val h = rememberHaptics()
    val taps = remember { mutableStateListOf<Int>() }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until 4) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(if (i < taps.size) bubbleColors[taps[i] % 4].copy(alpha = .8f) else White.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) { if (i < taps.size) Text("${taps[i]}", color = Palette.Sapphire, fontWeight = FontWeight.Black) }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val dn = LocalDensity.current
            val w = with(dn) { maxWidth.toPx() }
            val hg = with(dn) { maxHeight.toPx() }
            val n = 8
            val r = (min(w, hg) / 8f).coerceIn(30f, 70f)
            val xs = remember(w, hg) { FloatArray(n) { r + Random.nextFloat() * (w - 2 * r) } }
            val ys = remember(w, hg) { FloatArray(n) { r + Random.nextFloat() * (hg - 2 * r) } }
            val vx = remember(w, hg) { FloatArray(n) { (120f + Random.nextFloat() * 140f) * if (Random.nextBoolean()) 1 else -1 } }
            val vy = remember(w, hg) { FloatArray(n) { (120f + Random.nextFloat() * 140f) * if (Random.nextBoolean()) 1 else -1 } }
            val pop = remember(w, hg) { FloatArray(n) }
            var tick by remember { mutableIntStateOf(0) }
            val paint = remember { textPaint(0xFF0F172A.toInt(), 44f) }
            LaunchedEffect(w, hg) {
                var last = 0L
                while (true) {
                    androidx.compose.runtime.withFrameNanos { t ->
                        val dt = if (last == 0L) 0.016f else ((t - last) / 1e9f).coerceAtMost(0.05f)
                        last = t
                        for (i in 0 until n) {
                            xs[i] += vx[i] * dt; ys[i] += vy[i] * dt
                            if (xs[i] < r) { xs[i] = r; vx[i] = abs(vx[i]) }
                            if (xs[i] > w - r) { xs[i] = w - r; vx[i] = -abs(vx[i]) }
                            if (ys[i] < r) { ys[i] = r; vy[i] = abs(vy[i]) }
                            if (ys[i] > hg - r) { ys[i] = hg - r; vy[i] = -abs(vy[i]) }
                            if (pop[i] > 0f) pop[i] = (pop[i] - dt * 3f).coerceAtLeast(0f)
                        }
                        tick++
                    }
                }
            }
            Canvas(
                Modifier.fillMaxSize().pointerInput(w, hg) {
                    detectTapGestures { o ->
                        var best = -1
                        var bd = Float.MAX_VALUE
                        for (i in 0 until n) {
                            val d = hypot(o.x - xs[i], o.y - ys[i])
                            if (d <= r * 1.2f && d < bd) { bd = d; best = i }
                        }
                        if (best >= 0 && taps.size < 4) {
                            taps.add(best + 1); pop[best] = 1f; h.tick()
                            if (taps.size == 4) onComplete(taps.joinToString("-"))
                        }
                    }
                }
            ) {
                @Suppress("UNUSED_VARIABLE") val t = tick
                for (i in 0 until n) {
                    val c = Offset(xs[i], ys[i])
                    val col = bubbleColors[i % 4]
                    drawCircle(Brush.radialGradient(listOf(col.copy(alpha = .95f), col.copy(alpha = .55f)), center = c - Offset(r * .3f, r * .3f), radius = r * 1.3f), r, c)
                    drawCircle(White.copy(alpha = .4f), r * .2f, c - Offset(r * .42f, r * .42f))
                    text((i + 1).toString(), c.x, c.y, paint)
                    if (pop[i] > 0f) drawCircle(col.copy(alpha = pop[i]), r * (1f + (1f - pop[i]) * .8f), c, style = Stroke(5f))
                }
            }
        }
    }
}

// ───────────────────────── 10. Directional swipe ─────────────────────────
@Composable
fun SwipeLock(onComplete: (String) -> Unit) {
    val h = rememberHaptics()
    val dirs = remember { mutableStateListOf<String>() }
    val trail = remember { mutableStateListOf<Offset>() }
    fun arrow(d: String) = when (d) { "U" -> "↑"; "D" -> "↓"; "L" -> "←"; else -> "→" }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in 0 until 4) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (i < dirs.size) Palette.Cyan.copy(alpha = .25f) else White.copy(alpha = .08f)),
                    contentAlignment = Alignment.Center
                ) { if (i < dirs.size) Text(arrow(dirs[i]), color = White, fontSize = 24.sp, fontWeight = FontWeight.Black) }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    var start = Offset.Zero
                    var end = Offset.Zero
                    detectDragGestures(
                        onDragStart = { start = it; end = it; trail.clear(); trail.add(it) },
                        onDrag = { c, _ -> c.consume(); end = c.position; trail.add(c.position) },
                        onDragEnd = {
                            val d = end - start
                            if (d.getDistance() > 90f && dirs.size < 4) {
                                val dir = if (abs(d.x) > abs(d.y)) (if (d.x > 0) "R" else "L") else (if (d.y > 0) "D" else "U")
                                dirs.add(dir); h.tick()
                                if (dirs.size == 4) onComplete(dirs.joinToString("-"))
                            }
                            trail.clear()
                        },
                        onDragCancel = { trail.clear() }
                    )
                }
            ) {
                drawCircle(White.copy(alpha = .05f), size.minDimension * .3f)
                if (trail.size > 1) {
                    val p = Path()
                    p.moveTo(trail[0].x, trail[0].y)
                    for (i in 1 until trail.size) p.lineTo(trail[i].x, trail[i].y)
                    drawPath(p, Palette.Cyan, style = Stroke(14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            if (trail.isEmpty()) Text("Swipe anywhere", color = White.copy(alpha = .4f), fontSize = 16.sp)
        }
    }
}
