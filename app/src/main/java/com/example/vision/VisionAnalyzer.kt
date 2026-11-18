package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class VisualLabel(
    val text: String,
    val confidence: Float
)

data class PhotoVisionResult(
    val sourceUri: String,
    val previewBitmap: Bitmap?,
    val labels: List<VisualLabel>,
    val extractedText: String,
    val detectedBarcodes: List<String>,
    val dominantColors: List<String> = emptyList(),
    val resolutionLabel: String = "",
    val brightnessLabel: String = "",
    val base64Jpeg: String?
) {
    fun buildAiContextPrompt(): String = buildString {
        appendLine("Contexto visual extraído localmente (100% offline) da fotografia:")
        if (resolutionLabel.isNotBlank()) {
            appendLine("- Dimensão/Luminosidade: $resolutionLabel • $brightnessLabel")
        }
        if (dominantColors.isNotEmpty()) {
            appendLine("- Cores predominantes: ${dominantColors.joinToString(", ")}")
        }
        if (labels.isNotEmpty()) {
            appendLine("- Elementos e conceitos detetados: " +
                labels.joinToString(", ") { "${it.text} (${(it.confidence * 100).toInt()}%)" })
        } else {
            appendLine("- Elementos detetados: nenhum rótulo automático com confiança suficiente.")
        }
        if (extractedText.isNotBlank()) {
            appendLine("- Texto visível (OCR): \"${extractedText.trim()}\"")
        }
        if (detectedBarcodes.isNotEmpty()) {
            appendLine("- Códigos QR/Barras presentes: ${detectedBarcodes.joinToString(" | ")}")
        }
    }
}

data class VideoKeyframeAnalysis(
    val timestampMs: Long,
    val bitmap: Bitmap,
    val labels: List<VisualLabel>,
    val extractedText: String
)

data class VideoVisionResult(
    val sourceUri: String,
    val durationMs: Long,
    val keyframes: List<VideoKeyframeAnalysis>,
    val combinedLabels: List<VisualLabel>,
    val combinedText: String,
    val primaryFrameBase64: String?
) {
    fun buildAiContextPrompt(): String = buildString {
        val secs = durationMs / 1000.0
        appendLine("Contexto visual extraído localmente do vídeo (duração: ${"%.1f".format(secs)}s, ${keyframes.size} fotogramas-chave):")
        keyframes.forEach { frame ->
            val tSec = frame.timestampMs / 1000.0
            val lbl = frame.labels.take(5).joinToString(", ") { "${it.text} (${(it.confidence * 100).toInt()}%)" }
            append("- [${"%.1f".format(tSec)}s] ")
            append(if (lbl.isNotBlank()) "Objetos/Cenas: $lbl" else "Sem rótulos claros")
            if (frame.extractedText.isNotBlank()) {
                append(" | Texto OCR: \"${frame.extractedText.replace("\n", " ").take(120)}\"")
            }
            appendLine()
        }
    }
}

class VisionAnalyzer(private val context: Context) {
    private val labeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.55f)
                .build()
        )
    }

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val barcodeScanner by lazy {
        BarcodeScanning.getClient()
    }

    suspend fun analyzePhoto(uri: Uri): PhotoVisionResult = withContext(Dispatchers.IO) {
        val bitmap = decodeScaledBitmap(uri, maxDim = 1024)
            ?: throw IllegalStateException("Não foi possível carregar a imagem selecionada.")
        analyzeBitmap(bitmap, uri.toString())
    }

    suspend fun analyzeBitmap(bitmap: Bitmap, sourceLabel: String): PhotoVisionResult =
        withContext(Dispatchers.IO) {
            val scaled = scaleDownIfNeeded(bitmap, maxDim = 1024)
            val inputImage = InputImage.fromBitmap(scaled, 0)

            val labels = runCatching {
                labeler.process(inputImage).awaitResult()
                    .map { VisualLabel(it.text, it.confidence) }
                    .sortedByDescending { it.confidence }
                    .take(10)
            }.getOrDefault(emptyList())

            val ocrText = runCatching {
                textRecognizer.process(inputImage).awaitResult().text.trim()
            }.getOrDefault("")

            val barcodes = runCatching {
                barcodeScanner.process(inputImage).awaitResult()
                    .mapNotNull { it.rawValue }
            }.getOrDefault(emptyList())

            val base64 = encodeJpegBase64(scaleDownIfNeeded(scaled, maxDim = 768), quality = 80)
            val colorNames = extractDominantColorNames(scaled)
            val brightness = estimateBrightnessLabel(scaled)

            PhotoVisionResult(
                sourceUri = sourceLabel,
                previewBitmap = scaled,
                labels = labels,
                extractedText = ocrText,
                detectedBarcodes = barcodes,
                dominantColors = colorNames,
                resolutionLabel = "${scaled.width}x${scaled.height}px",
                brightnessLabel = brightness,
                base64Jpeg = base64
            )
        }

    suspend fun analyzeVideo(uri: Uri, frameCount: Int = 5): VideoVisionResult =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.coerceAtLeast(500L) ?: 3000L

                val count = frameCount.coerceIn(2, 6)
                val frames = mutableListOf<VideoKeyframeAnalysis>()
                for (i in 0 until count) {
                    val ratio = if (count == 1) 0.5 else (i + 0.5) / count.toDouble()
                    val timeUs = (durationMs * ratio * 1000.0).toLong()
                    val rawFrame = retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    ) ?: retriever.getFrameAtTime(timeUs)

                    if (rawFrame != null) {
                        val scaled = scaleDownIfNeeded(rawFrame, maxDim = 720)
                        val inputImage = InputImage.fromBitmap(scaled, 0)
                        val labels = runCatching {
                            labeler.process(inputImage).awaitResult()
                                .map { VisualLabel(it.text, it.confidence) }
                                .sortedByDescending { it.confidence }
                                .take(6)
                        }.getOrDefault(emptyList())

                        val text = runCatching {
                            textRecognizer.process(inputImage).awaitResult().text.trim()
                        }.getOrDefault("")

                        frames.add(
                            VideoKeyframeAnalysis(
                                timestampMs = timeUs / 1000L,
                                bitmap = scaled,
                                labels = labels,
                                extractedText = text
                            )
                        )
                    }
                }

                if (frames.isEmpty()) {
                    throw IllegalStateException("Não foi possível extrair fotogramas deste vídeo.")
                }

                val aggregatedLabels = frames
                    .flatMap { it.labels }
                    .groupBy { it.text }
                    .map { (name, group) -> VisualLabel(name, group.maxOf { it.confidence }) }
                    .sortedByDescending { it.confidence }
                    .take(12)

                val combinedText = frames
                    .mapNotNull { f -> f.extractedText.takeIf { it.isNotBlank() } }
                    .distinct()
                    .joinToString("\n---\n")

                val middleFrame = frames[frames.size / 2].bitmap
                val primaryBase64 = encodeJpegBase64(scaleDownIfNeeded(middleFrame, maxDim = 640), 75)

                VideoVisionResult(
                    sourceUri = uri.toString(),
                    durationMs = durationMs,
                    keyframes = frames,
                    combinedLabels = aggregatedLabels,
                    combinedText = combinedText,
                    primaryFrameBase64 = primaryBase64
                )
            } finally {
                runCatching { retriever.release() }
            }
        }

    private fun decodeScaledBitmap(uri: Uri, maxDim: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.isMutableRequired = true
                val w = info.size.width
                val h = info.size.height
                val maxSide = maxOf(w, h)
                if (maxSide > maxDim) {
                    val scale = maxDim.toFloat() / maxSide.toFloat()
                    decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                }
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.let { scaleDownIfNeeded(it, maxDim) }
            }
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun encodeJpegBase64(bitmap: Bitmap, quality: Int): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun estimateBrightnessLabel(bitmap: Bitmap): String {
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)
        var totalLum = 0.0
        var count = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val p = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(p)
                val g = android.graphics.Color.green(p)
                val b = android.graphics.Color.blue(p)
                totalLum += (0.299 * r + 0.587 * g + 0.114 * b)
                count++
                x += stepX
            }
            y += stepY
        }
        val avg = if (count > 0) totalLum / count else 128.0
        return when {
            avg < 65 -> "Ambiente escuro"
            avg < 140 -> "Iluminação moderada"
            else -> "Bem iluminada"
        }
    }

    private fun extractDominantColorNames(bitmap: Bitmap): List<String> {
        val counts = mutableMapOf<String, Int>()
        val stepX = (bitmap.width / 20).coerceAtLeast(1)
        val stepY = (bitmap.height / 20).coerceAtLeast(1)
        val hsv = FloatArray(3)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val p = bitmap.getPixel(x, y)
                android.graphics.Color.colorToHSV(p, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val valB = hsv[2]
                val bucket = when {
                    valB < 0.18f -> "Preto"
                    sat < 0.15f && valB > 0.82f -> "Branco"
                    sat < 0.18f -> "Cinzento"
                    hue < 18f || hue >= 340f -> "Vermelho"
                    hue < 45f -> "Laranja"
                    hue < 70f -> "Amarelo"
                    hue < 160f -> "Verde"
                    hue < 200f -> "Ciano"
                    hue < 255f -> "Azul"
                    else -> "Roxo/Magenta"
                }
                counts[bucket] = (counts[bucket] ?: 0) + 1
                x += stepX
            }
            y += stepY
        }
        return counts.entries.sortedByDescending { it.value }.take(3).map { it.key }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exc ->
            if (cont.isActive) cont.resumeWithException(exc)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}
