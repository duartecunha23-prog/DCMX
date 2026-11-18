package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ai.DiscoveredLanNode
import com.example.ai.LiteRtEngineState
import com.example.data.local.ActiveEngineMode
import com.example.data.local.AiBackendPreference
import com.example.data.local.LanProtocol
import com.example.data.local.LanServerEntity
import com.example.data.local.LocalModelEntity
import com.example.data.local.ModelStatus
import com.example.ui.components.Button3DTone
import com.example.ui.components.Interactive3DCard
import com.example.ui.components.Tactile3DButton
import com.example.ui.components.formatBytes

@Composable
fun ModelsScreen(
    initialTab: Int,
    engineMode: ActiveEngineMode,
    engineState: LiteRtEngineState,
    models: List<LocalModelEntity>,
    lanServers: List<LanServerEntity>,
    isDiscoveringLan: Boolean,
    discoveredNodes: List<DiscoveredLanNode>,
    lanProbeStatus: String?,
    onSetEngineMode: (ActiveEngineMode) -> Unit,
    onStartDownload: (LocalModelEntity) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteModel: (LocalModelEntity) -> Unit,
    onToggleBackend: (LocalModelEntity) -> Unit,
    onLoadOfflineModel: (LocalModelEntity) -> Unit,
    onUnloadOfflineModel: () -> Unit,
    onAddCustomModel: (name: String, repoOrUrl: String, fileName: String, sizeMb: Long) -> Unit,
    onImportLocalFile: (Uri) -> Unit,
    onSaveAndActivateLan: (id: Long?, name: String, url: String, protocol: LanProtocol, model: String) -> Unit,
    onActivateLanServer: (LanServerEntity) -> Unit,
    onProbeLanServer: (LanServerEntity) -> Unit,
    onSelectLanModel: (LanServerEntity, String) -> Unit,
    onDeleteLanServer: (LanServerEntity) -> Unit,
    onDiscoverLanNodes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showCustomModelDialog by remember { mutableStateOf(false) }

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportLocalFile(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Modelos & Rede Local",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Descarregue Gemma 4 E2B e outros modelos .litertlm para operar offline, ou ligue-se a um servidor de IA na rede local.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onSetEngineMode(ActiveEngineMode.OFFLINE_LITERT)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Filled.Memory, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_offline_models")
                ) {
                    Text("Modelos Offline")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onSetEngineMode(ActiveEngineMode.LOCAL_NETWORK_LAN)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Filled.Lan, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_lan_network")
                ) {
                    Text("Rede Local (LAN)")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (selectedTab == 0) {
                OfflineModelsTab(
                    engineState = engineState,
                    models = models,
                    onStartDownload = onStartDownload,
                    onCancelDownload = onCancelDownload,
                    onDeleteModel = onDeleteModel,
                    onToggleBackend = onToggleBackend,
                    onLoadOfflineModel = onLoadOfflineModel,
                    onUnloadOfflineModel = onUnloadOfflineModel,
                    onOpenAddCustom = { showCustomModelDialog = true },
                    onImportFile = { importPicker.launch(arrayOf("*/*")) }
                )
            } else {
                LanNetworkTab(
                    lanServers = lanServers,
                    isDiscoveringLan = isDiscoveringLan,
                    discoveredNodes = discoveredNodes,
                    lanProbeStatus = lanProbeStatus,
                    onSaveAndActivateLan = onSaveAndActivateLan,
                    onActivateLanServer = onActivateLanServer,
                    onProbeLanServer = onProbeLanServer,
                    onSelectLanModel = onSelectLanModel,
                    onDeleteLanServer = onDeleteLanServer,
                    onDiscoverLanNodes = onDiscoverLanNodes
                )
            }
        }
    }

    if (showCustomModelDialog) {
        AddCustomModelDialog(
            onDismiss = { showCustomModelDialog = false },
            onConfirm = { name, repoOrUrl, fileName, sizeMb ->
                onAddCustomModel(name, repoOrUrl, fileName, sizeMb)
                showCustomModelDialog = false
            }
        )
    }
}

@Composable
private fun OfflineModelsTab(
    engineState: LiteRtEngineState,
    models: List<LocalModelEntity>,
    onStartDownload: (LocalModelEntity) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteModel: (LocalModelEntity) -> Unit,
    onToggleBackend: (LocalModelEntity) -> Unit,
    onLoadOfflineModel: (LocalModelEntity) -> Unit,
    onUnloadOfflineModel: () -> Unit,
    onOpenAddCustom: () -> Unit,
    onImportFile: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenAddCustom,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("add_custom_model_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Adicionar URL / HF")
                }
                OutlinedButton(
                    onClick = onImportFile,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("import_litertlm_button")
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Importar .litertlm")
                }
            }
        }

        if (engineState.isLoading || engineState.isReady || engineState.errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (engineState.isReady) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else if (engineState.errorMessage != null) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    engineState.isLoading -> "A inicializar ${engineState.loadedModelName}..."
                                    engineState.isReady -> "Motor Ativo: ${engineState.loadedModelName} (${engineState.activeBackend?.name})"
                                    else -> "Erro no motor: ${engineState.errorMessage}"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        if (engineState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else if (engineState.isReady) {
                            TextButton(
                                onClick = onUnloadOfflineModel,
                                modifier = Modifier.testTag("unload_model_button")
                            ) {
                                Text("Descarregar RAM")
                            }
                        }
                    }
                }
            }
        }

        items(models, key = { it.id }) { model ->
            OfflineModelCard(
                model = model,
                isLoaded = engineState.isReady && engineState.loadedModelId == model.id,
                isLoading = engineState.isLoading && engineState.loadedModelId == model.id,
                onStartDownload = { onStartDownload(model) },
                onCancelDownload = { onCancelDownload(model.id) },
                onDelete = { onDeleteModel(model) },
                onToggleBackend = { onToggleBackend(model) },
                onLoad = { onLoadOfflineModel(model) }
            )
        }
    }
}

@Composable
private fun OfflineModelCard(
    model: LocalModelEntity,
    isLoaded: Boolean,
    isLoading: Boolean,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit,
    onToggleBackend: () -> Unit,
    onLoad: () -> Unit
) {
    val status = runCatching { ModelStatus.valueOf(model.status) }.getOrDefault(ModelStatus.AVAILABLE)
    val isReadyOnDisk = status == ModelStatus.DOWNLOADED || status == ModelStatus.IMPORTED

    Interactive3DCard(
        glowColor = if (model.isFeatured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("model_card_${model.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = model.name, style = MaterialTheme.typography.titleMedium)
                        if (model.isFeatured) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "RECOMENDADO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${model.repoId} • ${formatBytes(model.expectedSizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isReadyOnDisk) {
                    FilterChip(
                        selected = model.backendPreference == AiBackendPreference.GPU.name,
                        onClick = onToggleBackend,
                        label = { Text(model.backendPreference) },
                        leadingIcon = {
                            Icon(Icons.Filled.Speed, contentDescription = "Alternar CPU/GPU", modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("backend_toggle_${model.id}")
                    )
                }
            }

            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!model.errorMessage.isNullOrBlank()) {
                Text(
                    text = model.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            when (status) {
                ModelStatus.DOWNLOADING -> {
                    val progress = if (model.expectedSizeBytes > 0L) {
                        (model.downloadedBytes.toFloat() / model.expectedSizeBytes.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatBytes(model.downloadedBytes)} / ${formatBytes(model.expectedSizeBytes)} (${(progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall
                        )
                        TextButton(
                            onClick = onCancelDownload,
                            modifier = Modifier.testTag("cancel_download_${model.id}")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Cancelar")
                        }
                    }
                }

                ModelStatus.DOWNLOADED, ModelStatus.IMPORTED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onLoad,
                            enabled = !isLoaded && !isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("load_model_${model.id}")
                        ) {
                            Icon(
                                imageVector = if (isLoaded) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(
                                when {
                                    isLoading -> "A carregar..."
                                    isLoaded -> "Carregado e Ativo"
                                    else -> "Carregar Offline"
                                }
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("delete_model_${model.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Eliminar modelo local",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                ModelStatus.AVAILABLE, ModelStatus.ERROR -> {
                    Tactile3DButton(
                        text = if (status == ModelStatus.ERROR) "Tentar Novamente" else "Descarregar para Offline",
                        icon = Icons.Filled.Download,
                        tone = if (model.isFeatured) Button3DTone.CYAN else Button3DTone.INDIGO,
                        onClick = onStartDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_model_${model.id}")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanNetworkTab(
    lanServers: List<LanServerEntity>,
    isDiscoveringLan: Boolean,
    discoveredNodes: List<DiscoveredLanNode>,
    lanProbeStatus: String?,
    onSaveAndActivateLan: (id: Long?, name: String, url: String, protocol: LanProtocol, model: String) -> Unit,
    onActivateLanServer: (LanServerEntity) -> Unit,
    onProbeLanServer: (LanServerEntity) -> Unit,
    onSelectLanModel: (LanServerEntity, String) -> Unit,
    onDeleteLanServer: (LanServerEntity) -> Unit,
    onDiscoverLanNodes: () -> Unit
) {
    var nameInput by remember { mutableStateOf("Servidor IA Local") }
    var urlInput by remember { mutableStateOf("http://192.168.1.100:11434") }
    var modelInput by remember { mutableStateOf("gemma4") }
    var protocol by remember { mutableStateOf(LanProtocol.OLLAMA) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Configurar Servidor na Rede Local (Wi-Fi)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Opere modelos na sua rede doméstica ou escritório (Ollama, LM Studio, llama.cpp) sem depender de internet externa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = protocol == LanProtocol.OLLAMA,
                            onClick = {
                                protocol = LanProtocol.OLLAMA
                                if (urlInput.endsWith(":1234")) urlInput = urlInput.replace(":1234", ":11434")
                            },
                            label = { Text("Ollama (:11434)") },
                            modifier = Modifier.testTag("protocol_ollama")
                        )
                        FilterChip(
                            selected = protocol == LanProtocol.OPENAI_COMPATIBLE,
                            onClick = {
                                protocol = LanProtocol.OPENAI_COMPATIBLE
                                if (urlInput.endsWith(":11434")) urlInput = urlInput.replace(":11434", ":1234")
                            },
                            label = { Text("OpenAI / LM Studio") },
                            modifier = Modifier.testTag("protocol_openai")
                        )
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nome do Servidor") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lan_name_input")
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Endereço IP / URL na Rede Local") },
                        placeholder = { Text("http://192.168.1.100:11434") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lan_url_input")
                    )
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("Nome do Modelo no Servidor (ex: gemma4, gemma3:4b)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lan_model_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Tactile3DButton(
                            text = "Guardar & Ativar",
                            icon = Icons.Filled.CheckCircle,
                            tone = Button3DTone.EMERALD,
                            onClick = {
                                onSaveAndActivateLan(null, nameInput, urlInput, protocol, modelInput)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_lan_server_button")
                        )
                        Tactile3DButton(
                            text = if (isDiscoveringLan) "A procurar..." else "Procurar LAN",
                            icon = Icons.Filled.Radar,
                            tone = Button3DTone.AMBER,
                            enabled = !isDiscoveringLan,
                            onClick = onDiscoverLanNodes,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scan_lan_button")
                        )
                    }

                    if (!lanProbeStatus.isNullOrBlank()) {
                        Text(
                            text = lanProbeStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (discoveredNodes.isNotEmpty()) {
            item {
                Text(
                    text = "Nós Detetados na Rede Wi-Fi",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            items(discoveredNodes, key = { it.baseUrl }) { node ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(node.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${node.baseUrl} • ${node.latencyMs}ms • ${node.models.size} modelos",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Button(
                            onClick = {
                                onSaveAndActivateLan(
                                    null,
                                    node.label,
                                    node.baseUrl,
                                    node.protocol,
                                    node.models.firstOrNull() ?: "gemma4"
                                )
                            },
                            modifier = Modifier.testTag("use_discovered_${node.baseUrl}")
                        ) {
                            Text("Usar")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Servidores Guardados",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(lanServers, key = { it.id }) { server ->
            val availableModels = remember(server.availableModelsCsv) {
                server.availableModelsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (server.isActive) {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(server.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${server.baseUrl} • ${server.protocol}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Modelo ativo: ${server.selectedModel}" +
                                    (server.lastLatencyMs?.let { " • ${it}ms" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { onDeleteLanServer(server) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remover servidor")
                        }
                    }

                    if (availableModels.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableModels.forEach { m ->
                                FilterChip(
                                    selected = m == server.selectedModel,
                                    onClick = { onSelectLanModel(server, m) },
                                    label = { Text(m, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onProbeLanServer(server) },
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        ) {
                            Text("Testar & Listar Modelos")
                        }
                        Button(
                            onClick = { onActivateLanServer(server) },
                            enabled = !server.isActive,
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        ) {
                            Text(if (server.isActive) "Ativo" else "Ativar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomModelDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, repoOrUrl: String, fileName: String, sizeMb: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var repoOrUrl by remember { mutableStateOf("litert-community/") }
    var fileName by remember { mutableStateOf("model.litertlm") }
    var sizeMb by remember { mutableStateOf("1500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Descarregar Outro Modelo (.litertlm)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Indique um repositório Hugging Face (ex: litert-community/Gemma3-1B-IT) ou URL direto para um ficheiro .litertlm.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome de apresentação") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = repoOrUrl,
                    onValueChange = { repoOrUrl = it },
                    label = { Text("Repositório HF ou URL completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Nome do ficheiro .litertlm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sizeMb,
                    onValueChange = { sizeMb = it.filter { c -> c.isDigit() } },
                    label = { Text("Tamanho estimado (MB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, repoOrUrl, fileName, sizeMb.toLongOrNull() ?: 1000L)
                },
                enabled = repoOrUrl.isNotBlank()
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
