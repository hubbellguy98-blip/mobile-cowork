package com.phoneagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phoneagent.data.local.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("UPDATE sessions SET updatedAt = :time, messageCount = messageCount + 1 WHERE id = :sessionId")
    suspend fun updateSessionTimestamp(sessionId: String, time: Long)

    @Query("UPDATE sessions SET taskSuccessCount = taskSuccessCount + 1 WHERE id = :sessionId")
    suspend fun incrementSuccess(sessionId: String)

    @Query("UPDATE sessions SET taskFailCount = taskFailCount + 1 WHERE id = :sessionId")
    suspend fun incrementFail(sessionId: String)

    @Query("UPDATE sessions SET isArchived = 1 WHERE id = :sessionId")
    suspend fun archiveSession(sessionId: String)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
    
    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getTotalSessions(): Int
}
