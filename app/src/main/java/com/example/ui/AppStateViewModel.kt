package com.example.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.DiscoveredLanNode
import com.example.ai.LanAiClient
import com.example.ai.LanChatTurn
import com.example.ai.LanProbeResult
import com.example.ai.LiteRtEngineManager
import com.example.ai.LiteRtEngineState
import com.example.ai.LocalAiCoordinator
import com.example.ai.ModelDownloadManager
import com.example.ai.AiRouteTarget
import com.example.data.local.ActiveEngineMode
import com.example.data.local.AiBackendPreference
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.local.ChatMessageEntity
import com.example.data.local.LanProtocol
import com.example.data.local.LanServerEntity
import com.example.data.local.LocalHistoryRepository
import com.example.data.local.LocalModelEntity
import com.example.data.local.ModelCatalog
import com.example.data.local.ModelStatus
import com.example.data.local.ScannedCodeEntity
import com.example.data.local.UserAiPreferences
import com.example.data.local.VisionHistoryEntity
import com.example.vision.BarcodeInterpreter
import com.example.vision.CodeInterpretation
import com.example.vision.PhotoVisionResult
import com.example.vision.VideoVisionResult
import com.example.vision.VisionAnalyzer
import com.example.vision.VisualLabel
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class VisionChatMessage(
    val isUser: Boolean,
    val text: String
)

data class ScannedEditorState(
    val existingId: Long? = null,
    val rawValue: String,
    val editedValue: String,
    val formatCode: Int,
    val formatName: String,
    val title: String,
    val notes: String,
    val interpretation: CodeInterpretation,
    val aiInterpretation: String = "",
    val isInterpretingAi: Boolean = false,
    val generatedBitmap: Bitmap? = null
)

data class DeviceDiagnostics(
    val deviceName: String,
    val androidVersion: String,
    val totalRamGb: Double,
    val availableRamGb: Double,
    val localIpv4: String,
    val modelsStorageBytes: Long
)

class AppStateViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val modelDao = db.localModelDao()
    private val lanDao = db.lanServerDao()
    private val chatDao = db.chatMessageDao()
    val historyRepository = LocalHistoryRepository(
        context = application,
        scannedCodeDao = db.scannedCodeDao(),
        visionHistoryDao = db.visionHistoryDao()
    )

    private val prefsStore = AppPreferences(application)
    val liteRtManager = LiteRtEngineManager(application)
    val lanClient = LanAiClient(application)
    private val downloadManager = ModelDownloadManager(application, modelDao)
    private val coordinator = LocalAiCoordinator(liteRtManager, lanClient)
    private val visionAnalyzer = VisionAnalyzer(application)

    val preferences: StateFlow<UserAiPreferences> = prefsStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserAiPreferences())

    val models: StateFlow<List<LocalModelEntity>> = modelDao.observeAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lanServers: StateFlow<List<LanServerEntity>> = lanDao.observeServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val engineState: StateFlow<LiteRtEngineState> = liteRtManager.state

    val chatMessages: StateFlow<List<ChatMessageEntity>> = chatDao.observeMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scannedCodes: StateFlow<List<ScannedCodeEntity>> = historyRepository.scannedCodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visionHistory: StateFlow<List<VisionHistoryEntity>> = historyRepository.visionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _isDiscoveringLan = MutableStateFlow(false)
    val isDiscoveringLan: StateFlow<Boolean> = _isDiscoveringLan.asStateFlow()

    private val _discoveredNodes = MutableStateFlow<List<DiscoveredLanNode>>(emptyList())
    val discoveredNodes: StateFlow<List<DiscoveredLanNode>> = _discoveredNodes.asStateFlow()

    private val _lanProbeStatus = MutableStateFlow<String?>(null)
    val lanProbeStatus: StateFlow<String?> = _lanProbeStatus.asStateFlow()

    private val _photoVision = MutableStateFlow<PhotoVisionResult?>(null)
    val photoVision: StateFlow<PhotoVisionResult?> = _photoVision.asStateFlow()

    private val _videoVision = MutableStateFlow<VideoVisionResult?>(null)
    val videoVision: StateFlow<VideoVisionResult?> = _videoVision.asStateFlow()

    private val _isAnalyzingMedia = MutableStateFlow(false)
    val isAnalyzingMedia: StateFlow<Boolean> = _isAnalyzingMedia.asStateFlow()

    private val _visionError = MutableStateFlow<String?>(null)
    val visionError: StateFlow<String?> = _visionError.asStateFlow()

    private val _visionChat = MutableStateFlow<List<VisionChatMessage>>(emptyList())
    val visionChat: StateFlow<List<VisionChatMessage>> = _visionChat.asStateFlow()

    private val _isVisionAiBusy = MutableStateFlow(false)
    val isVisionAiBusy: StateFlow<Boolean> = _isVisionAiBusy.asStateFlow()

    private val _scannerEditor = MutableStateFlow<ScannedEditorState?>(null)
    val scannerEditor: StateFlow<ScannedEditorState?> = _scannerEditor.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var activeChatJob: Job? = null
    private var activeVisionJob: Job? = null
    private var activeScanAiJob: Job? = null

    init {
        seedInitialCatalog()
    }

    private fun seedInitialCatalog() {
        viewModelScope.launch {
            val existingModels = modelDao.observeAllModels().first()
            if (existingModels.isEmpty()) {
                modelDao.upsertAll(ModelCatalog.defaultModels())
            } else {
                // Reset stale DOWNLOADING state if app restarted mid-transfer, verify downloaded files exist
                existingModels.forEach { m ->
                    val status = runCatching { ModelStatus.valueOf(m.status) }.getOrNull()
                    if (status == ModelStatus.DOWNLOADING) {
                        modelDao.upsert(m.copy(status = ModelStatus.AVAILABLE.name, downloadedBytes = 0L))
                    } else if ((status == ModelStatus.DOWNLOADED || status == ModelStatus.IMPORTED) &&
                        (m.localPath == null || !File(m.localPath).exists())
                    ) {
                        modelDao.upsert(m.copy(status = ModelStatus.AVAILABLE.name, localPath = null, downloadedBytes = 0L))
                    }
                }
            }

            val existingLan = lanDao.observeServers().first()
            if (existingLan.isEmpty()) {
                ModelCatalog.defaultLanServers().forEach { lanDao.insert(it) }
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun setEngineMode(mode: ActiveEngineMode) {
        viewModelScope.launch {
            prefsStore.setEngineMode(mode)
        }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            prefsStore.setDarkTheme(dark)
        }
    }

    fun setForcePortuguese(force: Boolean) {
        viewModelScope.launch {
            prefsStore.setForcePortuguese(force)
        }
    }

    fun setCameraUseFront(useFront: Boolean) {
        viewModelScope.launch {
            prefsStore.setCameraUseFront(useFront)
        }
    }

    fun setCameraFlashEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setCameraFlashEnabled(enabled)
        }
    }

    fun setCameraLiveVision(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setCameraLiveVision(enabled)
        }
    }

    fun setCameraRecordAudio(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setCameraRecordAudio(enabled)
        }
    }

    fun startModelDownload(model: LocalModelEntity) {
        downloadManager.startDownload(model)
    }

    fun cancelModelDownload(modelId: String) {
        downloadManager.cancelDownload(modelId)
    }

    fun deleteModel(model: LocalModelEntity) {
        viewModelScope.launch {
            if (engineState.value.loadedModelId == model.id) {
                liteRtManager.unloadModel()
            }
            downloadManager.deleteLocalFile(model)
            _statusBanner.value = "Modelo ${model.name} removido do dispositivo."
        }
    }

    fun toggleModelBackend(model: LocalModelEntity) {
        viewModelScope.launch {
            val next = if (model.backendPreference == AiBackendPreference.GPU.name) {
                AiBackendPreference.CPU.name
            } else {
                AiBackendPreference.GPU.name
            }
            modelDao.upsert(model.copy(backendPreference = next))
        }
    }

    fun loadOfflineModel(model: LocalModelEntity) {
        val path = model.localPath ?: return
        viewModelScope.launch {
            val pref = runCatching { AiBackendPreference.valueOf(model.backendPreference) }
                .getOrDefault(AiBackendPreference.GPU)
            val userPrefs = preferences.value
            val result = liteRtManager.loadModel(
                modelId = model.id,
                modelName = model.name,
                modelPath = path,
                preferredBackend = pref,
                systemPrompt = if (userPrefs.forcePortuguese) ModelCatalog.DEFAULT_PORTUGUESE_SYSTEM_PROMPT else "És um assistente útil.",
                temperature = userPrefs.temperature,
                topK = userPrefs.topK
            )
            result.onSuccess {
                prefsStore.setSelectedOfflineModelId(model.id)
                prefsStore.setEngineMode(ActiveEngineMode.OFFLINE_LITERT)
                _statusBanner.value = "${model.name} carregado em modo offline (${liteRtManager.state.value.activeBackend?.name})."
            }.onFailure { err ->
                _statusBanner.value = "Erro ao carregar ${model.name}: ${err.localizedMessage}"
            }
        }
    }

    fun unloadOfflineModel() {
        viewModelScope.launch {
            liteRtManager.unloadModel()
        }
    }

    fun addCustomModel(name: String, repoOrUrl: String, fileName: String, sizeMb: Long) {
        viewModelScope.launch {
            val cleanUrl = if (repoOrUrl.startsWith("http://") || repoOrUrl.startsWith("https://")) {
                repoOrUrl.trim()
            } else {
                val repo = repoOrUrl.trim().trim('/')
                val file = fileName.trim().ifBlank { "model.litertlm" }
                "https://huggingface.co/$repo/resolve/main/$file"
            }
            val resolvedFile = fileName.trim().ifBlank {
                cleanUrl.substringAfterLast('/').substringBefore('?').ifBlank { "custom_model.litertlm" }
            }
            val entity = LocalModelEntity(
                id = "custom_${System.currentTimeMillis()}",
                name = name.trim().ifBlank { resolvedFile.removeSuffix(".litertlm") },
                family = "Personalizado",
                repoId = repoOrUrl.trim(),
                fileName = resolvedFile,
                downloadUrl = cleanUrl,
                expectedSizeBytes = (sizeMb.coerceAtLeast(100L)) * 1024L * 1024L,
                backendPreference = AiBackendPreference.GPU.name,
                description = "Modelo .litertlm personalizado adicionado pelo utilizador para descarga offline.",
                isFeatured = false,
                isCustom = true
            )
            modelDao.upsert(entity)
            _statusBanner.value = "Modelo ${entity.name} adicionado ao catálogo."
        }
    }

    fun importLocalModel(uri: Uri) {
        viewModelScope.launch {
            _statusBanner.value = "A importar ficheiro .litertlm..."
            val result = downloadManager.importLocalModelUri(uri)
            result.onSuccess { entity ->
                _statusBanner.value = "${entity.name} importado. Toque em Carregar para usar offline."
            }.onFailure { err ->
                _statusBanner.value = "Erro ao importar modelo: ${err.localizedMessage}"
            }
        }
    }

    // --- Rede Local (LAN) ---
    fun saveAndActivateLanServer(
        id: Long?,
        name: String,
        baseUrl: String,
        protocol: LanProtocol,
        selectedModel: String
    ) {
        viewModelScope.launch {
            lanDao.clearActiveFlags()
            val normalizedUrl = LanAiClient.normalizeBaseUrl(baseUrl)
            val entity = LanServerEntity(
                id = id ?: 0L,
                name = name.trim().ifBlank { "Servidor IA Local" },
                baseUrl = normalizedUrl,
                protocol = protocol.name,
                selectedModel = selectedModel.trim().ifBlank { "gemma4" },
                isActive = true
            )
            lanDao.insert(entity)
            prefsStore.setEngineMode(ActiveEngineMode.LOCAL_NETWORK_LAN)
            _statusBanner.value = "Servidor LAN ativado: $normalizedUrl"
        }
    }

    fun activateLanServer(server: LanServerEntity) {
        viewModelScope.launch {
            lanDao.clearActiveFlags()
            lanDao.setActiveById(server.id)
            prefsStore.setEngineMode(ActiveEngineMode.LOCAL_NETWORK_LAN)
        }
    }

    fun deleteLanServer(server: LanServerEntity) {
        viewModelScope.launch {
            lanDao.deleteById(server.id)
        }
    }

    fun probeLanServer(server: LanServerEntity) {
        viewModelScope.launch {
            _lanProbeStatus.value = "A testar ligação a ${server.baseUrl}..."
            val protocol = runCatching { LanProtocol.valueOf(server.protocol) }.getOrDefault(LanProtocol.OLLAMA)
            val result: LanProbeResult = lanClient.probeServer(server.baseUrl, protocol)
            if (result.reachable) {
                val modelsCsv = result.models.joinToString(",")
                val chosenModel = if (result.models.isNotEmpty() && !result.models.contains(server.selectedModel)) {
                    result.models.first()
                } else {
                    server.selectedModel
                }
                lanDao.update(
                    server.copy(
                        availableModelsCsv = modelsCsv,
                        selectedModel = chosenModel,
                        lastLatencyMs = result.latencyMs
                    )
                )
                _lanProbeStatus.value =
                    "Ligado em ${result.latencyMs}ms • ${result.models.size} modelo(s) detetado(s)."
            } else {
                _lanProbeStatus.value = "Falha: ${result.error ?: "Sem resposta"}"
            }
        }
    }

    fun selectLanModel(server: LanServerEntity, modelName: String) {
        viewModelScope.launch {
            lanDao.update(server.copy(selectedModel = modelName))
        }
    }

    fun discoverLanNodes() {
        if (_isDiscoveringLan.value) return
        viewModelScope.launch {
            _isDiscoveringLan.value = true
            _lanProbeStatus.value = "A procurar servidores IA (Ollama / LM Studio) na rede Wi-Fi local..."
            val nodes = lanClient.discoverLanNodes()
            _discoveredNodes.value = nodes
            _isDiscoveringLan.value = false
            _lanProbeStatus.value = if (nodes.isEmpty()) {
                "Nenhum servidor IA detetado na sub-rede (${lanClient.getDeviceLocalIpv4() ?: "sem Wi-Fi"}). Verifique se o servidor permite ligações na rede local (ex: OLLAMA_HOST=0.0.0.0)."
            } else {
                "${nodes.size} servidor(es) IA encontrado(s) na rede local!"
            }
        }
    }

    // --- Chat Assistente ---
    fun sendChatMessage(rawPrompt: String) {
        val text = rawPrompt.trim()
        if (text.isBlank() || _isChatGenerating.value) return

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            _chatError.value = null
            val prefs = preferences.value
            val activeLan = lanDao.getActiveServer()
            val target = coordinator.resolveTarget(prefs, activeLan)

            if (target is AiRouteTarget.Unavailable) {
                _chatError.value = target.reason
                _statusBanner.value = target.reason
                return@launch
            }

            val label = when (target) {
                is AiRouteTarget.OfflineLiteRt -> "Offline • ${target.modelName}"
                is AiRouteTarget.LocalNetworkLan -> "LAN • ${target.server.selectedModel}"
                is AiRouteTarget.Unavailable -> ""
            }

            val historySnapshot = chatMessages.value.takeLast(10).map {
                LanChatTurn(isUser = it.isUser, content = it.content)
            }

            chatDao.insert(ChatMessageEntity(isUser = true, content = text, engineLabel = label))
            val assistantMsgId = chatDao.insert(
                ChatMessageEntity(isUser = false, content = "...", engineLabel = label)
            )

            _isChatGenerating.value = true
            val accumulated = StringBuilder()
            try {
                coordinator.streamPrompt(
                    target = target,
                    prefs = prefs,
                    history = historySnapshot,
                    prompt = text
                ).collect { chunk ->
                    accumulated.append(chunk)
                    chatDao.updateContent(assistantMsgId, accumulated.toString())
                }
                if (accumulated.isBlank()) {
                    chatDao.updateContent(assistantMsgId, "(Sem resposta gerada pelo modelo)")
                }
            } catch (e: CancellationException) {
                if (accumulated.isBlank()) {
                    chatDao.updateContent(assistantMsgId, "[Geração interrompida]")
                }
            } catch (e: Throwable) {
                val errMsg = e.localizedMessage ?: "Erro durante a inferência"
                _chatError.value = errMsg
                chatDao.updateContent(assistantMsgId, "Erro: $errMsg")
            } finally {
                _isChatGenerating.value = false
            }
        }
    }

    fun stopChatGeneration() {
        viewModelScope.launch {
            coordinator.cancelActiveGeneration(preferences.value.engineMode)
            activeChatJob?.cancel()
            _isChatGenerating.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            activeChatJob?.cancel()
            _isChatGenerating.value = false
            chatDao.clearSession()
            liteRtManager.resetConversation()
            _chatError.value = null
            _statusBanner.value = "Histórico de perguntas apagado."
        }
    }

    fun deleteChatMessage(messageId: Long) {
        viewModelScope.launch {
            chatDao.deleteMessageById(messageId)
        }
    }

    fun clearVisionHistory() {
        viewModelScope.launch {
            activeVisionJob?.cancel()
            _isVisionAiBusy.value = false
            _visionChat.value = emptyList()
            _visionError.value = null
            _photoVision.value = null
            _videoVision.value = null
            historyRepository.clearVisionHistory()
            _statusBanner.value = "Histórico de imagens e perguntas da câmara apagado."
        }
    }

    fun deleteVisionHistoryItem(item: VisionHistoryEntity) {
        viewModelScope.launch {
            historyRepository.deleteVisionItem(item)
            if (_photoVision.value?.sourceUri == item.mediaUri) {
                _photoVision.value = null
            }
        }
    }

    fun openSavedVisionItem(item: VisionHistoryEntity) {
        if (item.mediaType == "VIDEO") {
            analyzeVideoUri(Uri.parse(item.mediaUri))
            return
        }
        viewModelScope.launch {
            _isAnalyzingMedia.value = true
            _visionError.value = null
            _videoVision.value = null
            try {
                val bmp = historyRepository.loadBitmapFromLocalUri(item.mediaUri)
                val b64 = bmp?.let { historyRepository.encodeBitmapToBase64(it) }
                val labels = item.detectedLabels
                    .split(",")
                    .mapNotNull { raw ->
                        val cleaned = raw.trim()
                        if (cleaned.isEmpty()) null else VisualLabel(text = cleaned, confidence = 1f)
                    }
                _photoVision.value = PhotoVisionResult(
                    sourceUri = item.mediaUri,
                    previewBitmap = bmp,
                    labels = labels,
                    extractedText = item.extractedText,
                    detectedBarcodes = emptyList(),
                    resolutionLabel = if (bmp != null) "${bmp.width}x${bmp.height}" else "",
                    base64Jpeg = b64
                )
                _visionChat.value = if (item.aiSummary.isNotBlank()) {
                    listOf(VisionChatMessage(isUser = false, text = item.aiSummary))
                } else {
                    emptyList()
                }
            } catch (e: Throwable) {
                _visionError.value = e.localizedMessage ?: "Falha ao abrir imagem guardada"
            } finally {
                _isAnalyzingMedia.value = false
            }
        }
    }

    fun clearAllQuestionHistory() {
        viewModelScope.launch {
            activeChatJob?.cancel()
            activeVisionJob?.cancel()
            _isChatGenerating.value = false
            _isVisionAiBusy.value = false
            chatDao.clearSession()
            liteRtManager.resetConversation()
            _chatError.value = null
            _visionChat.value = emptyList()
            _visionError.value = null
            _photoVision.value = null
            _videoVision.value = null
            historyRepository.clearVisionHistory()
            _statusBanner.value = "Todo o histórico de perguntas foi apagado."
        }
    }

    // --- Máquina Fotográfica & Visão IA (Fotos e Vídeos) ---
    fun analyzePhotoUri(uri: Uri) {
        viewModelScope.launch {
            _isAnalyzingMedia.value = true
            _visionError.value = null
            _videoVision.value = null
            _visionChat.value = emptyList()
            try {
                val res = visionAnalyzer.analyzePhoto(uri)
                val initialSummary = buildLocalVisionSummary(res)
                val storedUri = res.previewBitmap?.let { bmp ->
                    historyRepository.saveCapturedImageAndRecord(
                        bitmap = bmp,
                        prefix = "galeria",
                        detectedLabels = res.labels.joinToString(", ") { it.text },
                        extractedText = res.extractedText,
                        aiSummary = initialSummary
                    ).mediaUri
                } ?: uri.toString()
                _photoVision.value = res.copy(sourceUri = storedUri)
                _visionChat.value = listOf(
                    VisionChatMessage(isUser = false, text = initialSummary)
                )
            } catch (e: Throwable) {
                _visionError.value = e.localizedMessage ?: "Falha ao analisar fotografia"
            } finally {
                _isAnalyzingMedia.value = false
            }
        }
    }

    fun analyzeCapturedBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAnalyzingMedia.value = true
            _visionError.value = null
            _videoVision.value = null
            _visionChat.value = emptyList()
            try {
                val res = visionAnalyzer.analyzeBitmap(bitmap, "captura_camara")
                val initialSummary = buildLocalVisionSummary(res)
                val saved = historyRepository.saveCapturedImageAndRecord(
                    bitmap = res.previewBitmap ?: bitmap,
                    prefix = "camara",
                    detectedLabels = res.labels.joinToString(", ") { it.text },
                    extractedText = res.extractedText,
                    aiSummary = initialSummary
                )
                _photoVision.value = res.copy(sourceUri = saved.mediaUri)
                _visionChat.value = listOf(
                    VisionChatMessage(isUser = false, text = initialSummary)
                )
            } catch (e: Throwable) {
                _visionError.value = e.localizedMessage ?: "Falha ao analisar captura da câmara"
            } finally {
                _isAnalyzingMedia.value = false
            }
        }
    }

    fun analyzeVideoUri(uri: Uri) {
        viewModelScope.launch {
            _isAnalyzingMedia.value = true
            _visionError.value = null
            _photoVision.value = null
            _visionChat.value = emptyList()
            try {
                val res = visionAnalyzer.analyzeVideo(uri)
                _videoVision.value = res
                val initialSummary = buildString {
                    appendLine("Análise local do vídeo concluída (${res.keyframes.size} fotogramas extraídos, ${"%.1f".format(res.durationMs / 1000.0)}s).")
                    if (res.combinedLabels.isNotEmpty()) {
                        appendLine("Conceitos principais: ${res.combinedLabels.take(6).joinToString(", ") { it.text }}.")
                    }
                    if (res.combinedText.isNotBlank()) {
                        appendLine("Texto OCR detetado no vídeo:\n${res.combinedText.take(240)}")
                    }
                    append("Faça uma pergunta ou peça uma descrição detalhada à IA Local abaixo.")
                }
                _visionChat.value = listOf(VisionChatMessage(isUser = false, text = initialSummary))
                historyRepository.recordVideoVision(
                    mediaUri = uri.toString(),
                    detectedLabels = res.combinedLabels.joinToString(", ") { it.text },
                    extractedText = res.combinedText,
                    aiSummary = initialSummary
                )
            } catch (e: Throwable) {
                _visionError.value = e.localizedMessage ?: "Falha ao analisar vídeo"
            } finally {
                _isAnalyzingMedia.value = false
            }
        }
    }

    fun askVisionAi(question: String) {
        val q = question.trim()
        if (q.isBlank() || _isVisionAiBusy.value) return
        val photo = _photoVision.value
        val video = _videoVision.value
        if (photo == null && video == null) {
            _visionError.value = "Capture ou selecione primeiro uma fotografia ou vídeo."
            return
        }

        activeVisionJob?.cancel()
        activeVisionJob = viewModelScope.launch {
            _visionError.value = null
            val prefs = preferences.value
            val activeLan = lanDao.getActiveServer()
            val target = coordinator.resolveTarget(prefs, activeLan)
            if (target is AiRouteTarget.Unavailable) {
                // Offline on-device ML Kit + Pixel Inspector fallback so photo/video Q&A always works offline
                val offlineAnswer = buildOfflineFallbackVisionAnswer(q, photo, video)
                _visionChat.update {
                    it + VisionChatMessage(isUser = true, text = q) + VisionChatMessage(isUser = false, text = offlineAnswer)
                }
                return@launch
            }

            val contextBlock = photo?.buildAiContextPrompt() ?: video?.buildAiContextPrompt().orEmpty()
            val imageB64 = photo?.base64Jpeg ?: video?.primaryFrameBase64
            val fullPrompt = "$contextBlock\nPergunta do utilizador sobre este conteúdo visual: $q"

            _visionChat.update { it + VisionChatMessage(isUser = true, text = q) + VisionChatMessage(isUser = false, text = "...") }
            _isVisionAiBusy.value = true
            val acc = StringBuilder()

            try {
                coordinator.streamPrompt(
                    target = target,
                    prefs = prefs,
                    history = emptyList(),
                    prompt = fullPrompt,
                    imageBase64Jpeg = imageB64
                ).collect { chunk ->
                    acc.append(chunk)
                    _visionChat.update { list ->
                        if (list.isEmpty()) list else list.dropLast(1) + VisionChatMessage(isUser = false, text = acc.toString())
                    }
                }
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Throwable) {
                _visionError.value = e.localizedMessage ?: "Erro na interação visual com IA Local"
            } finally {
                _isVisionAiBusy.value = false
            }
        }
    }

    private fun buildOfflineFallbackVisionAnswer(
        question: String,
        photo: PhotoVisionResult?,
        video: VideoVisionResult?
    ): String = buildString {
        val lower = question.lowercase()
        appendLine("Resposta do Analisador Visual Offline (ML Kit + Inspeção Local):")
        if (photo != null) {
            if (lower.contains("cor") || lower.contains("luz") || lower.contains("ilumin")) {
                appendLine("• Cores dominantes: ${photo.dominantColors.joinToString(", ").ifBlank { "Não apuradas" }}.")
                appendLine("• Iluminação e resolução: ${photo.brightnessLabel} (${photo.resolutionLabel}).")
            }
            if (lower.contains("texto") || lower.contains("ler") || lower.contains("lê") || lower.contains("ocr") || lower.contains("traduz")) {
                appendLine(
                    if (photo.extractedText.isNotBlank()) {
                        "• Texto reconhecido (OCR):\n\"${photo.extractedText}\""
                    } else {
                        "• Não foi detetado texto legível nesta fotografia."
                    }
                )
            } else {
                if (photo.labels.isNotEmpty()) {
                    appendLine("• Elementos identificados: ${photo.labels.joinToString(", ") { "${it.text} (${(it.confidence * 100).toInt()}%)" }}.")
                }
                if (photo.dominantColors.isNotEmpty()) {
                    appendLine("• Paleta predominante: ${photo.dominantColors.joinToString(", ")} (${photo.brightnessLabel}).")
                }
                if (photo.extractedText.isNotBlank()) {
                    appendLine("• Texto visível: \"${photo.extractedText.take(180)}\"")
                }
                if (photo.detectedBarcodes.isNotEmpty()) {
                    appendLine("• Códigos encontrados: ${photo.detectedBarcodes.joinToString(" | ")}")
                }
            }
        } else if (video != null) {
            appendLine("• Vídeo com duração de ${"%.1f".format(video.durationMs / 1000.0)}s e ${video.keyframes.size} fotogramas analisados:")
            video.keyframes.forEach { frame ->
                val sec = "%.1f".format(frame.timestampMs / 1000.0)
                val lbl = frame.labels.take(3).joinToString(", ") { it.text }.ifBlank { "Cena geral" }
                append("  - ${sec}s: $lbl")
                if (frame.extractedText.isNotBlank()) append(" | OCR: \"${frame.extractedText.replace("\n", " ").take(60)}\"")
                appendLine()
            }
        }
        append("\nDica: Carregue o Gemma 4 E2B no separador Modelos para diálogo generativo completo sobre esta análise.")
    }

    private fun buildLocalVisionSummary(res: PhotoVisionResult): String = buildString {
        appendLine("Reconhecimento offline concluído (${res.resolutionLabel} • ${res.brightnessLabel}):")
        if (res.dominantColors.isNotEmpty()) {
            appendLine("Cores principais: ${res.dominantColors.joinToString(", ")}.")
        }
        if (res.labels.isNotEmpty()) {
            appendLine("Elementos: ${res.labels.take(6).joinToString(", ") { "${it.text} (${(it.confidence * 100).toInt()}%)" }}.")
        } else {
            appendLine("Sem rótulos visuais acima do limiar.")
        }
        if (res.extractedText.isNotBlank()) {
            appendLine("Texto OCR: \"${res.extractedText.take(260)}\"")
        }
        if (res.detectedBarcodes.isNotEmpty()) {
            appendLine("Códigos detetados: ${res.detectedBarcodes.joinToString(", ")}")
        }
        append("Pode agora conversar com a IA Offline (Gemma 4 E2B / Análise Local) sobre esta imagem.")
    }

    // --- Scanner QR & Códigos de Barras ---
    fun onBarcodeDetected(rawValue: String, formatCode: Int) {
        val current = _scannerEditor.value
        if (current != null && current.rawValue == rawValue && current.formatCode == formatCode) {
            return
        }
        openEditorWithCode(
            rawValue = rawValue,
            formatCode = formatCode,
            existingId = null,
            existingNotes = "",
            existingAi = "",
            autoPersist = true
        )
    }

    fun scanBarcodeFromPhotoUri(uri: Uri) {
        viewModelScope.launch {
            val res = runCatching { visionAnalyzer.analyzePhoto(uri) }.getOrNull()
            val firstCode = res?.detectedBarcodes?.firstOrNull()
            if (firstCode != null) {
                openEditorWithCode(
                    rawValue = firstCode,
                    formatCode = Barcode.FORMAT_QR_CODE,
                    existingId = null,
                    existingNotes = "",
                    existingAi = "",
                    autoPersist = true
                )
            } else {
                _statusBanner.value = "Nenhum código QR ou de barras encontrado na imagem."
            }
        }
    }

    fun openManualBarcodeEditor() {
        openEditorWithCode(
            rawValue = "https://nucleoia.local",
            formatCode = Barcode.FORMAT_QR_CODE,
            existingId = null,
            existingNotes = "",
            existingAi = ""
        )
    }

    fun openSavedCode(entity: ScannedCodeEntity) {
        openEditorWithCode(
            rawValue = entity.rawValue,
            formatCode = entity.formatCode,
            existingId = entity.id,
            existingNotes = entity.notes,
            existingAi = entity.aiInterpretation,
            overrideEdited = entity.editedValue,
            overrideTitle = entity.title
        )
    }

    private fun openEditorWithCode(
        rawValue: String,
        formatCode: Int,
        existingId: Long?,
        existingNotes: String,
        existingAi: String,
        overrideEdited: String? = null,
        overrideTitle: String? = null,
        autoPersist: Boolean = false
    ) {
        viewModelScope.launch {
            val effectiveVal = overrideEdited ?: rawValue
            val interp = BarcodeInterpreter.interpret(effectiveVal, formatCode)
            val bmp = withContext(Dispatchers.Default) {
                BarcodeInterpreter.generateBarcodeBitmap(effectiveVal, formatCode)
            }
            val resolvedId = if (autoPersist && existingId == null) {
                historyRepository.saveScannedCode(
                    ScannedCodeEntity(
                        rawValue = rawValue,
                        editedValue = effectiveVal,
                        formatName = interp.formatName,
                        formatCode = formatCode,
                        semanticType = interp.semanticType,
                        title = overrideTitle ?: interp.title,
                        notes = existingNotes,
                        aiInterpretation = existingAi
                    )
                )
            } else {
                existingId
            }
            _scannerEditor.value = ScannedEditorState(
                existingId = resolvedId,
                rawValue = rawValue,
                editedValue = effectiveVal,
                formatCode = formatCode,
                formatName = interp.formatName,
                title = overrideTitle ?: interp.title,
                notes = existingNotes,
                interpretation = interp,
                aiInterpretation = existingAi,
                generatedBitmap = bmp
            )
        }
    }

    fun updateEditorPayload(newValue: String) {
        val current = _scannerEditor.value ?: return
        viewModelScope.launch {
            val interp = BarcodeInterpreter.interpret(newValue, current.formatCode)
            val bmp = withContext(Dispatchers.Default) {
                BarcodeInterpreter.generateBarcodeBitmap(newValue, current.formatCode)
            }
            _scannerEditor.value = current.copy(
                editedValue = newValue,
                interpretation = interp,
                generatedBitmap = bmp
            )
        }
    }

    fun updateEditorFormat(newFormatCode: Int) {
        val current = _scannerEditor.value ?: return
        viewModelScope.launch {
            val interp = BarcodeInterpreter.interpret(current.editedValue, newFormatCode)
            val bmp = withContext(Dispatchers.Default) {
                BarcodeInterpreter.generateBarcodeBitmap(current.editedValue, newFormatCode)
            }
            _scannerEditor.value = current.copy(
                formatCode = newFormatCode,
                formatName = interp.formatName,
                interpretation = interp,
                generatedBitmap = bmp
            )
        }
    }

    fun updateEditorMeta(title: String, notes: String) {
        _scannerEditor.update { it?.copy(title = title, notes = notes) }
    }

    fun saveEditedCode() {
        val current = _scannerEditor.value ?: return
        viewModelScope.launch {
            val entity = ScannedCodeEntity(
                id = current.existingId ?: 0L,
                rawValue = current.rawValue,
                editedValue = current.editedValue,
                formatName = current.formatName,
                formatCode = current.formatCode,
                semanticType = current.interpretation.semanticType,
                title = current.title.ifBlank { current.interpretation.title },
                notes = current.notes,
                aiInterpretation = current.aiInterpretation
            )
            val id = historyRepository.saveScannedCode(entity)
            _scannerEditor.update { it?.copy(existingId = id) }
            _statusBanner.value = "Código guardado no histórico local."
        }
    }

    fun interpretCodeWithAi() {
        val current = _scannerEditor.value ?: return
        if (current.isInterpretingAi) return

        activeScanAiJob?.cancel()
        activeScanAiJob = viewModelScope.launch {
            val prefs = preferences.value
            val activeLan = lanDao.getActiveServer()
            val target = coordinator.resolveTarget(prefs, activeLan)
            if (target is AiRouteTarget.Unavailable) {
                _scannerEditor.update {
                    it?.copy(aiInterpretation = "IA Local indisponível: ${target.reason}")
                }
                return@launch
            }

            _scannerEditor.update { it?.copy(isInterpretingAi = true, aiInterpretation = "") }
            val prompt = buildString {
                appendLine("Analisa e interpreta em Português de Portugal o seguinte código digitalizado:")
                appendLine("- Formato: ${current.formatName}")
                appendLine("- Tipo detetado: ${current.interpretation.semanticType}")
                appendLine("- Conteúdo: ${current.editedValue}")
                appendLine("Explica claramente o que significa, valida possíveis riscos de segurança (phishing/URL suspeito/dados) e indica ações úteis.")
            }

            val acc = StringBuilder()
            try {
                coordinator.streamPrompt(
                    target = target,
                    prefs = prefs,
                    history = emptyList(),
                    prompt = prompt
                ).collect { chunk ->
                    acc.append(chunk)
                    _scannerEditor.update { it?.copy(aiInterpretation = acc.toString()) }
                }
                val savedId = _scannerEditor.value?.existingId
                if (savedId != null && acc.isNotEmpty()) {
                    historyRepository.updateScannedAiInterpretation(savedId, acc.toString())
                }
            } catch (e: Throwable) {
                _scannerEditor.update {
                    it?.copy(aiInterpretation = "Erro na interpretação IA: ${e.localizedMessage}")
                }
            } finally {
                _scannerEditor.update { it?.copy(isInterpretingAi = false) }
            }
        }
    }

    fun closeScannerEditor() {
        activeScanAiJob?.cancel()
        _scannerEditor.value = null
    }

    fun deleteScannedCode(item: ScannedCodeEntity) {
        viewModelScope.launch {
            historyRepository.deleteScannedCode(item)
        }
    }

    fun clearAllScannedCodes() {
        viewModelScope.launch {
            historyRepository.clearScannedCodes()
            _statusBanner.value = "Histórico de códigos apagado."
        }
    }

    fun getDeviceDiagnostics(): DeviceDiagnostics {
        val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val availGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        return DeviceDiagnostics(
            deviceName = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            totalRamGb = totalGb,
            availableRamGb = availGb,
            localIpv4 = lanClient.getDeviceLocalIpv4() ?: "Sem IP LAN (Offline)",
            modelsStorageBytes = downloadManager.getTotalStorageUsedBytes()
        )
    }

    override fun onCleared() {
        super.onCleared()
        liteRtManager.shutdown()
    }
}
