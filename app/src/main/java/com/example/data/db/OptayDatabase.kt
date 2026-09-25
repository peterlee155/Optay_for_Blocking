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

@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMinutes: Int,
    val blockedAppsCount: Int,
    val completedNaturally: Boolean
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
interface SessionHistoryDao {
    @Query("SELECT * FROM session_history ORDER BY startTimeMillis DESC")
    fun getAllHistory(): Flow<List<SessionHistoryEntity>>

    @Insert
    suspend fun insert(session: SessionHistoryEntity)

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM session_history")
    fun getTotalMinutesFlow(): Flow<Long>

    @Query("SELECT COUNT(*) FROM session_history")
    fun getSessionCountFlow(): Flow<Int>
}

@Database(entities = [FocusModeEntity::class, SessionHistoryEntity::class], version = 1, exportSchema = false)
abstract class OptayDatabase : RoomDatabase() {
    abstract fun focusModeDao(): FocusModeDao
    abstract fun sessionHistoryDao(): SessionHistoryDao
}
