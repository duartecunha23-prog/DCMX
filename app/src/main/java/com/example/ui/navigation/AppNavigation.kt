package com.example.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.NeuralDepthBackground
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.AppStateViewModel
import com.example.ui.screens.AuthorScreen
import com.example.ui.screens.CameraVisionScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ModelsScreen
import com.example.ui.screens.ScannerScreen
import kotlinx.serialization.Serializable

@Serializable
object ChatRoute

@Serializable
data class ModelsRoute(val initialTab: Int = 0)

@Serializable
object CameraRoute

@Serializable
object ScannerRoute

@Serializable
object AuthorRoute

private data class NavTabItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val matches: (androidx.navigation.NavDestination?) -> Boolean
)

@Composable
fun NucleoIaApp(viewModel: AppStateViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val lanServers by viewModel.lanServers.collectAsStateWithLifecycle()
    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatGenerating by viewModel.isChatGenerating.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val isDiscoveringLan by viewModel.isDiscoveringLan.collectAsStateWithLifecycle()
    val discoveredNodes by viewModel.discoveredNodes.collectAsStateWithLifecycle()
    val lanProbeStatus by viewModel.lanProbeStatus.collectAsStateWithLifecycle()

    val photoVision by viewModel.photoVision.collectAsStateWithLifecycle()
    val videoVision by viewModel.videoVision.collectAsStateWithLifecycle()
    val isAnalyzingMedia by viewModel.isAnalyzingMedia.collectAsStateWithLifecycle()
    val visionError by viewModel.visionError.collectAsStateWithLifecycle()
    val visionChat by viewModel.visionChat.collectAsStateWithLifecycle()
    val isVisionAiBusy by viewModel.isVisionAiBusy.collectAsStateWithLifecycle()
    val visionHistory by viewModel.visionHistory.collectAsStateWithLifecycle()

    val scannerEditor by viewModel.scannerEditor.collectAsStateWithLifecycle()
    val scannedCodes by viewModel.scannedCodes.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val activeLanServer = remember(lanServers) {
        lanServers.firstOrNull { it.isActive } ?: lanServers.firstOrNull()
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = remember {
        listOf(
            NavTabItem(
                route = ChatRoute,
                label = "Assistente",
                selectedIcon = Icons.Filled.ChatBubble,
                unselectedIcon = Icons.Outlined.ChatBubbleOutline,
                testTag = "nav_chat",
                matches = { it?.hasRoute<ChatRoute>() == true }
            ),
            NavTabItem(
                route = ModelsRoute(0),
                label = "Modelos",
                selectedIcon = Icons.Filled.Hub,
                unselectedIcon = Icons.Outlined.Hub,
                testTag = "nav_models",
                matches = { it?.hasRoute<ModelsRoute>() == true }
            ),
            NavTabItem(
                route = CameraRoute,
                label = "Câmara",
                selectedIcon = Icons.Filled.CameraAlt,
                unselectedIcon = Icons.Outlined.CameraAlt,
                testTag = "nav_camera",
                matches = { it?.hasRoute<CameraRoute>() == true }
            ),
            NavTabItem(
                route = ScannerRoute,
                label = "QR/Barras",
                selectedIcon = Icons.Filled.QrCodeScanner,
                unselectedIcon = Icons.Outlined.QrCodeScanner,
                testTag = "nav_scanner",
                matches = { it?.hasRoute<ScannerRoute>() == true }
            ),
            NavTabItem(
                route = AuthorRoute,
                label = "Autor",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                testTag = "nav_author",
                matches = { it?.hasRoute<AuthorRoute>() == true }
            )
        )
    }

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NeuralDepthBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        Floating3DDockBar(
                            tabs = tabs,
                            currentDestination = currentDestination,
                            onSelectTab = { navigateToTab(it.route) }
                        )
                    }
                }
            ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = tab.matches(currentDestination)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ChatRoute
                    ) {
                        composable<ChatRoute> {
                            ChatScreen(
                                engineMode = preferences.engineMode,
                                engineState = engineState,
                                activeLanServer = activeLanServer,
                                messages = chatMessages,
                                isGenerating = isChatGenerating,
                                chatError = chatError,
                                onSendMessage = viewModel::sendChatMessage,
                                onStopGeneration = viewModel::stopChatGeneration,
                                onClearChat = viewModel::clearChat,
                                onDeleteMessage = viewModel::deleteChatMessage,
                                onOpenModels = { navigateToTab(ModelsRoute(0)) },
                                onOpenLanSettings = { navigateToTab(ModelsRoute(1)) },
                                onOpenCamera = { navigateToTab(CameraRoute) },
                                onOpenScanner = { navigateToTab(ScannerRoute) },
                                onOpenAuthor = { navigateToTab(AuthorRoute) }
                            )
                        }

                        composable<ModelsRoute> { backStackEntry ->
                            val args = backStackEntry.toRoute<ModelsRoute>()
                            ModelsScreen(
                                initialTab = args.initialTab,
                                engineMode = preferences.engineMode,
                                engineState = engineState,
                                models = models,
                                lanServers = lanServers,
                                isDiscoveringLan = isDiscoveringLan,
                                discoveredNodes = discoveredNodes,
                                lanProbeStatus = lanProbeStatus,
                                onSetEngineMode = viewModel::setEngineMode,
                                onStartDownload = viewModel::startModelDownload,
                                onCancelDownload = viewModel::cancelModelDownload,
                                onDeleteModel = viewModel::deleteModel,
                                onToggleBackend = viewModel::toggleModelBackend,
                                onLoadOfflineModel = viewModel::loadOfflineModel,
                                onUnloadOfflineModel = viewModel::unloadOfflineModel,
                                onAddCustomModel = viewModel::addCustomModel,
                                onImportLocalFile = viewModel::importLocalModel,
                                onSaveAndActivateLan = viewModel::saveAndActivateLanServer,
                                onActivateLanServer = viewModel::activateLanServer,
                                onProbeLanServer = viewModel::probeLanServer,
                                onSelectLanModel = viewModel::selectLanModel,
                                onDeleteLanServer = viewModel::deleteLanServer,
                                onDiscoverLanNodes = viewModel::discoverLanNodes
                            )
                        }

                        composable<CameraRoute> {
                            CameraVisionScreen(
                                preferences = preferences,
                                photoVision = photoVision,
                                videoVision = videoVision,
                                isAnalyzingMedia = isAnalyzingMedia,
                                visionError = visionError,
                                visionChat = visionChat,
                                isVisionAiBusy = isVisionAiBusy,
                                visionHistory = visionHistory,
                                onAnalyzePhotoUri = viewModel::analyzePhotoUri,
                                onAnalyzeCapturedBitmap = viewModel::analyzeCapturedBitmap,
                                onAnalyzeVideoUri = viewModel::analyzeVideoUri,
                                onAskVisionAi = viewModel::askVisionAi,
                                onOpenVisionHistoryItem = viewModel::openSavedVisionItem,
                                onDeleteVisionHistoryItem = viewModel::deleteVisionHistoryItem,
                                onClearVisionHistory = viewModel::clearVisionHistory,
                                onSetCameraUseFront = viewModel::setCameraUseFront,
                                onSetCameraFlashEnabled = viewModel::setCameraFlashEnabled,
                                onSetCameraLiveVision = viewModel::setCameraLiveVision,
                                onSetCameraRecordAudio = viewModel::setCameraRecordAudio
                            )
                        }

                        composable<ScannerRoute> {
                            ScannerScreen(
                                editorState = scannerEditor,
                                history = scannedCodes,
                                flashEnabled = preferences.cameraFlashEnabled,
                                onToggleFlash = viewModel::setCameraFlashEnabled,
                                onBarcodeDetected = viewModel::onBarcodeDetected,
                                onScanFromPhotoUri = viewModel::scanBarcodeFromPhotoUri,
                                onOpenManualEditor = viewModel::openManualBarcodeEditor,
                                onOpenSavedCode = viewModel::openSavedCode,
                                onUpdatePayload = viewModel::updateEditorPayload,
                                onUpdateFormat = viewModel::updateEditorFormat,
                                onUpdateMeta = viewModel::updateEditorMeta,
                                onSaveEditedCode = viewModel::saveEditedCode,
                                onInterpretWithAi = viewModel::interpretCodeWithAi,
                                onCloseEditor = viewModel::closeScannerEditor,
                                onDeleteSavedCode = viewModel::deleteScannedCode,
                                onClearHistory = viewModel::clearAllScannedCodes
                            )
                        }

                        composable<AuthorRoute> {
                            val diagnostics = remember { viewModel.getDeviceDiagnostics() }
                            AuthorScreen(
                                preferences = preferences,
                                diagnostics = diagnostics,
                                onToggleDarkTheme = viewModel::setDarkTheme,
                                onTogglePortuguese = viewModel::setForcePortuguese,
                                onClearAllHistory = viewModel::clearAllQuestionHistory
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun Floating3DDockBar(
    tabs: List<NavTabItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onSelectTab: (NavTabItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(
                width = 1.1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.30f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = tab.matches(currentDestination)
                    val liftOffset by animateDpAsState(
                        targetValue = if (selected) (-3).dp else 0.dp,
                        animationSpec = spring(dampingRatio = 0.62f, stiffness = 500f),
                        label = "dockLift"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .offset(y = liftOffset)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) {
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 7.dp, horizontal = 4.dp)
                            .testTag(tab.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(width = 18.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}
