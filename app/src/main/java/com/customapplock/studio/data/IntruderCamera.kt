package com.customapplock.studio.data

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Silent front-camera capture (no preview) used for the break-in selfie. */
@Singleton
class IntruderCamera @Inject constructor(@ApplicationContext private val ctx: Context) {

    suspend fun capture(owner: LifecycleOwner, file: File): Boolean = withContext(Dispatchers.Main) {
        try {
            val executor = ContextCompat.getMainExecutor(ctx)
            val provider = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
                val f = ProcessCameraProvider.getInstance(ctx)
                f.addListener({
                    try { cont.resume(f.get()) } catch (e: Exception) { cont.resumeWithException(e) }
                }, executor)
            }
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            provider.unbindAll()
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, imageCapture)
            delay(600) // let auto-exposure settle
            val ok = suspendCancellableCoroutine<Boolean> { cont ->
                imageCapture.takePicture(
                    ImageCapture.OutputFileOptions.Builder(file).build(),
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) { cont.resume(true) }
                        override fun onError(exception: ImageCaptureException) { cont.resume(false) }
                    }
                )
            }
            provider.unbindAll()
            ok
        } catch (e: Exception) {
            false
        }
    }
}
