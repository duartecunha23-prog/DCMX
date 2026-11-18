package com.example.ai

import android.content.Context
import com.example.data.local.AiBackendPreference
import com.example.data.local.ModelCatalog
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

data class LiteRtEngineState(
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val isGenerating: Boolean = false,
    val loadedModelId: String? = null,
    val loadedModelName: String? = null,
    val loadedModelPath: String? = null,
    val activeBackend: AiBackendPreference? = null,
    val errorMessage: String? = null
)

class LiteRtEngineManager(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val dispatcher = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutex = Mutex()

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentSystemPrompt: String = ModelCatalog.DEFAULT_PORTUGUESE_SYSTEM_PROMPT
    private var currentTemperature: Double = 0.7
    private var currentTopK: Int = 32

    private val _state = MutableStateFlow(LiteRtEngineState())
    val state: StateFlow<LiteRtEngineState> = _state.asStateFlow()

    suspend fun loadModel(
        modelId: String,
        modelName: String,
        modelPath: String,
        preferredBackend: AiBackendPreference,
        systemPrompt: String = ModelCatalog.DEFAULT_PORTUGUESE_SYSTEM_PROMPT,
        temperature: Double = 0.7,
        topK: Int = 32
    ): Result<Unit> = withContext(dispatcher) {
        mutex.withLock {
            val file = File(modelPath)
            if (!file.exists() || !file.isFile) {
                val msg = "Ficheiro do modelo não encontrado em: $modelPath"
                _state.update { it.copy(isLoading = false, isReady = false, errorMessage = msg) }
                return@withLock Result.failure(IllegalStateException(msg))
            }

            if (_state.value.isReady &&
                _state.value.loadedModelPath == modelPath &&
                _state.value.activeBackend == preferredBackend
            ) {
                return@withLock Result.success(Unit)
            }

            releaseInternal()
            _state.update {
                it.copy(
                    isLoading = true,
                    isReady = false,
                    loadedModelId = modelId,
                    loadedModelName = modelName,
                    errorMessage = null
                )
            }

            currentSystemPrompt = systemPrompt
            currentTemperature = temperature
            currentTopK = topK

            val backendsToTry = if (preferredBackend == AiBackendPreference.GPU) {
                listOf(AiBackendPreference.GPU to Backend.GPU(), AiBackendPreference.CPU to Backend.CPU())
            } else {
                listOf(AiBackendPreference.CPU to Backend.CPU())
            }

            var lastError: Throwable? = null
            for ((backendEnum, backendObj) in backendsToTry) {
                var candidateEngine: Engine? = null
                try {
                    val config = EngineConfig(
                        modelPath = modelPath,
                        backend = backendObj,
                        cacheDir = context.cacheDir.path
                    )
                    candidateEngine = Engine(config).also { it.initialize() }
                    engine = candidateEngine
                    conversation = candidateEngine.createConversation(
                        ConversationConfig(
                            systemInstruction = Contents.of(systemPrompt),
                            samplerConfig = SamplerConfig(
                                topK = topK,
                                topP = 0.95,
                                temperature = temperature
                            )
                        )
                    )
                    _state.value = LiteRtEngineState(
                        isLoading = false,
                        isReady = true,
                        isGenerating = false,
                        loadedModelId = modelId,
                        loadedModelName = modelName,
                        loadedModelPath = modelPath,
                        activeBackend = backendEnum,
                        errorMessage = null
                    )
                    return@withLock Result.success(Unit)
                } catch (e: Throwable) {
                    lastError = e
                    // Never close an engine whose initialize() threw, unless initialize succeeded and createConversation threw
                    if (engine != null) {
                        runCatching { conversation?.close() }
                        conversation = null
                        runCatching { engine?.close() }
                        engine = null
                    }
                }
            }

            val errMsg = lastError?.localizedMessage ?: "Falha ao inicializar motor LiteRT-LM"
            _state.value = LiteRtEngineState(
                isLoading = false,
                isReady = false,
                errorMessage = errMsg
            )
            Result.failure(lastError ?: IllegalStateException(errMsg))
        }
    }

    fun generateStream(prompt: String): Flow<String> = flow {
        val conv = conversation
            ?: throw IllegalStateException("Nenhum modelo offline carregado. Descarregue ou selecione Gemma 4 E2B no separador Modelos.")
        if (!_state.value.isReady) {
            throw IllegalStateException("O motor offline ainda não está pronto.")
        }

        _state.update { it.copy(isGenerating = true, errorMessage = null) }
        try {
            val replies = conv.sendMessageAsync(prompt)
            replies.collect { chunk ->
                val text = chunk.toString()
                if (text.isNotEmpty()) {
                    emit(text)
                }
            }
        } catch (e: CancellationException) {
            runCatching { conv.cancelProcess() }
            throw e
        } catch (e: Throwable) {
            _state.update { it.copy(errorMessage = e.localizedMessage) }
            throw e
        } finally {
            _state.update { it.copy(isGenerating = false) }
        }
    }.flowOn(dispatcher)

    suspend fun cancelGeneration() = withContext(dispatcher) {
        runCatching { conversation?.cancelProcess() }
        _state.update { it.copy(isGenerating = false) }
    }

    suspend fun resetConversation(systemPrompt: String = currentSystemPrompt) = withContext(dispatcher) {
        mutex.withLock {
            val eng = engine ?: return@withLock
            while (_state.value.isGenerating) {
                runCatching { conversation?.cancelProcess() }
                delay(80)
            }
            runCatching { conversation?.close() }
            conversation = null
            currentSystemPrompt = systemPrompt
            conversation = eng.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                    samplerConfig = SamplerConfig(
                        topK = currentTopK,
                        topP = 0.95,
                        temperature = currentTemperature
                    )
                )
            )
        }
    }

    suspend fun unloadModel() = withContext(dispatcher) {
        mutex.withLock {
            releaseInternal()
        }
    }

    private suspend fun releaseInternal() {
        _state.update { it.copy(isReady = false) }
        while (_state.value.isGenerating) {
            runCatching { conversation?.cancelProcess() }
            delay(80)
        }
        runCatching { conversation?.close() }
        conversation = null
        runCatching { engine?.close() }
        engine = null
        _state.value = LiteRtEngineState()
    }

    fun shutdown() {
        scope.launch {
            mutex.withLock { releaseInternal() }
            executor.shutdown()
        }.invokeOnCompletion { scope.cancel() }
    }
}
