package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.LiteRtEngineState
import com.example.data.local.ActiveEngineMode
import com.example.data.local.ChatMessageEntity
import com.example.data.local.LanServerEntity
import com.example.ui.components.Button3DTone
import com.example.ui.components.EngineStatusPill
import com.example.ui.components.Interactive3DCard
import com.example.ui.components.Tactile3DButton
import com.example.ui.components.Tactile3DChipButton
import com.example.ui.components.Tactile3DMenuTile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    engineMode: ActiveEngineMode,
    engineState: LiteRtEngineState,
    activeLanServer: LanServerEntity?,
    messages: List<ChatMessageEntity>,
    isGenerating: Boolean,
    chatError: String?,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onClearChat: () -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onOpenModels: () -> Unit,
    onOpenLanSettings: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenAuthor: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var emptyPromptHint by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val isReady = if (engineMode == ActiveEngineMode.OFFLINE_LITERT) {
        engineState.isReady
    } else {
        activeLanServer != null
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
            // Top bar with Engine Pill & Clear Question History Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EngineStatusPill(
                    engineMode = engineMode,
                    engineState = engineState,
                    activeLanServer = activeLanServer,
                    onClick = onOpenModels,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(Modifier.weight(1f))

                if (messages.isNotEmpty()) {
                    Tactile3DChipButton(
                        text = "Apagar Histórico",
                        icon = Icons.Filled.DeleteSweep,
                        accentColor = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = onClearChat,
                        modifier = Modifier.testTag("clear_chat_button")
                    )
                }
            }

            if (chatError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = chatError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tactile3DChipButton(
                                text = "Abrir Modelos",
                                icon = Icons.Filled.Download,
                                accentColor = MaterialTheme.colorScheme.primary,
                                onClick = onOpenModels
                            )
                            Tactile3DChipButton(
                                text = "Rede Local",
                                icon = Icons.Filled.Lan,
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                onClick = onOpenLanSettings
                            )
                        }
                    }
                }
            } else if (emptyPromptHint) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Escreva a sua pergunta em baixo e toque em Enviar para perguntar à IA.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Messages or Enhanced 3D Studio Home
            if (messages.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Interactive3DCard(
                            glowColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_ai_studio_banner_1794936078179),
                                    contentDescription = "Painel Núcleo IA Local",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(176.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(176.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.background.copy(alpha = 0.14f),
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                                                )
                                            )
                                        )
                                )

                                // Floating top-left Studio 3D Badge
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = "IA LOCAL • PORTUGUÊS PT-PT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Núcleo IA • Offline & Rede Local",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Opere com Gemma 4 E2B diretamente no telemóvel ou ligue-se a modelos Ollama / LM Studio na sua rede local em Português.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Menu Rápido 3D",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Tactile3DMenuTile(
                                    title = "Câmara IA",
                                    subtitle = "Fotos & Vídeos Offline",
                                    icon = Icons.Filled.CameraAlt,
                                    tone = Button3DTone.CYAN,
                                    onClick = onOpenCamera,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_3d_camera")
                                )
                                Tactile3DMenuTile(
                                    title = "Modelos Offline",
                                    subtitle = "Gemma 4 E2B & LAN",
                                    icon = Icons.Filled.Memory,
                                    tone = Button3DTone.EMERALD,
                                    onClick = onOpenModels,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_3d_models")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Tactile3DMenuTile(
                                    title = "QR & Barras",
                                    subtitle = "Ler, Editar & Explicar",
                                    icon = Icons.Filled.QrCodeScanner,
                                    tone = Button3DTone.AMBER,
                                    onClick = onOpenScanner,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_3d_scanner")
                                )
                                Tactile3DMenuTile(
                                    title = "Menu do Autor",
                                    subtitle = "Duarte Cunha",
                                    icon = Icons.Filled.Person,
                                    tone = Button3DTone.VIOLET,
                                    onClick = onOpenAuthor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_3d_author")
                                )
                            }
                        }
                    }

                    if (!isReady) {
                        item {
                            Interactive3DCard(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                glowColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Configure o seu motor de IA",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Para começar offline, descarregue e carregue o Gemma 4 E2B (ou importe um ficheiro .litertlm). Em alternativa, ligue-se à sua rede local.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Tactile3DButton(
                                            text = "Modelos Offline",
                                            icon = Icons.Filled.Download,
                                            tone = Button3DTone.EMERALD,
                                            onClick = onOpenModels,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("empty_download_gemma_button")
                                        )
                                        Tactile3DButton(
                                            text = "Rede Local",
                                            icon = Icons.Filled.Lan,
                                            tone = Button3DTone.INDIGO,
                                            onClick = onOpenLanSettings,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("empty_configure_lan_button")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Sugestões rápidas em Português",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        val suggestions = listOf(
                            "Explica como funciona a IA offline no telemóvel",
                            "Resume as vantagens do Gemma 4 E2B em rede local",
                            "Escreve um email profissional em Português de Portugal",
                            "Cria uma lista de verificação para segurança Wi-Fi"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.forEachIndexed { idx, prompt ->
                                Tactile3DChipButton(
                                    text = prompt,
                                    icon = Icons.Filled.AutoAwesome,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    onClick = { onSendMessage(prompt) },
                                    modifier = Modifier.testTag("quick_prompt_$idx")
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(
                            message = msg,
                            onDelete = { onDeleteMessage(msg.id) }
                        )
                    }
                }
            }

            // Elevated Glassmorphic 3D Composer Bar
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            if (it.isNotBlank()) emptyPromptHint = false
                        },
                        placeholder = {
                            Text(
                                text = if (isReady) {
                                    "Escreva a sua pergunta em Português..."
                                } else {
                                    "Escreva a pergunta (carregue modelo ou LAN)..."
                                },
                                maxLines = 1
                            )
                        },
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isGenerating) {
                                    emptyPromptHint = false
                                    onSendMessage(inputText)
                                    if (isReady) {
                                        inputText = ""
                                    }
                                } else if (inputText.isBlank()) {
                                    emptyPromptHint = true
                                }
                            }
                        ),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("chat_input_field")
                    )

                    if (isGenerating) {
                        Tactile3DButton(
                            text = "Parar",
                            icon = Icons.Filled.StopCircle,
                            tone = Button3DTone.CORAL,
                            onClick = onStopGeneration,
                            modifier = Modifier
                                .width(108.dp)
                                .testTag("stop_chat_button")
                        )
                    } else {
                        Tactile3DButton(
                            text = "Enviar",
                            icon = Icons.AutoMirrored.Filled.Send,
                            tone = Button3DTone.CYAN,
                            enabled = true,
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    emptyPromptHint = false
                                    onSendMessage(inputText)
                                    if (isReady) {
                                        inputText = ""
                                        keyboardController?.hide()
                                    }
                                } else {
                                    emptyPromptHint = true
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            },
                            modifier = Modifier
                                .width(108.dp)
                                .testTag("send_chat_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessageEntity,
    onDelete: () -> Unit
) {
    val isUser = message.isUser
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isUser) 20.dp else 6.dp,
        bottomEnd = if (isUser) 6.dp else 20.dp
    )
    val accent = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
            },
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.24f), accent.copy(alpha = 0.22f))
                )
            ),
            modifier = Modifier.widthIn(max = 540.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isUser) "Pergunta" else message.engineLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(32.dp)
                            .testTag("delete_message_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Apagar mensagem",
                            tint = if (isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
