package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.ScannedCodeEntity
import com.example.ui.ScannedEditorState
import com.example.ui.components.Button3DTone
import com.example.ui.components.Interactive3DCard
import com.example.ui.components.Tactile3DButton
import com.example.ui.components.Tactile3DChipButton
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun ScannerScreen(
    editorState: ScannedEditorState?,
    history: List<ScannedCodeEntity>,
    flashEnabled: Boolean,
    onToggleFlash: (Boolean) -> Unit,
    onBarcodeDetected: (rawValue: String, formatCode: Int) -> Unit,
    onScanFromPhotoUri: (Uri) -> Unit,
    onOpenManualEditor: () -> Unit,
    onOpenSavedCode: (ScannedCodeEntity) -> Unit,
    onUpdatePayload: (String) -> Unit,
    onUpdateFormat: (Int) -> Unit,
    onUpdateMeta: (title: String, notes: String) -> Unit,
    onSaveEditedCode: () -> Unit,
    onInterpretWithAi: () -> Unit,
    onCloseEditor: () -> Unit,
    onDeleteSavedCode: (ScannedCodeEntity) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var scannerPaused by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPerm = granted
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onScanFromPhotoUri(uri)
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
                text = "Scanner QR & Códigos de Barras",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Digitalize códigos QR, EAN-13, Code 128 e mais. Edite o conteúdo, regenere o código e interprete com a IA Local.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            if (editorState != null) {
                BarcodeEditorPane(
                    state = editorState,
                    onUpdatePayload = onUpdatePayload,
                    onUpdateFormat = onUpdateFormat,
                    onUpdateMeta = onUpdateMeta,
                    onSave = onSaveEditedCode,
                    onInterpretAi = onInterpretWithAi,
                    onClose = {
                        scannerPaused = false
                        onCloseEditor()
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        if (hasCameraPerm) {
                            LiveBarcodeScannerBox(
                                paused = scannerPaused,
                                torchOn = flashEnabled,
                                onToggleTorch = { onToggleFlash(!flashEnabled) },
                                onBarcodeDetected = { raw, fmt ->
                                    scannerPaused = true
                                    onBarcodeDetected(raw, fmt)
                                }
                            )
                        } else {
                            Interactive3DCard(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                glowColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Câmara necessária para leitura ao vivo", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Autorize a câmara para apontar a códigos QR ou de barras, ou escolha uma foto / crie manualmente abaixo.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Tactile3DButton(
                                        text = "Ativar Leitor de Câmara",
                                        icon = Icons.Filled.QrCodeScanner,
                                        tone = Button3DTone.CYAN,
                                        onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("grant_scanner_camera_button")
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Tactile3DButton(
                                text = "Ler de Foto",
                                icon = Icons.Filled.PhotoLibrary,
                                tone = Button3DTone.EMERALD,
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scan_from_photo_button")
                            )
                            Tactile3DButton(
                                text = "Criar / Editar",
                                icon = Icons.Filled.Edit,
                                tone = Button3DTone.AMBER,
                                onClick = onOpenManualEditor,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_manual_code_button")
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Histórico Local (${history.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (history.isNotEmpty()) {
                                Tactile3DChipButton(
                                    text = "Apagar Tudo",
                                    icon = Icons.Filled.DeleteSweep,
                                    accentColor = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    onClick = onClearHistory,
                                    modifier = Modifier.testTag("clear_scanner_history_button")
                                )
                            }
                        }
                    }

                    if (history.isEmpty()) {
                        item {
                            Interactive3DCard(
                                containerColor = MaterialTheme.colorScheme.surface,
                                glowColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Ainda não digitalizou nenhum código QR ou de barras.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(history, key = { it.id }) { item ->
                            Interactive3DCard(
                                containerColor = MaterialTheme.colorScheme.surface,
                                glowColor = MaterialTheme.colorScheme.secondary,
                                onClick = { onOpenSavedCode(item) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scanned_history_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
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
                                                    text = item.formatName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = item.semanticType,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.editedValue,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteSavedCode(item) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Remover código",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBarcodeScannerBox(
    paused: Boolean,
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    onBarcodeDetected: (rawValue: String, formatCode: Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    DisposableEffect(lifecycleOwner, paused) {
        val executor = ContextCompat.getMainExecutor(context)
        controller.setImageAnalysisAnalyzer(
            executor,
            MlKitAnalyzer(
                listOf(barcodeScanner),
                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                executor
            ) { result ->
                if (!paused) {
                    val detected = result?.getValue(barcodeScanner)?.firstOrNull()
                    val raw = detected?.rawValue
                    if (!raw.isNullOrBlank()) {
                        onBarcodeDetected(raw, detected.format)
                    }
                }
            }
        )
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
        }
    }

    LaunchedEffect(torchOn) {
        controller.enableTorch(torchOn)
    }

    val reticleColor = MaterialTheme.colorScheme.primary
    val laserProgress by rememberInfiniteTransition(label = "laser").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPos"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(MaterialTheme.shapes.large)
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

        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxW = size.width * 0.66f
            val boxH = size.height * 0.56f
            val left = (size.width - boxW) / 2f
            val top = (size.height - boxH) / 2f
            drawRoundRect(
                color = reticleColor,
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 5f)
            )
            if (!paused) {
                val laserY = top + 12f + (boxH - 24f) * laserProgress
                drawLine(
                    color = reticleColor.copy(alpha = 0.85f),
                    start = Offset(left + 16f, laserY),
                    end = Offset(left + boxW - 16f, laserY),
                    strokeWidth = 4f
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("scanner_torch_button")
            ) {
                Icon(
                    imageVector = if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Lanterna do leitor"
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            Text(
                text = "Aponte para código QR ou de barras",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BarcodeEditorPane(
    state: ScannedEditorState,
    onUpdatePayload: (String) -> Unit,
    onUpdateFormat: (Int) -> Unit,
    onUpdateMeta: (title: String, notes: String) -> Unit,
    onSave: () -> Unit,
    onInterpretAi: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "${state.formatName} • ${state.interpretation.semanticType}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("close_barcode_editor")
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Voltar ao leitor")
            }
        }

        // Regenerated Barcode / QR preview
        state.generatedBitmap?.let { bmp ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Pré-visualização do código editado",
                    modifier = Modifier.size(180.dp)
                )
            }
        }

        // Format switcher chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val formats = listOf(
                Barcode.FORMAT_QR_CODE to "QR Code",
                Barcode.FORMAT_EAN_13 to "EAN-13",
                Barcode.FORMAT_CODE_128 to "Code 128",
                Barcode.FORMAT_DATA_MATRIX to "Data Matrix"
            )
            formats.forEach { (code, label) ->
                FilterChip(
                    selected = state.formatCode == code,
                    onClick = { onUpdateFormat(code) },
                    label = { Text(label) }
                )
            }
        }

        // Editable fields
        OutlinedTextField(
            value = state.title,
            onValueChange = { onUpdateMeta(it, state.notes) },
            label = { Text("Título / Identificação") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("barcode_title_input")
        )

        OutlinedTextField(
            value = state.editedValue,
            onValueChange = onUpdatePayload,
            label = { Text("Conteúdo Editável do Código") },
            minLines = 2,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("barcode_payload_input")
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = { onUpdateMeta(state.title, it) },
            label = { Text("Notas pessoais (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Action Buttons (Save, Copy, Share)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tactile3DButton(
                text = "Guardar",
                icon = Icons.Filled.Save,
                tone = Button3DTone.EMERALD,
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_barcode_button")
            )
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(state.editedValue)) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("copy_barcode_button")
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar conteúdo", modifier = Modifier.size(18.dp))
            }
            OutlinedButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, state.editedValue)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Partilhar código"))
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("share_barcode_button")
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Partilhar", modifier = Modifier.size(18.dp))
            }
        }

        // Deterministic Offline Interpretation Card
        Interactive3DCard(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            glowColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Interpretação Técnica Offline",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = state.interpretation.structuredSummary,
                    style = MaterialTheme.typography.bodySmall
                )
                state.interpretation.securityHint?.let { hint ->
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Local AI Interpretation
        Tactile3DButton(
            text = if (state.isInterpretingAi) "A interpretar com IA Local..." else "Interpretar com IA Local",
            icon = Icons.Filled.AutoAwesome,
            tone = Button3DTone.VIOLET,
            enabled = !state.isInterpretingAi,
            onClick = onInterpretAi,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("interpret_barcode_ai_button")
        )

        if (state.aiInterpretation.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Explicação da IA Local",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.aiInterpretation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
