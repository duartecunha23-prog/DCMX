package com.example.data.local

object ModelCatalog {
    const val DEFAULT_PORTUGUESE_SYSTEM_PROMPT =
        "És o Núcleo IA, um assistente inteligente privado que opera offline e em rede local. " +
            "Responde sempre em Português de Portugal claro, direto, rigoroso e prestável."

    fun defaultModels(): List<LocalModelEntity> = listOf(
        LocalModelEntity(
            id = "gemma-4-e2b-it",
            name = "Gemma 4 E2B (Instruct)",
            family = "Gemma 4",
            repoId = "litert-community/gemma-4-E2B-it-litert-lm",
            fileName = "gemma-4-E2B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            expectedSizeBytes = 2_600_000_000L,
            backendPreference = AiBackendPreference.GPU.name,
            description = "Modelo principal Gemma 4 E2B otimizado para execução local offline em CPU e GPU via LiteRT-LM. Excelente compreensão em Português.",
            isFeatured = true,
            isCustom = false
        ),
        LocalModelEntity(
            id = "qwen3-0.6b",
            name = "Qwen 3 0.6B (Ultra Leve)",
            family = "Qwen 3",
            repoId = "litert-community/Qwen3-0.6B",
            fileName = "Qwen3-0.6B.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm",
            expectedSizeBytes = 614_000_000L,
            backendPreference = AiBackendPreference.CPU.name,
            description = "Modelo compacto de 0.6B parâmetros para arranque rápido e dispositivos com menor memória RAM. Funciona em CPU e GPU.",
            isFeatured = false,
            isCustom = false
        )
    )

    fun defaultLanServers(): List<LanServerEntity> = listOf(
        LanServerEntity(
            name = "Ollama Rede Local",
            baseUrl = "http://192.168.1.100:11434",
            protocol = LanProtocol.OLLAMA.name,
            selectedModel = "gemma4",
            isActive = true
        ),
        LanServerEntity(
            name = "LM Studio / Llama.cpp LAN",
            baseUrl = "http://192.168.1.100:1234",
            protocol = LanProtocol.OPENAI_COMPATIBLE.name,
            selectedModel = "gemma-4-e2b-it",
            isActive = false
        )
    )
}
