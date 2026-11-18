package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.LiteRtEngineState
import com.example.data.local.ActiveEngineMode
import com.example.data.local.LanServerEntity
import com.example.ui.theme.Button3DAmber
import com.example.ui.theme.Button3DAmberBase
import com.example.ui.theme.Button3DCoral
import com.example.ui.theme.Button3DCoralBase
import com.example.ui.theme.Button3DCyan
import com.example.ui.theme.Button3DCyanBase
import com.example.ui.theme.Button3DEmerald
import com.example.ui.theme.Button3DEmeraldBase
import com.example.ui.theme.Button3DIndigo
import com.example.ui.theme.Button3DIndigoBase
import com.example.ui.theme.Button3DViolet
import com.example.ui.theme.Button3DVioletBase

@Composable
fun NeuralDepthBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    val tertiaryGlow = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
    val gridLine = MaterialTheme.colorScheme.outline.copy(alpha = 0.11f)
    val nodeGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .drawBehind {
                // Multi-point studio aurora lighting
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryGlow, Color.Transparent),
                        center = Offset(size.width * 0.14f, size.height * 0.10f),
                        radius = size.maxDimension * 0.52f
                    ),
                    radius = size.maxDimension * 0.52f,
                    center = Offset(size.width * 0.14f, size.height * 0.10f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryGlow, Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.65f),
                        radius = size.maxDimension * 0.44f
                    ),
                    radius = size.maxDimension * 0.44f,
                    center = Offset(size.width * 0.88f, size.height * 0.65f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiaryGlow, Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.92f),
                        radius = size.maxDimension * 0.36f
                    ),
                    radius = size.maxDimension * 0.36f,
                    center = Offset(size.width * 0.5f, size.height * 0.92f)
                )

                // Architectural perspective floor grid near bottom
                val horizonY = size.height * 0.74f
                val centerX = size.width * 0.5f
                for (i in -5..5) {
                    val bottomX = centerX + i * (size.width * 0.22f)
                    drawLine(
                        color = gridLine,
                        start = Offset(centerX + i * 24f, horizonY),
                        end = Offset(bottomX, size.height),
                        strokeWidth = 1f
                    )
                }
                val depthSteps = listOf(0.06f, 0.14f, 0.26f, 0.44f, 0.70f)
                depthSteps.forEach { frac ->
                    val y = horizonY + (size.height - horizonY) * frac
                    drawLine(
                        color = gridLine.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                // Subtle static ambient micro-nodes
                drawCircle(color = nodeGlow, radius = 3.dp.toPx(), center = Offset(size.width * 0.84f, size.height * 0.16f))
                drawCircle(color = nodeGlow, radius = 2.dp.toPx(), center = Offset(size.width * 0.12f, size.height * 0.48f))
            }
    ) {
        content()
    }
}

@Composable
fun Interactive3DCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var targetRotX by remember { mutableFloatStateOf(0f) }
    var targetRotY by remember { mutableFloatStateOf(0f) }
    var targetScale by remember { mutableFloatStateOf(1f) }

    val rotX by animateFloatAsState(
        targetValue = targetRotX,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
        label = "cardRotX"
    )
    val rotY by animateFloatAsState(
        targetValue = targetRotY,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
        label = "cardRotY"
    )
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 500f),
        label = "cardScale"
    )

    val shape = RoundedCornerShape(24.dp)

    Surface(
        shape = shape,
        color = containerColor,
        tonalElevation = 5.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(
            width = 1.1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.28f),
                    glowColor.copy(alpha = 0.30f),
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                )
            )
        ),
        modifier = modifier
            .graphicsLayer {
                cameraDistance = 14f * density
                rotationX = rotX
                rotationY = rotY
                scaleX = scale
                scaleY = scale
                ambientShadowColor = glowColor
                spotShadowColor = glowColor
            }
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = { offset ->
                        val nx = ((offset.x / size.width.coerceAtLeast(1)) - 0.5f) * 2f
                        val ny = ((offset.y / size.height.coerceAtLeast(1)) - 0.5f) * 2f
                        targetRotY = nx * 5.8f
                        targetRotX = -ny * 5.8f
                        targetScale = 0.985f
                        tryAwaitRelease()
                        targetRotX = 0f
                        targetRotY = 0f
                        targetScale = 1f
                    },
                    onTap = { onClick?.invoke() }
                )
            }
    ) {
        Column(content = content)
    }
}

@Composable
fun EngineStatusPill(
    engineMode: ActiveEngineMode,
    engineState: LiteRtEngineState,
    activeLanServer: LanServerEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOffline = engineMode == ActiveEngineMode.OFFLINE_LITERT
    val isReady = if (isOffline) engineState.isReady else (activeLanServer != null)

    val containerColor = when {
        !isReady -> MaterialTheme.colorScheme.surfaceVariant
        isOffline -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when {
        !isReady -> MaterialTheme.colorScheme.onSurfaceVariant
        isOffline -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val dotColor = when {
        engineState.isLoading -> MaterialTheme.colorScheme.primary
        !isReady -> MaterialTheme.colorScheme.error
        isOffline -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    val pulseAlpha by rememberInfiniteTransition(label = "dotPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val labelText = when {
        isOffline && engineState.isLoading -> "A carregar ${engineState.loadedModelName ?: "modelo"}..."
        isOffline && engineState.isReady ->
            "Offline (${engineState.activeBackend?.name ?: "CPU"}) • ${engineState.loadedModelName}"
        isOffline -> "Offline • Sem modelo carregado"
        activeLanServer != null -> "Rede Local • ${activeLanServer.selectedModel}"
        else -> "Rede Local • Não configurado"
    }

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag("engine_status_pill"),
        shape = RoundedCornerShape(50),
        color = containerColor,
        tonalElevation = 5.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.30f), MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = if (isReady || engineState.isLoading) pulseAlpha else 1f))
            )
            Icon(
                imageVector = if (isOffline) Icons.Filled.Memory else Icons.Filled.Lan,
                contentDescription = if (isOffline) "Modo Offline" else "Modo Rede Local",
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = "Alterar motor de IA",
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        "%.2f GB".format(mb / 1024.0)
    } else {
        "%.0f MB".format(mb)
    }
}

enum class Button3DTone {
    CYAN,
    EMERALD,
    AMBER,
    CORAL,
    VIOLET,
    INDIGO
}

fun toneColors(tone: Button3DTone): Pair<Color, Color> {
    return when (tone) {
        Button3DTone.CYAN -> Button3DCyan to Button3DCyanBase
        Button3DTone.EMERALD -> Button3DEmerald to Button3DEmeraldBase
        Button3DTone.AMBER -> Button3DAmber to Button3DAmberBase
        Button3DTone.CORAL -> Button3DCoral to Button3DCoralBase
        Button3DTone.VIOLET -> Button3DViolet to Button3DVioletBase
        Button3DTone.INDIGO -> Button3DIndigo to Button3DIndigoBase
    }
}

@Composable
fun Tactile3DButton(
    text: String,
    icon: ImageVector,
    tone: Button3DTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val maxDepth = 6.dp
    val pressOffset by animateDpAsState(
        targetValue = if (pressed && enabled) 5.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "press3d"
    )
    val tiltX by animateFloatAsState(
        targetValue = if (pressed && enabled) -4.5f else 0f,
        animationSpec = spring(stiffness = 650f),
        label = "tilt3d"
    )

    val (faceColor, baseColor) = toneColors(tone)
    val effectiveFace = if (enabled) faceColor else faceColor.copy(alpha = 0.45f)
    val effectiveBase = if (enabled) baseColor else baseColor.copy(alpha = 0.45f)
    val shape = RoundedCornerShape(17.dp)

    Box(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .minimumInteractiveComponentSize()
            .height(50.dp + maxDepth)
            .graphicsLayer {
                cameraDistance = 12f * density
                rotationX = tiltX
                ambientShadowColor = effectiveFace
                spotShadowColor = effectiveFace
            }
    ) {
        // Extruded 3D bottom ledge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(50.dp)
                .clip(shape)
                .background(effectiveBase)
        )
        // Top tactile face with specular rim and top gloss
        Box(
            modifier = Modifier
                .offset(y = pressOffset)
                .fillMaxWidth()
                .height(50.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            lerp(effectiveFace, Color.White, 0.22f),
                            effectiveFace,
                            lerp(effectiveFace, effectiveBase, 0.22f)
                        )
                    )
                )
                .border(
                    width = 1.15.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.56f), Color.White.copy(alpha = 0.12f))
                    ),
                    shape = shape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun Tactile3DChipButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.28f),
                    accentColor.copy(alpha = 0.34f)
                )
            )
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun Tactile3DMenuTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tone: Button3DTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val maxDepth = 7.dp
    val pressOffset by animateDpAsState(
        targetValue = if (pressed) 5.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "tilePress3d"
    )
    val tiltX by animateFloatAsState(
        targetValue = if (pressed) -5f else 0f,
        animationSpec = spring(stiffness = 650f),
        label = "tileTilt3d"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(stiffness = 650f),
        label = "tileScale3d"
    )

    val (faceColor, baseColor) = toneColors(tone)
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(98.dp + maxDepth)
            .graphicsLayer {
                cameraDistance = 12f * density
                rotationX = tiltX
                scaleX = scale
                scaleY = scale
                ambientShadowColor = faceColor
                spotShadowColor = faceColor
            }
    ) {
        // Extruded 3D base
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(98.dp)
                .clip(shape)
                .background(baseColor)
        )
        // Top beveled face with dynamic gloss
        Box(
            modifier = Modifier
                .offset(y = pressOffset)
                .fillMaxWidth()
                .height(98.dp)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            lerp(faceColor, Color.White, 0.22f),
                            faceColor,
                            lerp(faceColor, baseColor, 0.32f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(380f, 260f)
                    )
                )
                .border(
                    width = 1.25.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.56f), Color.White.copy(alpha = 0.14f))
                    ),
                    shape = shape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                )
        ) {
            // Subtle glossy specular arc in upper-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(72.dp)
                    .offset(x = 18.dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(13.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.15f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
