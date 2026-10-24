package com.customapplock.studio.data

import android.content.Context
import android.content.Intent
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.customapplock.studio.domain.LockType
import com.customapplock.studio.domain.MASTER_PKG
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockRepository @Inject constructor(
    private val dao: AppLockDao,
    private val intruderDao: IntruderDao
) {
    val all: Flow<List<AppLockEntity>> = dao.observeAll()
    val intruders: Flow<List<IntruderEntity>> = intruderDao.observeAll()

    suspend fun get(pkg: String): AppLockEntity? = dao.get(pkg)

    suspend fun saveLock(pkg: String, type: LockType, secret: String) {
        val hash = withContext(Dispatchers.Default) { SecretCodec.hash(secret) }
        dao.upsert(AppLockEntity(pkg, type.id, hash, isLocked = true, practiceStatus = true))
    }

    suspend fun setLocked(pkg: String, locked: Boolean) = dao.setLocked(pkg, locked)
    suspend fun remove(pkg: String) = dao.delete(pkg)

    suspend fun verify(e: AppLockEntity, secret: String): Boolean =
        withContext(Dispatchers.Default) { SecretCodec.verify(secret, e.encryptedSecretPasscode) }

    suspend fun addIntruder(e: IntruderEntity) { intruderDao.insert(e) }

    suspend fun deleteIntruder(e: IntruderEntity) {
        e.photoPath?.let { runCatching { File(it).delete() } }
        intruderDao.delete(e.id)
    }

    suspend fun clearIntruders() {
        intruderDao.snapshot().forEach { e -> e.photoPath?.let { runCatching { File(it).delete() } } }
        intruderDao.clear()
    }
}

data class InstalledApp(val packageName: String, val label: String)

@Singleton
class AppsRepository @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val iconCache = LruCache<String, ImageBitmap>(200)

    suspend fun loadApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .filter { it.first != ctx.packageName }
            .map { InstalledApp(it.first, it.second) }
            .sortedBy { it.label.lowercase() }
    }

    fun label(pkg: String): String {
        if (pkg == MASTER_PKG) return "App Lock Studio"
        return try {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    suspend fun icon(pkg: String): ImageBitmap? {
        iconCache.get(pkg)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }
                .getOrNull()
                ?.also { iconCache.put(pkg, it) }
        }
    }
}
