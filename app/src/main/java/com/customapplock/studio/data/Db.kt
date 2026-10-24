package com.customapplock.studio.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "locked_apps")
data class AppLockEntity(
    @PrimaryKey val packageName: String,
    val lockTypeId: String,
    /** Salted PBKDF2 hash of the secret (never the raw passcode). */
    val encryptedSecretPasscode: String,
    val isLocked: Boolean,
    val practiceStatus: Boolean
)

@Entity(tableName = "intruder_logs")
data class IntruderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long,
    val photoPath: String?
)

@Dao
interface AppLockDao {
    @Query("SELECT * FROM locked_apps")
    fun observeAll(): Flow<List<AppLockEntity>>

    @Query("SELECT * FROM locked_apps WHERE packageName = :pkg")
    suspend fun get(pkg: String): AppLockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: AppLockEntity)

    @Query("UPDATE locked_apps SET isLocked = :locked WHERE packageName = :pkg")
    suspend fun setLocked(pkg: String, locked: Boolean)

    @Query("DELETE FROM locked_apps WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}

@Dao
interface IntruderDao {
    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<IntruderEntity>>

    @Query("SELECT * FROM intruder_logs")
    suspend fun snapshot(): List<IntruderEntity>

    @Insert
    suspend fun insert(e: IntruderEntity): Long

    @Query("DELETE FROM intruder_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM intruder_logs")
    suspend fun clear()
}

@Database(entities = [AppLockEntity::class, IntruderEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appLockDao(): AppLockDao
    abstract fun intruderDao(): IntruderDao
}
