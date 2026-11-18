package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import com.example.data.local.LanProtocol
import com.example.data.local.LanServerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit

data class LanProbeResult(
    val reachable: Boolean,
    val latencyMs: Long,
    val models: List<String>,
    val error: String? = null
)

data class DiscoveredLanNode(
    val baseUrl: String,
    val protocol: LanProtocol,
    val label: String,
    val latencyMs: Long,
    val models: List<String>
)

data class LanChatTurn(
    val isUser: Boolean,
    val content: String
)

class LanAiClient(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(1500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .build()

    suspend fun probeServer(baseUrl: String, protocol: LanProtocol): LanProbeResult =
        withContext(Dispatchers.IO) {
            val cleanBase = normalizeBaseUrl(baseUrl)
            val start = System.currentTimeMillis()
            val endpoint = when (protocol) {
                LanProtocol.OLLAMA -> "$cleanBase/api/tags"
                LanProtocol.OPENAI_COMPATIBLE -> "$cleanBase/v1/models"
            }

            try {
                val request = Request.Builder().url(endpoint).get().build()
                probeClient.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - start
                    if (!response.isSuccessful) {
                        return@withContext LanProbeResult(
                            reachable = false,
                            latencyMs = latency,
                            models = emptyList(),
                            error = "HTTP ${response.code}: ${response.message}"
                        )
                    }
                    val body = response.body?.string().orEmpty()
                    val models = parseModelList(body, protocol)
                    LanProbeResult(
                        reachable = true,
                        latencyMs = latency,
                        models = models
                    )
                }
            } catch (e: Exception) {
                LanProbeResult(
                    reachable = false,
                    latencyMs = System.currentTimeMillis() - start,
                    models = emptyList(),
                    error = e.localizedMessage ?: "Sem resposta do servidor local"
                )
            }
        }

    private fun parseModelList(rawJson: String, protocol: LanProtocol): List<String> {
        return runCatching {
            val root = json.parseToJsonElement(rawJson).jsonObject
            when (protocol) {
                LanProtocol.OLLAMA -> {
                    root["models"]?.jsonArray?.mapNotNull {
                        it.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                    }.orEmpty()
                }
                LanProtocol.OPENAI_COMPATIBLE -> {
                    root["data"]?.jsonArray?.mapNotNull {
                        it.jsonObject["id"]?.jsonPrimitive?.contentOrNull
                    }.orEmpty()
                }
            }
        }.getOrDefault(emptyList())
    }

    fun getDeviceLocalIpv4(): String? {
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val active = cm?.activeNetwork
            val linkProps = cm?.getLinkProperties(active)
            val addr = linkProps?.linkAddresses
                ?.map { it.address }
                ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            if (addr != null) return addr.hostAddress
        }
        return runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull()
    }

    suspend fun discoverLanNodes(): List<DiscoveredLanNode> = withContext(Dispatchers.IO) {
        val localIp = getDeviceLocalIpv4() ?: return@withContext emptyList()
        val parts = localIp.split(".")
        if (parts.size != 4) return@withContext emptyList()
        val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
        val selfLastOctet = parts[3].toIntOrNull() ?: -1

        val ports = listOf(
            11434 to LanProtocol.OLLAMA,
            1234 to LanProtocol.OPENAI_COMPATIBLE,
            8080 to LanProtocol.OPENAI_COMPATIBLE,
            8000 to LanProtocol.OPENAI_COMPATIBLE
        )

        val candidates = (1..254).filter { it != selfLastOctet }
        val discovered = mutableListOf<DiscoveredLanNode>()

        // Probe in batches so we stay fast and gentle on the local Wi-Fi network
        for (chunk in candidates.chunked(32)) {
            val batchResults = coroutineScope {
                chunk.flatMap { octet ->
                    val ip = "$prefix.$octet"
                    ports.map { (port, protocol) ->
                        async {
                            if (isTcpPortOpen(ip, port, timeoutMs = 220)) {
                                val url = "http://$ip:$port"
                                val probe = probeServer(url, protocol)
                                if (probe.reachable) {
                                    val label = if (protocol == LanProtocol.OLLAMA) {
                                        "Ollama ($ip)"
                                    } else {
                                        "Servidor IA ($ip:$port)"
                                    }
                                    DiscoveredLanNode(
                                        baseUrl = url,
                                        protocol = protocol,
                                        label = label,
                                        latencyMs = probe.latencyMs,
                                        models = probe.models
                                    )
                                } else null
                            } else null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            discovered.addAll(batchResults)
        }
        discovered.distinctBy { it.baseUrl }
    }

    private fun isTcpPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun streamChat(
        server: LanServerEntity,
        systemPrompt: String,
        history: List<LanChatTurn>,
        userPrompt: String,
        imageBase64Jpeg: String? = null,
        temperature: Double = 0.7
    ): Flow<String> = flow {
        val cleanBase = normalizeBaseUrl(server.baseUrl)
        val protocol = runCatching { LanProtocol.valueOf(server.protocol) }.getOrDefault(LanProtocol.OLLAMA)

        when (protocol) {
            LanProtocol.OLLAMA -> {
                val messagesJson = buildJsonArray {
                    add(buildJsonObject {
                        put("role", JsonPrimitive("system"))
                        put("content", JsonPrimitive(systemPrompt))
                    })
                    history.takeLast(8).forEach { turn ->
                        add(buildJsonObject {
                            put("role", JsonPrimitive(if (turn.isUser) "user" else "assistant"))
                            put("content", JsonPrimitive(turn.content))
                        })
                    }
                    add(buildJsonObject {
                        put("role", JsonPrimitive("user"))
                        put("content", JsonPrimitive(userPrompt))
                        if (!imageBase64Jpeg.isNullOrBlank()) {
                            put("images", buildJsonArray { add(JsonPrimitive(imageBase64Jpeg)) })
                        }
                    })
                }

                val payload = buildJsonObject {
                    put("model", JsonPrimitive(server.selectedModel.ifBlank { "gemma4" }))
                    put("messages", messagesJson)
                    put("stream", JsonPrimitive(true))
                    put("options", buildJsonObject {
                        put("temperature", JsonPrimitive(temperature))
                    })
                }.toString()

                val request = Request.Builder()
                    .url("$cleanBase/api/chat")
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string().orEmpty()
                        throw IllegalStateException("Erro Ollama HTTP ${response.code}: $errBody")
                    }
                    val stream = response.body?.byteStream()
                        ?: throw IllegalStateException("Resposta vazia do servidor Ollama")
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            if (line.isNotBlank()) {
                                val obj = runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull()
                                val token = obj?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                                if (!token.isNullOrEmpty()) {
                                    emit(token)
                                }
                                val done = obj?.get("done")?.jsonPrimitive?.contentOrNull == "true"
                                if (done) break
                            }
                            line = reader.readLine()
                        }
                    }
                }
            }

            LanProtocol.OPENAI_COMPATIBLE -> {
                val messagesJson = buildJsonArray {
                    add(buildJsonObject {
                        put("role", JsonPrimitive("system"))
                        put("content", JsonPrimitive(systemPrompt))
                    })
                    history.takeLast(8).forEach { turn ->
                        add(buildJsonObject {
                            put("role", JsonPrimitive(if (turn.isUser) "user" else "assistant"))
                            put("content", JsonPrimitive(turn.content))
                        })
                    }
                    add(buildJsonObject {
                        put("role", JsonPrimitive("user"))
                        if (imageBase64Jpeg.isNullOrBlank()) {
                            put("content", JsonPrimitive(userPrompt))
                        } else {
                            put("content", buildJsonArray {
                                add(buildJsonObject {
                                    put("type", JsonPrimitive("text"))
                                    put("text", JsonPrimitive(userPrompt))
                                })
                                add(buildJsonObject {
                                    put("type", JsonPrimitive("image_url"))
                                    put("image_url", buildJsonObject {
                                        put("url", JsonPrimitive("data:image/jpeg;base64,$imageBase64Jpeg"))
                                    })
                                })
                            })
                        }
                    })
                }

                val payload = buildJsonObject {
                    put("model", JsonPrimitive(server.selectedModel.ifBlank { "local-model" }))
                    put("messages", messagesJson)
                    put("stream", JsonPrimitive(true))
                    put("temperature", JsonPrimitive(temperature))
                }.toString()

                val request = Request.Builder()
                    .url("$cleanBase/v1/chat/completions")
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string().orEmpty()
                        throw IllegalStateException("Erro Servidor LAN HTTP ${response.code}: $errBody")
                    }
                    val stream = response.body?.byteStream()
                        ?: throw IllegalStateException("Resposta vazia do servidor LAN")
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            val trimmed = line.trim()
                            if (trimmed.startsWith("data:")) {
                                val data = trimmed.removePrefix("data:").trim()
                                if (data == "[DONE]") break
                                val obj = runCatching { json.parseToJsonElement(data).jsonObject }.getOrNull()
                                val delta = obj?.get("choices")
                                    ?.jsonArray
                                    ?.firstOrNull()
                                    ?.jsonObject
                                    ?.get("delta")
                                    ?.jsonObject
                                    ?.get("content")
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                if (!delta.isNullOrEmpty()) {
                                    emit(delta)
                                }
                            }
                            line = reader.readLine()
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        fun normalizeBaseUrl(raw: String): String {
            var trimmed = raw.trim().trimEnd('/')
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                trimmed = "http://$trimmed"
            }
            return trimmed
        }
    }
}
