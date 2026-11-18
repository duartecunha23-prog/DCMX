package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalModelDao {
    @Query("SELECT * FROM local_models ORDER BY isFeatured DESC, name ASC")
    fun observeAllModels(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): LocalModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(models: List<LocalModelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(model: LocalModelEntity)

    @Query("DELETE FROM local_models WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface LanServerDao {
    @Query("SELECT * FROM lan_servers ORDER BY isActive DESC, id DESC")
    fun observeServers(): Flow<List<LanServerEntity>>

    @Query("SELECT * FROM lan_servers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveServer(): LanServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: LanServerEntity): Long

    @Update
    suspend fun update(server: LanServerEntity)

    @Query("UPDATE lan_servers SET isActive = 0")
    suspend fun clearActiveFlags()

    @Query("UPDATE lan_servers SET isActive = 1 WHERE id = :id")
    suspend fun setActiveById(id: Long)

    @Query("DELETE FROM lan_servers WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    fun observeMessages(sessionId: String = "default"): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET content = :content WHERE id = :id")
    suspend fun updateContent(id: Long, content: String)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String = "default")
}

@Dao
interface ScannedCodeDao {
    @Query("SELECT * FROM scanned_codes ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<ScannedCodeEntity>>

    @Query("SELECT * FROM scanned_codes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScannedCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(code: ScannedCodeEntity): Long

    @Update
    suspend fun update(code: ScannedCodeEntity)

    @Query("DELETE FROM scanned_codes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scanned_codes")
    suspend fun clearAll()
}

@Dao
interface VisionHistoryDao {
    @Query("SELECT * FROM vision_history ORDER BY timestamp DESC, id DESC LIMIT 30")
    fun observeAll(): Flow<List<VisionHistoryEntity>>

    @Query("SELECT * FROM vision_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VisionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VisionHistoryEntity): Long

    @Query("DELETE FROM vision_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vision_history")
    suspend fun clearAll()
}
