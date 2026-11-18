package com.example.ai

import com.example.data.local.ActiveEngineMode
import com.example.data.local.LanServerEntity
import com.example.data.local.ModelCatalog
import com.example.data.local.UserAiPreferences
import kotlinx.coroutines.flow.Flow

sealed interface AiRouteTarget {
    data class OfflineLiteRt(val modelName: String) : AiRouteTarget
    data class LocalNetworkLan(val server: LanServerEntity) : AiRouteTarget
    data class Unavailable(val reason: String) : AiRouteTarget
}

class LocalAiCoordinator(
    private val liteRtEngineManager: LiteRtEngineManager,
    private val lanAiClient: LanAiClient
) {
    fun resolveTarget(
        prefs: UserAiPreferences,
        activeLanServer: LanServerEntity?
    ): AiRouteTarget {
        return when (prefs.engineMode) {
            ActiveEngineMode.OFFLINE_LITERT -> {
                val engineState = liteRtEngineManager.state.value
                if (engineState.isReady) {
                    AiRouteTarget.OfflineLiteRt(engineState.loadedModelName ?: "Modelo Offline")
                } else {
                    AiRouteTarget.Unavailable(
                        "Nenhum modelo offline carregado. Descarregue o Gemma 4 E2B (ou Qwen 3) no menu Modelos e toque em Carregar."
                    )
                }
            }
            ActiveEngineMode.LOCAL_NETWORK_LAN -> {
                if (activeLanServer != null && activeLanServer.baseUrl.isNotBlank()) {
                    AiRouteTarget.LocalNetworkLan(activeLanServer)
                } else {
                    AiRouteTarget.Unavailable(
                        "Nenhum servidor de IA na rede local configurado. Configure o IP do Ollama ou LM Studio no menu Modelos -> Rede Local."
                    )
                }
            }
        }
    }

    fun streamPrompt(
        target: AiRouteTarget,
        prefs: UserAiPreferences,
        history: List<LanChatTurn>,
        prompt: String,
        imageBase64Jpeg: String? = null
    ): Flow<String> {
        val systemPrompt = if (prefs.forcePortuguese) {
            ModelCatalog.DEFAULT_PORTUGUESE_SYSTEM_PROMPT
        } else {
            "És um assistente técnico local útil e preciso."
        }

        return when (target) {
            is AiRouteTarget.OfflineLiteRt -> {
                val finalPrompt = if (prefs.forcePortuguese) {
                    "[Responde em Português de Portugal]\n$prompt"
                } else {
                    prompt
                }
                liteRtEngineManager.generateStream(finalPrompt)
            }
            is AiRouteTarget.LocalNetworkLan -> {
                lanAiClient.streamChat(
                    server = target.server,
                    systemPrompt = systemPrompt,
                    history = history,
                    userPrompt = prompt,
                    imageBase64Jpeg = imageBase64Jpeg,
                    temperature = prefs.temperature
                )
            }
            is AiRouteTarget.Unavailable -> {
                throw IllegalStateException(target.reason)
            }
        }
    }

    suspend fun cancelActiveGeneration(mode: ActiveEngineMode) {
        if (mode == ActiveEngineMode.OFFLINE_LITERT) {
            liteRtEngineManager.cancelGeneration()
        }
    }
}
