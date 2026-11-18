package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Repository abstracting Room DAOs and app-private local storage for scanned QR/barcodes
 * and captured/analyzed images.
 */
class LocalHistoryRepository(
    context: Context,
    private val scannedCodeDao: ScannedCodeDao,
    private val visionHistoryDao: VisionHistoryDao
) {
    private val appContext = context.applicationContext
    private val capturedImagesDir: File = File(appContext.filesDir, "captured_images").apply {
        mkdirs()
    }

    val scannedCodes: Flow<List<ScannedCodeEntity>> = scannedCodeDao.observeAll()
    val visionHistory: Flow<List<VisionHistoryEntity>> = visionHistoryDao.observeAll()

    suspend fun saveScannedCode(entity: ScannedCodeEntity): Long {
        return scannedCodeDao.insert(entity)
    }

    suspend fun updateScannedAiInterpretation(id: Long, aiInterpretation: String) {
        val existing = scannedCodeDao.getById(id) ?: return
        scannedCodeDao.update(existing.copy(aiInterpretation = aiInterpretation))
    }

    suspend fun deleteScannedCode(item: ScannedCodeEntity) {
        scannedCodeDao.deleteById(item.id)
    }

    suspend fun clearScannedCodes() {
        scannedCodeDao.clearAll()
    }

    suspend fun saveCapturedImageAndRecord(
        bitmap: Bitmap,
        prefix: String,
        detectedLabels: String,
        extractedText: String,
        aiSummary: String
    ): VisionHistoryEntity {
        val savedUri = withContext(Dispatchers.IO) {
            if (!capturedImagesDir.exists()) capturedImagesDir.mkdirs()
            val outFile = File(capturedImagesDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
            Uri.fromFile(outFile).toString()
        }
        val entity = VisionHistoryEntity(
            mediaType = "PHOTO",
            mediaUri = savedUri,
            detectedLabels = detectedLabels,
            extractedText = extractedText,
            aiSummary = aiSummary
        )
        val id = visionHistoryDao.insert(entity)
        return entity.copy(id = id)
    }

    suspend fun recordVideoVision(
        mediaUri: String,
        detectedLabels: String,
        extractedText: String,
        aiSummary: String
    ): Long {
        return visionHistoryDao.insert(
            VisionHistoryEntity(
                mediaType = "VIDEO",
                mediaUri = mediaUri,
                detectedLabels = detectedLabels,
                extractedText = extractedText,
                aiSummary = aiSummary
            )
        )
    }

    suspend fun loadBitmapFromLocalUri(mediaUri: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = Uri.parse(mediaUri)
            if (parsed.scheme == "file") {
                val path = parsed.path ?: return@runCatching null
                BitmapFactory.decodeFile(path)
            } else {
                appContext.contentResolver.openInputStream(parsed)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        }.getOrNull()
    }

    suspend fun encodeBitmapToBase64(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        runCatching {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    suspend fun deleteVisionItem(item: VisionHistoryEntity) {
        withContext(Dispatchers.IO) {
            deleteLocalCapturedFileIfOwned(item.mediaUri)
        }
        visionHistoryDao.deleteById(item.id)
    }

    suspend fun clearVisionHistory() {
        visionHistoryDao.clearAll()
        withContext(Dispatchers.IO) {
            capturedImagesDir.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
        }
    }

    private fun deleteLocalCapturedFileIfOwned(mediaUri: String) {
        runCatching {
            val parsed = Uri.parse(mediaUri)
            val path = parsed.path ?: return
            val file = File(path)
            if (file.exists() && file.parentFile?.canonicalPath == capturedImagesDir.canonicalPath) {
                file.delete()
            }
        }
    }
}
