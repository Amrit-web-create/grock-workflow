package com.customapplock.studio.ui.lock

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customapplock.studio.data.AppLockEntity
import com.customapplock.studio.data.AppsRepository
import com.customapplock.studio.data.IntruderCamera
import com.customapplock.studio.data.IntruderEntity
import com.customapplock.studio.data.LockRepository
import com.customapplock.studio.data.SecretCodec
import com.customapplock.studio.data.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GateViewModel @Inject constructor(
    private val repo: LockRepository,
    private val apps: AppsRepository,
    private val camera: IntruderCamera,
    val settings: SettingsStore,
    @ApplicationContext private val ctx: Context
) : ViewModel() {

    suspend fun entity(pkg: String): AppLockEntity? = repo.get(pkg)
    suspend fun verify(e: AppLockEntity, secret: String): Boolean = repo.verify(e, secret)
    fun label(pkg: String): String = apps.label(pkg)
    suspend fun icon(pkg: String): ImageBitmap? = apps.icon(pkg)

    suspend fun checkAnswer(answer: String): Boolean = withContext(Dispatchers.Default) {
        val h = settings.state.value.securityAnswerHash
        h.isNotEmpty() && SecretCodec.verify(answer, h)
    }

    /** Called after every wrong attempt. Logs (and photographs) the intruder every N failures. */
    fun onWrong(pkg: String, failedCount: Int, owner: LifecycleOwner?) {
        val s = settings.state.value
        if (!s.intruderSelfie || failedCount % s.intruderAttempts != 0) return
        viewModelScope.launch {
            var path: String? = null
            val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (granted && owner != null) {
                val dir = File(ctx.filesDir, "intruders").apply { mkdirs() }
                val f = File(dir, "${System.currentTimeMillis()}.jpg")
                if (camera.capture(owner, f)) path = f.absolutePath
            }
            repo.addIntruder(IntruderEntity(packageName = pkg, timestamp = System.currentTimeMillis(), photoPath = path))
        }
    }
}
