package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ModelStatus {
    AVAILABLE,
    DOWNLOADING,
    DOWNLOADED,
    IMPORTED,
    ERROR
}

enum class AiBackendPreference {
    CPU,
    GPU
}

enum class LanProtocol {
    OLLAMA,
    OPENAI_COMPATIBLE
}

enum class ActiveEngineMode {
    OFFLINE_LITERT,
    LOCAL_NETWORK_LAN
}

@Entity(tableName = "local_models")
data class LocalModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val family: String,
    val repoId: String,
    val fileName: String,
    val downloadUrl: String,
    val expectedSizeBytes: Long,
    val downloadedBytes: Long = 0L,
    val localPath: String? = null,
    val status: String = ModelStatus.AVAILABLE.name,
    val backendPreference: String = AiBackendPreference.GPU.name,
    val description: String,
    val isFeatured: Boolean = false,
    val isCustom: Boolean = false,
    val errorMessage: String? = null
)

@Entity(tableName = "lan_servers")
data class LanServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val protocol: String = LanProtocol.OLLAMA.name,
    val selectedModel: String = "gemma4",
    val availableModelsCsv: String = "",
    val isActive: Boolean = false,
    val lastLatencyMs: Long? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String = "default",
    val isUser: Boolean,
    val content: String,
    val engineLabel: String,
    val attachmentSummary: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "scanned_codes")
data class ScannedCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawValue: String,
    val editedValue: String,
    val formatName: String,
    val formatCode: Int,
    val semanticType: String,
    val title: String,
    val notes: String = "",
    val aiInterpretation: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vision_history")
data class VisionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaType: String, // "PHOTO" or "VIDEO"
    val mediaUri: String,
    val detectedLabels: String,
    val extractedText: String,
    val aiSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)
