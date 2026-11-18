package com.example.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

data class CodeInterpretation(
    val formatName: String,
    val semanticType: String,
    val title: String,
    val structuredSummary: String,
    val securityHint: String? = null
)

object BarcodeInterpreter {

    fun formatName(formatCode: Int): String = when (formatCode) {
        Barcode.FORMAT_QR_CODE -> "QR Code"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_CODABAR -> "Codabar"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "Aztec"
        else -> "Código"
    }

    fun interpret(rawValue: String, formatCode: Int): CodeInterpretation {
        val trimmed = rawValue.trim()
        val fmt = formatName(formatCode)

        // 1. Wi-Fi QR
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val ssid = extractWifiField(trimmed, "S") ?: "Desconhecida"
            val auth = extractWifiField(trimmed, "T") ?: "WPA/WPA2"
            val pass = extractWifiField(trimmed, "P") ?: "(Sem palavra-passe)"
            return CodeInterpretation(
                formatName = fmt,
                semanticType = "Rede Wi-Fi",
                title = "Wi-Fi: $ssid",
                structuredSummary = "SSID: $ssid\nSegurança: $auth\nPalavra-passe: $pass",
                securityHint = if (auth.equals("nopass", ignoreCase = true)) {
                    "Rede aberta sem encriptação."
                } else {
                    "Credenciais de rede local Wi-Fi prontas a utilizar."
                }
            )
        }

        // 2. URL
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            val isHttps = trimmed.startsWith("https://", ignoreCase = true)
            val host = trimmed.substringAfter("://").substringBefore("/").substringBefore("?")
            return CodeInterpretation(
                formatName = fmt,
                semanticType = "Ligação Web (URL)",
                title = host.ifBlank { "Endereço Web" },
                structuredSummary = "Domínio: $host\nProtocolo: ${if (isHttps) "HTTPS (Encriptado)" else "HTTP (Não encriptado)"}\nEndereço completo: $trimmed",
                securityHint = if (!isHttps) {
                    "Atenção: ligação HTTP sem encriptação TLS."
                } else {
                    "Verifique o domínio '$host' antes de abrir no navegador."
                }
            )
        }

        // 3. vCard
        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true)) {
            val fn = Regex("FN:(.*)").find(trimmed)?.groupValues?.getOrNull(1)?.trim() ?: "Contacto"
            val tel = Regex("TEL[^:]*:(.*)").find(trimmed)?.groupValues?.getOrNull(1)?.trim()
            val email = Regex("EMAIL[^:]*:(.*)").find(trimmed)?.groupValues?.getOrNull(1)?.trim()
            val summary = buildString {
                appendLine("Nome: $fn")
                if (!tel.isNullOrBlank()) appendLine("Telefone: $tel")
                if (!email.isNullOrBlank()) appendLine("Email: $email")
            }.trim()
            return CodeInterpretation(
                formatName = fmt,
                semanticType = "Cartão de Contacto (vCard)",
                title = fn,
                structuredSummary = summary
            )
        }

        // 4. Geo
        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            val coords = trimmed.removePrefix("geo:").substringBefore("?")
            return CodeInterpretation(
                formatName = fmt,
                semanticType = "Coordenadas Geográficas",
                title = "Localização ($coords)",
                structuredSummary = "Coordenadas GPS: $coords"
            )
        }

        // 5. Email / Tel / SMS
        if (trimmed.startsWith("mailto:", ignoreCase = true)) {
            val addr = trimmed.removePrefix("mailto:").substringBefore("?")
            return CodeInterpretation(
                formatName = fmt,
                semanticType = "Correio Eletrónico",
                title = addr,
                structuredSummary = "Destinatário: $addr"
            )
        }
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val phone = trimmed.removePrefix("tel:")
            return CodeInterpretation(
                formatName = fmt,
                semanticType = "Número de Telefone",
                title = phone,
                structuredSummary = "Telefone: $phone"
            )
        }

        // 6. EAN-13 / EAN-8 / UPC Retail Barcode
        val digitsOnly = trimmed.all { it.isDigit() }
        if (digitsOnly && (trimmed.length == 13 || trimmed.length == 8 || trimmed.length == 12)) {
            val checksumOk = verifyGtinCheckDigit(trimmed)
            val origin = if (trimmed.length == 13) gs1CountryOrigin(trimmed.take(3)) else "Padrão Internacional"
            return CodeInterpretation(
                formatName = fmt,
                semanticType = "Produto Comercial (GTIN)",
                title = "Código $fmt • $trimmed",
                structuredSummary = buildString {
                    appendLine("Número GTIN: $trimmed")
                    appendLine("Origem GS1: $origin")
                    appendLine("Dígito de controlo: ${if (checksumOk) "Válido" else "Inválido (possível erro de leitura/edição)"}")
                }.trim(),
                securityHint = if (!checksumOk) "O dígito verificador não corresponde ao padrão GTIN." else null
            )
        }

        // 7. JSON
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))
        ) {
            return CodeInterpretation(
                formatName = fmt,
                semanticType = "Dados Estruturados (JSON)",
                title = "Payload JSON (${trimmed.length} car.)",
                structuredSummary = trimmed.take(300)
            )
        }

        // 8. Generic text / industrial barcode
        return CodeInterpretation(
            formatName = fmt,
            semanticType = if (formatCode == Barcode.FORMAT_QR_CODE) "Texto QR" else "Código de Barras",
            title = trimmed.take(36).ifBlank { "Código Vazio" },
            structuredSummary = "Conteúdo bruto (${trimmed.length} caracteres):\n$trimmed"
        )
    }

    fun generateBarcodeBitmap(
        content: String,
        formatCode: Int,
        widthPx: Int = 512,
        heightPx: Int = 512
    ): Bitmap? {
        if (content.isBlank()) return null
        val zxingFormat = mapToZxingFormat(formatCode)
        val is1D = zxingFormat != BarcodeFormat.QR_CODE &&
            zxingFormat != BarcodeFormat.DATA_MATRIX &&
            zxingFormat != BarcodeFormat.AZTEC
        val targetW = widthPx
        val targetH = if (is1D) (widthPx / 2).coerceAtLeast(180) else heightPx

        return runCatching {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 2
            )
            val matrix: BitMatrix = try {
                MultiFormatWriter().encode(content, zxingFormat, targetW, targetH, hints)
            } catch (_: Exception) {
                // Fallback to Code 128 for arbitrary edited 1D strings or QR if 1D fails
                val fallbackFmt = if (is1D) BarcodeFormat.CODE_128 else BarcodeFormat.QR_CODE
                MultiFormatWriter().encode(content, fallbackFmt, targetW, targetH, hints)
            }
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        }.getOrNull()
    }

    private fun mapToZxingFormat(mlKitFormat: Int): BarcodeFormat = when (mlKitFormat) {
        Barcode.FORMAT_EAN_13 -> BarcodeFormat.EAN_13
        Barcode.FORMAT_EAN_8 -> BarcodeFormat.EAN_8
        Barcode.FORMAT_UPC_A -> BarcodeFormat.UPC_A
        Barcode.FORMAT_UPC_E -> BarcodeFormat.UPC_E
        Barcode.FORMAT_CODE_128 -> BarcodeFormat.CODE_128
        Barcode.FORMAT_CODE_39 -> BarcodeFormat.CODE_39
        Barcode.FORMAT_CODE_93 -> BarcodeFormat.CODE_93
        Barcode.FORMAT_CODABAR -> BarcodeFormat.CODABAR
        Barcode.FORMAT_ITF -> BarcodeFormat.ITF
        Barcode.FORMAT_DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
        Barcode.FORMAT_PDF417 -> BarcodeFormat.PDF_417
        Barcode.FORMAT_AZTEC -> BarcodeFormat.AZTEC
        else -> BarcodeFormat.QR_CODE
    }

    private fun extractWifiField(raw: String, key: String): String? {
        val regex = Regex("(?:^|[;:])$key:([^;]*)")
        return regex.find(raw)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun verifyGtinCheckDigit(digits: String): Boolean {
        if (digits.length < 2 || !digits.all { it.isDigit() }) return false
        val body = digits.dropLast(1)
        val expectedCheck = digits.last().digitToInt()
        var sum = 0
        body.reversed().forEachIndexed { idx, c ->
            val weight = if (idx % 2 == 0) 3 else 1
            sum += c.digitToInt() * weight
        }
        val computed = (10 - (sum % 10)) % 10
        return computed == expectedCheck
    }

    private fun gs1CountryOrigin(prefix3: String): String {
        val num = prefix3.toIntOrNull() ?: return "Desconhecido"
        return when (num) {
            in 0..19, in 30..39, in 60..139 -> "EUA / Canadá"
            in 300..379 -> "França / Mónaco"
            in 400..440 -> "Alemanha"
            in 450..459, in 490..499 -> "Japão"
            in 500..509 -> "Reino Unido"
            560 -> "Portugal (GS1 PT)"
            in 840..849 -> "Espanha"
            in 800..839 -> "Itália"
            in 690..699 -> "China"
            in 789..790 -> "Brasil"
            else -> "Prefixo GS1 ($prefix3)"
        }
    }
}
