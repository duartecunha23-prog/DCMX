package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ai.LiteRtEngineState
import com.example.data.local.ActiveEngineMode
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.ui.screens.ChatScreen
import com.example.ui.theme.NucleoIaTheme
import com.example.vision.BarcodeInterpreter
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app strings match Nucleo IA and author Duarte Cunha`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Núcleo IA", context.getString(R.string.app_name))
        assertEquals("DUARTE CUNHA", context.getString(R.string.author_name))
        assertEquals("duartecunha@icloud.com", context.getString(R.string.author_email))
        assertEquals("TEL. 962346169", context.getString(R.string.author_phone_display))
    }

    @Test
    fun `barcode interpreter parses Portugal EAN13 and WiFi QR and generates bitmap`() {
        val ean = BarcodeInterpreter.interpret("5601234567892", Barcode.FORMAT_EAN_13)
        assertTrue(ean.structuredSummary.contains("Portugal"))

        val wifi = BarcodeInterpreter.interpret("WIFI:T:WPA;S:RedeCasa;P:segredo123;;", Barcode.FORMAT_QR_CODE)
        assertEquals("Rede Wi-Fi", wifi.semanticType)
        assertTrue(wifi.structuredSummary.contains("RedeCasa"))

        val bmp = BarcodeInterpreter.generateBarcodeBitmap("https://nucleoia.local", Barcode.FORMAT_QR_CODE, 256, 256)
        assertNotNull(bmp)
    }

    @Test
    fun `chat question history supports individual deletion and full clear`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val dao = db.chatMessageDao()
        dao.clearSession()

        val id1 = dao.insert(ChatMessageEntity(isUser = true, content = "Pergunta 1", engineLabel = "Offline"))
        val id2 = dao.insert(ChatMessageEntity(isUser = true, content = "Pergunta 2", engineLabel = "Offline"))
        assertEquals(2, dao.observeMessages().first().size)

        dao.deleteMessageById(id1)
        val remaining = dao.observeMessages().first()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining.first().id)

        dao.clearSession()
        assertTrue(dao.observeMessages().first().isEmpty())
    }

    @Test
    fun `chat send button interacts properly and submits typed question`() {
        val sentPrompts = mutableListOf<String>()

        composeTestRule.setContent {
            NucleoIaTheme {
                ChatScreen(
                    engineMode = ActiveEngineMode.OFFLINE_LITERT,
                    engineState = LiteRtEngineState(isReady = true, loadedModelName = "Gemma 4 E2B"),
                    activeLanServer = null,
                    messages = emptyList(),
                    isGenerating = false,
                    chatError = null,
                    onSendMessage = { sentPrompts.add(it) },
                    onStopGeneration = {},
                    onClearChat = {},
                    onDeleteMessage = {},
                    onOpenModels = {},
                    onOpenLanSettings = {},
                    onOpenCamera = {},
                    onOpenScanner = {},
                    onOpenAuthor = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("chat_input_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_chat_button").assertIsDisplayed()

        // Tapping Enviar while empty shows helpful hint instead of being dead
        composeTestRule.onNodeWithTag("send_chat_button").performClick()
        composeTestRule.onNodeWithText("Escreva a sua pergunta em baixo e toque em Enviar para perguntar à IA.").assertIsDisplayed()

        // Typing a question and tapping Enviar dispatches the prompt
        composeTestRule.onNodeWithTag("chat_input_field").performTextInput("Olá Gemma")
        composeTestRule.onNodeWithTag("send_chat_button").performClick()
        assertEquals(listOf("Olá Gemma"), sentPrompts)
    }

    @Test
    fun `datastore persists theme and camera preferences locally`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.local.AppPreferences(context)

        prefs.setDarkTheme(false)
        prefs.setForcePortuguese(false)
        prefs.setCameraUseFront(true)
        prefs.setCameraFlashEnabled(true)
        prefs.setCameraLiveVision(false)
        prefs.setCameraRecordAudio(false)

        val stored = prefs.preferencesFlow.first()
        assertEquals(false, stored.darkTheme)
        assertEquals(false, stored.forcePortuguese)
        assertEquals(true, stored.cameraUseFront)
        assertEquals(true, stored.cameraFlashEnabled)
        assertEquals(false, stored.cameraLiveVision)
        assertEquals(false, stored.cameraRecordAudio)
    }

    @Test
    fun `room repository persists scanned qr codes and captured camera images in local storage`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repo = com.example.data.local.LocalHistoryRepository(
            context = context,
            scannedCodeDao = db.scannedCodeDao(),
            visionHistoryDao = db.visionHistoryDao()
        )
        repo.clearScannedCodes()
        repo.clearVisionHistory()

        // 1. Scanned QR code insert + update + delete
        val qrId = repo.saveScannedCode(
            com.example.data.local.ScannedCodeEntity(
                rawValue = "https://nucleoia.local",
                editedValue = "https://nucleoia.local",
                formatName = "QR Code",
                formatCode = Barcode.FORMAT_QR_CODE,
                semanticType = "Website / URL",
                title = "Portal Local"
            )
        )
        repo.updateScannedAiInterpretation(qrId, "Ligação segura para servidor local.")
        val storedCodes = repo.scannedCodes.first()
        assertEquals(1, storedCodes.size)
        assertEquals("Ligação segura para servidor local.", storedCodes.first().aiInterpretation)

        // 2. Captured Bitmap saved to filesDir/captured_images/ and recorded in Room
        val bmp = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
        val savedVision = repo.saveCapturedImageAndRecord(
            bitmap = bmp,
            prefix = "camara",
            detectedLabels = "Mesa, Portátil",
            extractedText = "Núcleo IA",
            aiSummary = "Análise local concluída."
        )
        val storedVision = repo.visionHistory.first()
        assertEquals(1, storedVision.size)
        val filePath = android.net.Uri.parse(savedVision.mediaUri).path
        assertNotNull(filePath)
        val localFile = java.io.File(filePath!!)
        assertTrue(localFile.exists() && localFile.length() > 0L)

        val reloadedBitmap = repo.loadBitmapFromLocalUri(savedVision.mediaUri)
        assertNotNull(reloadedBitmap)

        // 3. Deleting vision item deletes both Room record and local image file
        repo.deleteVisionItem(savedVision)
        assertTrue(repo.visionHistory.first().isEmpty())
        assertTrue(!localFile.exists())

        repo.deleteScannedCode(storedCodes.first())
        assertTrue(repo.scannedCodes.first().isEmpty())
    }
}
