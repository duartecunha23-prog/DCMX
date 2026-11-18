package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.UserAiPreferences
import com.example.data.local.VisionHistoryEntity
import com.example.ui.VisionChatMessage
import com.example.ui.components.Button3DTone
import com.example.ui.components.Interactive3DCard
import com.example.ui.components.Tactile3DButton
import com.example.ui.components.Tactile3DChipButton
import com.example.ui.components.Tactile3DMenuTile
import com.example.vision.PhotoVisionResult
import com.example.vision.VideoVisionResult
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraVisionScreen(
    preferences: UserAiPreferences,
    photoVision: PhotoVisionResult?,
    videoVision: VideoVisionResult?,
    isAnalyzingMedia: Boolean,
    visionError: String?,
    visionChat: List<VisionChatMessage>,
    isVisionAiBusy: Boolean,
    visionHistory: List<VisionHistoryEntity>,
    onAnalyzePhotoUri: (Uri) -> Unit,
    onAnalyzeCapturedBitmap: (Bitmap) -> Unit,
    onAnalyzeVideoUri: (Uri) -> Unit,
    onAskVisionAi: (String) -> Unit,
    onOpenVisionHistoryItem: (VisionHistoryEntity) -> Unit,
    onDeleteVisionHistoryItem: (VisionHistoryEntity) -> Unit,
    onClearVisionHistory: () -> Unit,
    onSetCameraUseFront: (Boolean) -> Unit,
    onSetCameraFlashEnabled: (Boolean) -> Unit,
    onSetCameraLiveVision: (Boolean) -> Unit,
    onSetCameraRecordAudio: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 0 = Menu 3D da Máquina, 1 = Câmara Ao Vivo, 2 = Reconhecimento IA Offline
    var selectedMode by remember { mutableIntStateOf(0) }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPerm = granted
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAnalyzePhotoUri(uri)
            selectedMode = 2
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAnalyzeVideoUri(uri)
            selectedMode = 2
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
                text = "Máquina Fotográfica & IA Offline",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Menu 3D tátil para fotografar, gravar vídeo e interagir no reconhecimento offline de fotos e vídeos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 3),
                    icon = { Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("camera_tab_menu")
                ) {
                    Text("Menu 3D")
                }
                SegmentedButton(
                    selected = selectedMode == 1,
                    onClick = { selectedMode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 3),
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("camera_tab_live")
                ) {
                    Text("Câmara")
                }
                SegmentedButton(
                    selected = selectedMode == 2,
                    onClick = { selectedMode = 2 },
                    shape = SegmentedButtonDefaults.itemShape(2, 3),
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("camera_tab_analysis")
                ) {
                    Text("Visão IA")
                }
            }

            Spacer(Modifier.height(12.dp))

            when (selectedMode) {
                0 -> {
                    Camera3DMenuHub(
                        preferences = preferences,
                        hasResult = photoVision != null || videoVision != null,
                        visionHistory = visionHistory,
                        onOpenPhotoCamera = {
                            onSetCameraLiveVision(false)
                            selectedMode = 1
                        },
                        onOpenVideoCamera = {
                            onSetCameraLiveVision(false)
                            selectedMode = 1
                        },
                        onOpenLiveVision = {
                            onSetCameraLiveVision(true)
                            selectedMode = 1
                        },
                        onPickPhoto = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPickVideo = {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        onOpenAiChat = { selectedMode = 2 },
                        onOpenHistoryItem = { item ->
                            onOpenVisionHistoryItem(item)
                            selectedMode = 2
                        },
                        onDeleteHistoryItem = onDeleteVisionHistoryItem,
                        onClearVisionHistory = onClearVisionHistory,
                        onSetCameraUseFront = onSetCameraUseFront,
                        onSetCameraFlashEnabled = onSetCameraFlashEnabled,
                        onSetCameraLiveVision = onSetCameraLiveVision,
                        onSetCameraRecordAudio = onSetCameraRecordAudio,
                        modifier = Modifier.weight(1f)
                    )
                }

                1 -> {
                    if (!hasCameraPerm) {
                        Interactive3DCard(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            glowColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Permissão de Câmara",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "Acesso à Máquina Fotográfica",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Autorize a câmara para fotografar, gravar vídeos e reconhecer objetos em tempo real offline.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Tactile3DButton(
                                    text = "Ativar Câmara Agora",
                                    icon = Icons.Filled.CameraAlt,
                                    tone = Button3DTone.CYAN,
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("grant_camera_permission_button")
                                )
                            }
                        }
                    } else {
                        LiveCameraCapturePane(
                            liveAnalysisEnabled = preferences.cameraLiveVision,
                            isFrontCamera = preferences.cameraUseFront,
                            flashEnabled = preferences.cameraFlashEnabled,
                            recordAudioEnabled = preferences.cameraRecordAudio,
                            onToggleLiveAnalysis = { onSetCameraLiveVision(!preferences.cameraLiveVision) },
                            onToggleFrontCamera = { onSetCameraUseFront(!preferences.cameraUseFront) },
                            onToggleFlash = { onSetCameraFlashEnabled(!preferences.cameraFlashEnabled) },
                            onPhotoCaptured = { bmp ->
                                onAnalyzeCapturedBitmap(bmp)
                                selectedMode = 2
                            },
                            onVideoRecorded = { uri ->
                                onAnalyzeVideoUri(uri)
                                selectedMode = 2
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 12.dp)
                        )
                    }
                }

                else -> {
                    VisionAnalysisWorkspace(
                        photoVision = photoVision,
                        videoVision = videoVision,
                        isAnalyzingMedia = isAnalyzingMedia,
                        visionError = visionError,
                        visionChat = visionChat,
                        isVisionAiBusy = isVisionAiBusy,
                        onPickPhoto = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPickVideo = {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        onAskVisionAi = onAskVisionAi,
                        onClearVisionHistory = onClearVisionHistory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Camera3DMenuHub(
    preferences: UserAiPreferences,
    hasResult: Boolean,
    visionHistory: List<VisionHistoryEntity>,
    onOpenPhotoCamera: () -> Unit,
    onOpenVideoCamera: () -> Unit,
    onOpenLiveVision: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenHistoryItem: (VisionHistoryEntity) -> Unit,
    onDeleteHistoryItem: (VisionHistoryEntity) -> Unit,
    onClearVisionHistory: () -> Unit,
    onSetCameraUseFront: (Boolean) -> Unit,
    onSetCameraFlashEnabled: (Boolean) -> Unit,
    onSetCameraLiveVision: (Boolean) -> Unit,
    onSetCameraRecordAudio: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Controlo 3D da Máquina Fotográfica",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Tactile3DMenuTile(
                    title = "Fotografar",
                    subtitle = "Câmara + IA Offline",
                    icon = Icons.Filled.CameraAlt,
                    tone = Button3DTone.CYAN,
                    onClick = onOpenPhotoCamera,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu3d_take_photo")
                )
                Tactile3DMenuTile(
                    title = "Gravar Vídeo",
                    subtitle = "Capturar & Analisar",
                    icon = Icons.Filled.Videocam,
                    tone = Button3DTone.CORAL,
                    onClick = onOpenVideoCamera,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu3d_record_video")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Tactile3DMenuTile(
                    title = "Analisar Foto",
                    subtitle = "Escolher da Galeria",
                    icon = Icons.Filled.PhotoLibrary,
                    tone = Button3DTone.EMERALD,
                    onClick = onPickPhoto,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_photo_button")
                )
                Tactile3DMenuTile(
                    title = "Analisar Vídeo",
                    subtitle = "Fotogramas + OCR",
                    icon = Icons.Filled.VideoLibrary,
                    tone = Button3DTone.VIOLET,
                    onClick = onPickVideo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_video_button")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Tactile3DMenuTile(
                    title = "Visão Ao Vivo",
                    subtitle = "Reconhecimento Real",
                    icon = Icons.Filled.RemoveRedEye,
                    tone = Button3DTone.AMBER,
                    onClick = onOpenLiveVision,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu3d_live_vision")
                )
                Tactile3DMenuTile(
                    title = "Interagir IA",
                    subtitle = if (hasResult) "Ver Análise Atual" else "Perguntar Offline",
                    icon = Icons.Filled.AutoAwesome,
                    tone = Button3DTone.INDIGO,
                    onClick = onOpenAiChat,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu3d_ai_workspace")
                )
            }
        }

        item {
            Interactive3DCard(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                glowColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Reconhecimento Visual 100% Offline",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "As fotografias e fotogramas de vídeo são analisados localmente no telemóvel (objetos, cores dominantes, luminosidade e texto OCR). Quando carrega o Gemma 4 E2B, pode conversar livremente sobre cada detalhe em Português sem internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Interactive3DCard(
                containerColor = MaterialTheme.colorScheme.surface,
                glowColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Configurações da Câmara (DataStore)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    CameraPrefSwitchRow(
                        label = "Câmara Frontal por defeito",
                        checked = preferences.cameraUseFront,
                        onCheckedChange = onSetCameraUseFront,
                        testTag = "pref_camera_front"
                    )
                    CameraPrefSwitchRow(
                        label = "Flash / Lanterna ativo",
                        checked = preferences.cameraFlashEnabled,
                        onCheckedChange = onSetCameraFlashEnabled,
                        testTag = "pref_camera_flash"
                    )
                    CameraPrefSwitchRow(
                        label = "Visão Offline Ao Vivo",
                        checked = preferences.cameraLiveVision,
                        onCheckedChange = onSetCameraLiveVision,
                        testTag = "pref_camera_live_vision"
                    )
                    CameraPrefSwitchRow(
                        label = "Gravar áudio nos vídeos",
                        checked = preferences.cameraRecordAudio,
                        onCheckedChange = onSetCameraRecordAudio,
                        testTag = "pref_camera_audio"
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Imagens Capturadas Guardadas (${visionHistory.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (visionHistory.isNotEmpty()) {
                    Tactile3DChipButton(
                        text = "Apagar Tudo",
                        icon = Icons.Filled.DeleteSweep,
                        accentColor = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = onClearVisionHistory,
                        modifier = Modifier.testTag("clear_saved_vision_history_button")
                    )
                }
            }
        }

        if (visionHistory.isEmpty()) {
            item {
                Interactive3DCard(
                    containerColor = MaterialTheme.colorScheme.surface,
                    glowColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "As fotografias capturadas e imagens analisadas são guardadas automaticamente no armazenamento local (Room).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(visionHistory, key = { it.id }) { item ->
                SavedVisionHistoryCard(
                    item = item,
                    onClick = { onOpenHistoryItem(item) },
                    onDelete = { onDeleteHistoryItem(item) }
                )
            }
        }
    }
}

@Composable
private fun SavedVisionHistoryCard(
    item: VisionHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val thumbBitmap by produceState<Bitmap?>(initialValue = null, item.mediaUri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val parsed = Uri.parse(item.mediaUri)
                if (parsed.scheme == "file") {
                    val path = parsed.path ?: return@runCatching null
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(path, opts)
                } else if (item.mediaType == "PHOTO") {
                    context.contentResolver.openInputStream(parsed)?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                } else {
                    null
                }
            }.getOrNull()
        }
    }
    val timeFormatted = remember(item.timestamp) {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    }

    Interactive3DCard(
        containerColor = MaterialTheme.colorScheme.surface,
        glowColor = MaterialTheme.colorScheme.secondary,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vision_history_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bmp = thumbBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Miniatura capturada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (item.mediaType == "VIDEO") Icons.Filled.Videocam else Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (item.mediaType == "VIDEO") "VÍDEO" else "FOTO LOCAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.detectedLabels.ifBlank { "Imagem capturada offline" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.extractedText.isNotBlank()) {
                    Text(
                        text = item.extractedText.replace("\n", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_vision_history_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remover imagem guardada",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CameraPrefSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun LiveCameraCapturePane(
    liveAnalysisEnabled: Boolean,
    isFrontCamera: Boolean,
    flashEnabled: Boolean,
    recordAudioEnabled: Boolean,
    onToggleLiveAnalysis: () -> Unit,
    onToggleFrontCamera: () -> Unit,
    onToggleFlash: () -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit,
    onVideoRecorded: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isRecording by remember { mutableStateOf(false) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var liveLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var liveOcrSnippet by remember { mutableStateOf("") }

    val labeler = remember {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder().setConfidenceThreshold(0.55f).build()
        )
    }
    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                    CameraController.VIDEO_CAPTURE or
                    CameraController.IMAGE_ANALYSIS
            )
        }
    }

    LaunchedEffect(isFrontCamera, flashEnabled) {
        controller.cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        controller.enableTorch(flashEnabled && !isFrontCamera)
    }

    DisposableEffect(lifecycleOwner, liveAnalysisEnabled, isRecording) {
        val executor = ContextCompat.getMainExecutor(context)
        if (liveAnalysisEnabled && !isRecording) {
            controller.setImageAnalysisAnalyzer(
                executor,
                MlKitAnalyzer(
                    listOf(labeler, textRecognizer),
                    CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    executor
                ) { result ->
                    val labels = result?.getValue(labeler)
                        ?.sortedByDescending { it.confidence }
                        ?.take(4)
                        ?.map { "${it.text} ${(it.confidence * 100).toInt()}%" }
                        .orEmpty()
                    val text = result?.getValue(textRecognizer)?.text?.trim().orEmpty()
                    liveLabels = labels
                    liveOcrSnippet = text.replace("\n", " ").take(64)
                }
            )
        } else {
            controller.clearImageAnalysisAnalyzer()
        }
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            activeRecording?.stop()
            controller.unbind()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.controller = controller
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Live Offline Recognition HUD Overlay
        if (liveAnalysisEnabled && (liveLabels.isNotEmpty() || liveOcrSnippet.isNotBlank())) {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .widthIn(max = 260.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Visão Offline Ao Vivo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (liveLabels.isNotEmpty()) {
                        Text(
                            text = liveLabels.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (liveOcrSnippet.isNotBlank()) {
                        Text(
                            text = "Texto: \"$liveOcrSnippet\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Top right camera controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (liveAnalysisEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
                }
            ) {
                IconButton(
                    onClick = onToggleLiveAnalysis,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("camera_live_vision_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RemoveRedEye,
                        contentDescription = "Alternar Visão Offline Ao Vivo"
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
            ) {
                IconButton(
                    onClick = onToggleFlash,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("camera_flash_button")
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Alternar Flash",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
            ) {
                IconButton(
                    onClick = onToggleFrontCamera,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("camera_flip_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = "Trocar Câmara",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        if (captureError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text(
                    text = captureError!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Bottom 3D Tactile Shutter & Video Bar
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Tactile3DButton(
                    text = "Fotografar",
                    icon = Icons.Filled.CameraAlt,
                    tone = Button3DTone.CYAN,
                    enabled = !isRecording,
                    onClick = {
                        captureError = null
                        controller.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val rotated = imageProxyToRotatedBitmap(image)
                                    image.close()
                                    onPhotoCaptured(rotated)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    captureError = "Erro na fotografia: ${exception.localizedMessage}"
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("capture_photo_button")
                )

                Tactile3DButton(
                    text = if (isRecording) "Parar & Analisar" else "Gravar Vídeo",
                    icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.Videocam,
                    tone = if (isRecording) Button3DTone.AMBER else Button3DTone.CORAL,
                    onClick = {
                        if (isRecording) {
                            activeRecording?.stop()
                            activeRecording = null
                            isRecording = false
                        } else {
                            captureError = null
                            val videoFile = File(context.cacheDir, "cam_${System.currentTimeMillis()}.mp4")
                            val outOpts = FileOutputOptions.Builder(videoFile).build()
                            val hasAudio = recordAudioEnabled && ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            val audioCfg = if (hasAudio) {
                                AudioConfig.create(true)
                            } else {
                                AudioConfig.AUDIO_DISABLED
                            }

                            isRecording = true
                            activeRecording = controller.startRecording(
                                outOpts,
                                audioCfg,
                                ContextCompat.getMainExecutor(context)
                            ) { event ->
                                when (event) {
                                    is VideoRecordEvent.Finalize -> {
                                        isRecording = false
                                        activeRecording = null
                                        if (!event.hasError()) {
                                            onVideoRecorded(Uri.fromFile(videoFile))
                                        } else {
                                            captureError = "Erro vídeo (${event.error})"
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_video_button")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VisionAnalysisWorkspace(
    photoVision: PhotoVisionResult?,
    videoVision: VideoVisionResult?,
    isAnalyzingMedia: Boolean,
    visionError: String?,
    visionChat: List<VisionChatMessage>,
    isVisionAiBusy: Boolean,
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
    onAskVisionAi: (String) -> Unit,
    onClearVisionHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var question by remember { mutableStateOf("") }
    val visionFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxSize()) {
        // Quick 3D media switcher buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Tactile3DButton(
                text = "Nova Foto",
                icon = Icons.Filled.PhotoLibrary,
                tone = Button3DTone.EMERALD,
                onClick = onPickPhoto,
                modifier = Modifier.weight(1f)
            )
            Tactile3DButton(
                text = "Novo Vídeo",
                icon = Icons.Filled.VideoLibrary,
                tone = Button3DTone.VIOLET,
                onClick = onPickVideo,
                modifier = Modifier.weight(1f)
            )
        }

        if (visionChat.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Tactile3DChipButton(
                    text = "Apagar Perguntas",
                    icon = Icons.Filled.DeleteSweep,
                    accentColor = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = onClearVisionHistory,
                    modifier = Modifier.testTag("clear_vision_chat_button")
                )
            }
        }

        if (isAnalyzingMedia) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator()
                    Text("A extrair objetos, cores e texto OCR offline...")
                }
            }
        } else if (photoVision == null && videoVision == null) {
            Interactive3DCard(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                glowColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Nenhuma foto ou vídeo carregado", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Use os botões 3D acima ou o Menu 3D da Máquina Fotográfica para fotografar, gravar vídeo ou carregar da galeria.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (photoVision != null) {
                    item {
                        photoVision.previewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Fotografia analisada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(MaterialTheme.shapes.large)
                            )
                        }
                    }
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (photoVision.brightnessLabel.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${photoVision.brightnessLabel} • ${photoVision.resolutionLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            photoVision.dominantColors.forEach { clr ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "Cor: $clr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            photoVision.labels.forEach { lbl ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${lbl.text} ${(lbl.confidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (videoVision != null) {
                    item {
                        Text(
                            text = "Fotogramas extraídos (${videoVision.keyframes.size} frames • ${"%.1f".format(videoVision.durationMs / 1000.0)}s)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(videoVision.keyframes) { frame ->
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.width(136.dp)) {
                                        Image(
                                            bitmap = frame.bitmap.asImageBitmap(),
                                            contentDescription = "Fotograma vídeo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(86.dp)
                                        )
                                        Text(
                                            text = "${"%.1f".format(frame.timestampMs / 1000.0)}s • ${frame.labels.firstOrNull()?.text ?: "Cena"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (visionError != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = visionError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                item {
                    val quickQuestions = if (videoVision != null) {
                        listOf(
                            "Resume o que acontece neste vídeo",
                            "Que objetos e texto aparecem?"
                        )
                    } else {
                        listOf(
                            "Descreve o que está nesta foto",
                            "Quais as cores e iluminação?",
                            "Lê o texto visível na imagem"
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickQuestions.forEach { q ->
                            Tactile3DChipButton(
                                text = q,
                                icon = Icons.Filled.AutoAwesome,
                                onClick = { onAskVisionAi(q) }
                            )
                        }
                    }
                }

                items(visionChat) { msg ->
                    Surface(
                        color = if (msg.isUser) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (msg.isUser) "Pergunta" else "Análise IA Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text("Pergunte à IA Offline sobre a foto ou vídeo...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(visionFocusRequester)
                        .testTag("vision_question_input")
                )
                Tactile3DButton(
                    text = if (isVisionAiBusy) "..." else "Perguntar",
                    icon = Icons.AutoMirrored.Filled.Send,
                    tone = Button3DTone.CYAN,
                    enabled = !isVisionAiBusy,
                    onClick = {
                        if (question.isNotBlank()) {
                            onAskVisionAi(question)
                            question = ""
                            keyboardController?.hide()
                        } else {
                            visionFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    modifier = Modifier
                        .width(126.dp)
                        .testTag("vision_ask_button")
                )
            }
        }
    }
}

private fun imageProxyToRotatedBitmap(image: ImageProxy): Bitmap {
    val raw = image.toBitmap()
    val rotation = image.imageInfo.rotationDegrees
    if (rotation == 0) return raw
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
}
