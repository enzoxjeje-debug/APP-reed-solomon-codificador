package com.example.reedsolomonlab.data

import java.nio.charset.StandardCharsets

/**
 * Conversiones entre texto, binario y símbolos Reed-Solomon (bytes 0..255).
 *
 * Todo el texto se codifica en UTF-8: cada carácter ocupa 1 o más bytes
 * (símbolos). Los caracteres con tilde o la «ñ» ocupan 2 bytes, y se explica
 * en la pantalla educativa.
 */
object MessageCodec {

    sealed class ParseResult {
        data class Ok(val symbols: IntArray, val note: String? = null) : ParseResult()
        data class Err(val message: String) : ParseResult()
    }

    /** Texto → símbolos (bytes UTF-8 sin signo). */
    fun textToSymbols(text: String): IntArray {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        return IntArray(bytes.size) { bytes[it].toInt() and 0xFF }
    }

    /** Símbolos → texto (decodificación UTF-8). */
    fun symbolsToText(symbols: IntArray): String =
        symbolsToTextOrNull(symbols) ?: symbols.joinToString("") {
            if (it in 32..126) it.toChar().toString() else "�"
        }

    fun symbolsToTextOrNull(symbols: IntArray): String? = try {
        val bytes = ByteArray(symbols.size) { symbols[it].toByte() }
        String(bytes, StandardCharsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    /**
     * Entrada binaria → símbolos.
     * Acepta bytes separados por espacios ("01001000 01101111") o una tira
     * continua múltiplo de 8 bits. También acepta comas.
     */
    fun parseBinary(input: String): ParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ParseResult.Err("El campo está vacío. Escribe o pega datos binarios.")
        val invalid = trimmed.filter { it != '0' && it != '1' && !it.isWhitespace() && it != ',' && it != ';' }
        if (invalid.isNotEmpty()) {
            val bad = invalid.take(6).toSet().joinToString(" ") { "'$it'" }
            return ParseResult.Err("Carácter no válido en modo binario: $bad. Solo se aceptan 0 y 1 (y espacios entre bytes).")
        }
        val withoutSeparators = trimmed.replace(Regex("[\\s,;]+"), " ")
        val groups = withoutSeparators.trim().split(" ").filter { it.isNotEmpty() }
        val bits = if (groups.size == 1 && groups[0].length > 8) {
            // Tira continua: debe ser múltiplo de 8.
            val raw = groups[0]
            if (raw.length % 8 != 0) {
                return ParseResult.Err(
                    "La tira binaria tiene ${raw.length} bits y no es múltiplo de 8. " +
                        "Cada símbolo son 8 bits: separa los bytes con espacios, por ejemplo «01001000 01101111»."
                )
            }
            raw.chunked(8)
        } else {
            for (g in groups) {
                if (g.length != 8) {
                    return ParseResult.Err(
                        "El grupo «$g» tiene ${g.length} bits. Cada símbolo debe tener exactamente 8 bits."
                    )
                }
            }
            groups
        }
        if (bits.isEmpty()) return ParseResult.Err("No se encontró ningún byte binario.")
        return ParseResult.Ok(IntArray(bits.size) { bits[it].toInt(2) })
    }

    /** Símbolos → texto binario separado por espacios. */
    fun symbolsToBinary(symbols: IntArray): String =
        symbols.joinToString(" ") { it.toBinary() }

    /**
     * Lista de símbolos decimales ("72 111 108 97" o con comas) → símbolos.
     * Si la entrada empieza por la cabecera canónica «RS1 k=.. nsym=.. len=.. |»,
     * también se extraen la configuración y la longitud real de los datos.
     */
    data class SymbolList(val symbols: IntArray, val config: RSConfig?, val dataLength: Int?)

    fun parseSymbolList(input: String, fallback: RSConfig): ParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ParseResult.Err("El campo está vacío. Pega un mensaje codificado.")
        var body = trimmed
        var config: RSConfig? = null
        var dataLength: Int? = null
        if (trimmed.startsWith(RSConfig.HEADER_MAGIC)) {
            val parts = trimmed.split("|", limit = 2)
            if (parts.size < 2) return ParseResult.Err("La cabecera «${RSConfig.HEADER_MAGIC}» existe pero falta el cuerpo con los símbolos después de «|».")
            val header = parts[0]
            body = parts[1]
            val k = Regex("k\\s*=\\s*(\\d+)").find(header)?.groupValues?.get(1)?.toIntOrNull()
            val nsym = Regex("nsym\\s*=\\s*(\\d+)").find(header)?.groupValues?.get(1)?.toIntOrNull()
            if (k == null || nsym == null) {
                return ParseResult.Err("La cabecera es ilegible. Debe tener el formato «${RSConfig.headerFor(fallback)} …».")
            }
            val parsed = RSConfig(k, nsym)
            val error = parsed.validationError()
            if (error != null) return ParseResult.Err("La configuración de la cabecera no es válida: $error")
            config = parsed
            Regex("len\\s*=\\s*(\\d+)").find(header)?.groupValues?.get(1)?.toIntOrNull()?.let {
                dataLength = it
            }
        }
        val tokens = body.trim().split(Regex("[\\s,;]+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return ParseResult.Err("No se encontró ningún símbolo. Escribe números del 0 al 255 separados por espacios.")
        val nonNumeric = tokens.firstOrNull { it.toIntOrNull() == null }
        if (nonNumeric != null) {
            return ParseResult.Err("«$nonNumeric» no es un número válido. En este modo solo se aceptan símbolos del 0 al 255 separados por espacios o comas.")
        }
        val outOfRange = tokens.mapNotNull { it.toIntOrNull() }.firstOrNull { it !in 0..255 }
        if (outOfRange != null) {
            return ParseResult.Err("El valor $outOfRange está fuera de rango. Cada símbolo debe estar entre 0 y 255.")
        }
        return SymbolListResult(tokens.map { it.toInt() }.toIntArray(), config, dataLength)
    }

    /** Resultado con símbolos y (opcionalmente) la configuración leída de la cabecera. */
    class SymbolListResult(val symbols: IntArray, val config: RSConfig?, val dataLength: Int?) : ParseResult()

    fun extractSymbolList(result: ParseResult): SymbolList? = when (result) {
        is SymbolListResult -> SymbolList(result.symbols, result.config, result.dataLength)
        is ParseResult.Ok -> SymbolList(result.symbols, null, null)
        is ParseResult.Err -> null
    }

    /** ¿La entrada parece una lista de símbolos (sin cabecera)? */
    fun looksLikeSymbolList(input: String): Boolean {
        val t = input.trim()
        if (t.isEmpty()) return false
        if (t.startsWith(RSConfig.HEADER_MAGIC)) return true
        val tokens = t.split(Regex("[\\s,;]+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return false
        return tokens.all { tok ->
            val n = tok.toIntOrNull()
            n != null && n in 0..255
        }
    }

    /**
     * Representación para copiar: solo los números separados por espacios,
     * p. ej. «72 111 108 97 …». El usuario nunca escribe parámetros: el
     * decodificador deduce solo la configuración (k, paridad) y la longitud
     * real. Los parámetros [config] y [dataLength] se conservan en la firma
     * por compatibilidad, pero ya no forman parte del texto copiado.
     * La cabecera antigua «RS1 k=.. nsym=.. len=.. | …» sigue aceptándose
     * al pegar, por compatibilidad con mensajes copiados antes.
     */
    fun toShareableString(symbols: IntArray, config: RSConfig, dataLength: Int): String =
        symbols.joinToString(" ")
}
