package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nucleo_ia_prefs")

data class UserAiPreferences(
    val engineMode: ActiveEngineMode = ActiveEngineMode.OFFLINE_LITERT,
    val selectedOfflineModelId: String = "gemma-4-e2b-it",
    val darkTheme: Boolean = true,
    val forcePortuguese: Boolean = true,
    val temperature: Double = 0.7,
    val topK: Int = 32,
    val cameraUseFront: Boolean = false,
    val cameraFlashEnabled: Boolean = false,
    val cameraLiveVision: Boolean = true,
    val cameraRecordAudio: Boolean = true
)

class AppPreferences(private val context: Context) {
    private object Keys {
        val ENGINE_MODE = stringPreferencesKey("engine_mode")
        val SELECTED_OFFLINE_MODEL_ID = stringPreferencesKey("selected_offline_model_id")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val FORCE_PORTUGUESE = booleanPreferencesKey("force_portuguese")
        val TEMPERATURE = doublePreferencesKey("temperature")
        val TOP_K = intPreferencesKey("top_k")
        val CAMERA_USE_FRONT = booleanPreferencesKey("camera_use_front")
        val CAMERA_FLASH_ENABLED = booleanPreferencesKey("camera_flash_enabled")
        val CAMERA_LIVE_VISION = booleanPreferencesKey("camera_live_vision")
        val CAMERA_RECORD_AUDIO = booleanPreferencesKey("camera_record_audio")
    }

    val preferencesFlow: Flow<UserAiPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val mode = prefs[Keys.ENGINE_MODE]?.let {
                runCatching { ActiveEngineMode.valueOf(it) }.getOrNull()
            } ?: ActiveEngineMode.OFFLINE_LITERT

            UserAiPreferences(
                engineMode = mode,
                selectedOfflineModelId = prefs[Keys.SELECTED_OFFLINE_MODEL_ID] ?: "gemma-4-e2b-it",
                darkTheme = prefs[Keys.DARK_THEME] ?: true,
                forcePortuguese = prefs[Keys.FORCE_PORTUGUESE] ?: true,
                temperature = prefs[Keys.TEMPERATURE] ?: 0.7,
                topK = prefs[Keys.TOP_K] ?: 32,
                cameraUseFront = prefs[Keys.CAMERA_USE_FRONT] ?: false,
                cameraFlashEnabled = prefs[Keys.CAMERA_FLASH_ENABLED] ?: false,
                cameraLiveVision = prefs[Keys.CAMERA_LIVE_VISION] ?: true,
                cameraRecordAudio = prefs[Keys.CAMERA_RECORD_AUDIO] ?: true
            )
        }

    suspend fun setEngineMode(mode: ActiveEngineMode) {
        context.dataStore.edit { it[Keys.ENGINE_MODE] = mode.name }
    }

    suspend fun setSelectedOfflineModelId(modelId: String) {
        context.dataStore.edit { it[Keys.SELECTED_OFFLINE_MODEL_ID] = modelId }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = dark }
    }

    suspend fun setForcePortuguese(force: Boolean) {
        context.dataStore.edit { it[Keys.FORCE_PORTUGUESE] = force }
    }

    suspend fun setSampling(temperature: Double, topK: Int) {
        context.dataStore.edit {
            it[Keys.TEMPERATURE] = temperature
            it[Keys.TOP_K] = topK
        }
    }

    suspend fun setCameraUseFront(useFront: Boolean) {
        context.dataStore.edit { it[Keys.CAMERA_USE_FRONT] = useFront }
    }

    suspend fun setCameraFlashEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CAMERA_FLASH_ENABLED] = enabled }
    }

    suspend fun setCameraLiveVision(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CAMERA_LIVE_VISION] = enabled }
    }

    suspend fun setCameraRecordAudio(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CAMERA_RECORD_AUDIO] = enabled }
    }
}
