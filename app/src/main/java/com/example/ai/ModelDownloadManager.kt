package com.example.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.LocalModelDao
import com.example.data.local.LocalModelEntity
import com.example.data.local.ModelStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ModelDownloadManager(
    private val context: Context,
    private val modelDao: LocalModelDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun modelsDirectory(): File {
        return File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
    }

    fun startDownload(model: LocalModelEntity) {
        if (activeJobs[model.id]?.isActive == true) return

        val job = scope.launch {
            val destFile = File(modelsDirectory(), model.fileName)
            val tempFile = File(modelsDirectory(), "${model.fileName}.part")

            modelDao.upsert(
                model.copy(
                    status = ModelStatus.DOWNLOADING.name,
                    downloadedBytes = 0L,
                    errorMessage = null
                )
            )

            try {
                val request = Request.Builder()
                    .url(model.downloadUrl)
                    .header("User-Agent", "NucleoIA-Android/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val hint = if (response.code == 401 || response.code == 403) {
                            "HTTP ${response.code}: O repositório requer autenticação ou URL direto público."
                        } else {
                            "Erro HTTP ${response.code} ao descarregar modelo."
                        }
                        modelDao.upsert(
                            model.copy(
                                status = ModelStatus.ERROR.name,
                                errorMessage = hint
                            )
                        )
                        return@launch
                    }

                    val body = response.body ?: throw IllegalStateException("Resposta vazia do servidor.")
                    val contentLength = body.contentLength().takeIf { it > 0 } ?: model.expectedSizeBytes
                    var totalRead = 0L
                    var lastReportTime = System.currentTimeMillis()

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                totalRead += read

                                val now = System.currentTimeMillis()
                                if (now - lastReportTime >= 350L) {
                                    lastReportTime = now
                                    modelDao.upsert(
                                        model.copy(
                                            status = ModelStatus.DOWNLOADING.name,
                                            downloadedBytes = totalRead,
                                            expectedSizeBytes = contentLength,
                                            errorMessage = null
                                        )
                                    )
                                }
                            }
                            output.flush()
                        }
                    }

                    if (totalRead < 1024 * 1024L) {
                        tempFile.delete()
                        throw IllegalStateException("Ficheiro descarregado demasiado pequeno ($totalRead bytes). Verifique o URL do modelo .litertlm.")
                    }

                    if (destFile.exists()) destFile.delete()
                    if (!tempFile.renameTo(destFile)) {
                        tempFile.copyTo(destFile, overwrite = true)
                        tempFile.delete()
                    }

                    modelDao.upsert(
                        model.copy(
                            status = ModelStatus.DOWNLOADED.name,
                            downloadedBytes = totalRead,
                            expectedSizeBytes = totalRead,
                            localPath = destFile.absolutePath,
                            errorMessage = null
                        )
                    )
                }
            } catch (e: CancellationException) {
                tempFile.delete()
                modelDao.upsert(
                    model.copy(
                        status = ModelStatus.AVAILABLE.name,
                        downloadedBytes = 0L,
                        errorMessage = null
                    )
                )
            } catch (e: Throwable) {
                tempFile.delete()
                modelDao.upsert(
                    model.copy(
                        status = ModelStatus.ERROR.name,
                        errorMessage = e.localizedMessage ?: "Falha na descarga do modelo"
                    )
                )
            } finally {
                activeJobs.remove(model.id)
            }
        }
        activeJobs[model.id] = job
    }

    fun cancelDownload(modelId: String) {
        activeJobs[modelId]?.cancel()
        activeJobs.remove(modelId)
    }

    suspend fun deleteLocalFile(model: LocalModelEntity) = withContext(Dispatchers.IO) {
        cancelDownload(model.id)
        model.localPath?.let { path ->
            runCatching { File(path).delete() }
        }
        File(modelsDirectory(), model.fileName).delete()
        File(modelsDirectory(), "${model.fileName}.part").delete()

        if (model.isCustom && model.downloadUrl.isBlank()) {
            modelDao.deleteById(model.id)
        } else {
            modelDao.upsert(
                model.copy(
                    status = ModelStatus.AVAILABLE.name,
                    downloadedBytes = 0L,
                    localPath = null,
                    errorMessage = null
                )
            )
        }
    }

    suspend fun importLocalModelUri(uri: Uri): Result<LocalModelEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = resolveFileName(uri) ?: "modelo_importado_${System.currentTimeMillis()}.litertlm"
            val sanitizedName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val destFile = File(modelsDirectory(), sanitizedName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            } ?: throw IllegalStateException("Não foi possível ler o ficheiro selecionado.")

            val sizeBytes = destFile.length()
            val id = "imported_${sanitizedName.lowercase()}"
            val entity = LocalModelEntity(
                id = id,
                name = sanitizedName.removeSuffix(".litertlm"),
                family = "Local Importado",
                repoId = "Dispositivo Local",
                fileName = sanitizedName,
                downloadUrl = "",
                expectedSizeBytes = sizeBytes,
                downloadedBytes = sizeBytes,
                localPath = destFile.absolutePath,
                status = ModelStatus.IMPORTED.name,
                description = "Modelo .litertlm importado do armazenamento local para execução offline.",
                isFeatured = false,
                isCustom = true
            )
            modelDao.upsert(entity)
            entity
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        } ?: uri.lastPathSegment
    }

    fun getTotalStorageUsedBytes(): Long {
        return modelsDirectory().listFiles()
            ?.filter { it.isFile }
            ?.sumOf { it.length() } ?: 0L
    }
}
