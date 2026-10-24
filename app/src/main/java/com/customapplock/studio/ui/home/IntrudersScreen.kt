package com.customapplock.studio.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customapplock.studio.data.AppsRepository
import com.customapplock.studio.data.IntruderEntity
import com.customapplock.studio.data.LockRepository
import com.customapplock.studio.ui.common.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class IntrudersViewModel @Inject constructor(
    private val repo: LockRepository,
    private val apps: AppsRepository
) : ViewModel() {
    val logs: StateFlow<List<IntruderEntity>> =
        repo.intruders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun label(pkg: String) = apps.label(pkg)
    fun delete(e: IntruderEntity) { viewModelScope.launch { repo.deleteIntruder(e) } }
    fun clear() { viewModelScope.launch { repo.clearIntruders() } }
}

private fun loadThumb(path: String): ImageBitmap? {
    val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 }) ?: return null
    val deg = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (deg == 0f) return bmp.asImageBitmap()
    val m = Matrix().apply { postRotate(deg) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true).asImageBitmap()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntrudersScreen(vm: IntrudersViewModel = hiltViewModel()) {
    val logs by vm.logs.collectAsState()
    val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intruders", fontWeight = FontWeight.Bold) },
                actions = { if (logs.isNotEmpty()) TextButton(onClick = { vm.clear() }) { Text("Clear all") } }
            )
        }
    ) { pad ->
        if (logs.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize()) {
                EmptyState("🛡️", "No break-in attempts", "Turn on intruder selfie in Settings. Wrong attempts will be logged here.")
            }
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs, key = { it.id }) { e ->
                    val thumb by produceState<ImageBitmap?>(null, e.photoPath) {
                        value = e.photoPath?.let { p -> withContext(Dispatchers.IO) { runCatching { loadThumb(p) }.getOrNull() } }
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = .4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val t = thumb
                                if (t != null) Image(t, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Text("📵", style = MaterialTheme.typography.headlineMedium)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(vm.label(e.packageName), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    fmt.format(Date(e.timestamp)), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (e.photoPath == null) {
                                    Text("No photo (camera off)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { vm.delete(e) }) { Icon(Icons.Default.Delete, "Delete") }
                        }
                    }
                }
            }
        }
    }
}
