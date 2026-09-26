package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "focus_modes")
data class FocusModeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String,
    val description: String,
    val colorHex: String,
    val blockedAppsJson: String,
    val allowedAppsJson: String,
    val callBehavior: String,
    val isStrictMode: Boolean,
    val strictModeType: String,
    val durationMinutes: Int,
    val scheduleDays: String,
    val scheduleStartHour: Int,
    val scheduleStartMinute: Int,
    val scheduleEndHour: Int,
    val scheduleEndMinute: Int,
    val scheduleEnabled: Boolean,
    val isPredefined: Boolean
)

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val sessionId: String,
    val modeId: String,
    val modeName: String,
    val blockedPackagesJson: String,
    val allowedPackagesJson: String,
    val callBehavior: String,
    val isStrictMode: Boolean,
    val strictModeType: String,
    val startTimeMillis: Long,
    val endTargetTimeMillis: Long,
    val emergencyOverrideActive: Boolean,
    val emergencyOverrideEndTimeMillis: Long,
    val firewallExpectedActive: Boolean,
    val lastUpdatedMillis: Long
)

@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String = "",
    val modeId: String = "",
    val modeName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val plannedDurationMinutes: Int = 0,
    val durationMinutes: Int,
    val blockedAppsCount: Int,
    val completedNaturally: Boolean,
    val endedEarly: Boolean = false,
    val emergencyOverrideUsed: Boolean = false
)

@Dao
interface FocusModeDao {
    @Query("SELECT * FROM focus_modes ORDER BY isPredefined DESC, name ASC")
    fun getAllFocusModes(): Flow<List<FocusModeEntity>>

    @Query("SELECT * FROM focus_modes WHERE id = :id LIMIT 1")
    suspend fun getFocusModeById(id: String): FocusModeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mode: FocusModeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modes: List<FocusModeEntity>)

    @Update
    suspend fun update(mode: FocusModeEntity)

    @Delete
    suspend fun delete(mode: FocusModeEntity)

    @Query("DELETE FROM focus_modes WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ActiveSessionDao {
    @Query("SELECT * FROM active_session LIMIT 1")
    suspend fun getActiveSession(): ActiveSessionEntity?

    @Query("SELECT * FROM active_session LIMIT 1")
    fun getActiveSessionFlow(): Flow<ActiveSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setActiveSession(session: ActiveSessionEntity)

    @Query("DELETE FROM active_session")
    suspend fun clearActiveSession()
}

@Dao
interface SessionHistoryDao {
    @Query("SELECT * FROM session_history ORDER BY startTimeMillis DESC")
    fun getAllHistory(): Flow<List<SessionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: SessionHistoryEntity)

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM session_history")
    fun getTotalMinutesFlow(): Flow<Long>

    @Query("SELECT COUNT(*) FROM session_history")
    fun getSessionCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM session_history WHERE sessionId = :sessionId")
    suspend fun hasSession(sessionId: String): Int
}

@Database(
    entities = [FocusModeEntity::class, ActiveSessionEntity::class, SessionHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class OptayDatabase : RoomDatabase() {
    abstract fun focusModeDao(): FocusModeDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun sessionHistoryDao(): SessionHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create active_session table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `active_session` (
                        `sessionId` TEXT NOT NULL,
                        `modeId` TEXT NOT NULL,
                        `modeName` TEXT NOT NULL,
                        `blockedPackagesJson` TEXT NOT NULL,
                        `allowedPackagesJson` TEXT NOT NULL,
                        `callBehavior` TEXT NOT NULL,
                        `isStrictMode` INTEGER NOT NULL,
                        `strictModeType` TEXT NOT NULL,
                        `startTimeMillis` INTEGER NOT NULL,
                        `endTargetTimeMillis` INTEGER NOT NULL,
                        `emergencyOverrideActive` INTEGER NOT NULL,
                        `emergencyOverrideEndTimeMillis` INTEGER NOT NULL,
                        `firewallExpectedActive` INTEGER NOT NULL,
                        `lastUpdatedMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`sessionId`)
                    )
                    """.trimIndent()
                )

                // 2. Add new columns to session_history safely
                try {
                    db.execSQL("ALTER TABLE `session_history` ADD COLUMN `sessionId` TEXT NOT NULL DEFAULT ''")
                } catch (ignored: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `session_history` ADD COLUMN `modeId` TEXT NOT NULL DEFAULT ''")
                } catch (ignored: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `session_history` ADD COLUMN `plannedDurationMinutes` INTEGER NOT NULL DEFAULT 0")
                } catch (ignored: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `session_history` ADD COLUMN `endedEarly` INTEGER NOT NULL DEFAULT 0")
                } catch (ignored: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `session_history` ADD COLUMN `emergencyOverrideUsed` INTEGER NOT NULL DEFAULT 0")
                } catch (ignored: Exception) {}
            }
        }
    }
}
