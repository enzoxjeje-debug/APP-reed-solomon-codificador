package com.example.reedsolomonlab.data

/** Tipo de mensaje introducido por la persona usuaria. */
enum class MessageType { TEXTO, BINARIO }

/** Cómo se representan los símbolos en pantalla. */
enum class SymbolRepresentation { DECIMAL, HEXADECIMAL, BINARIO }

/** Configuración del código Reed-Solomon. */
data class RSConfig(
    /** Símbolos de datos por bloque (k). */
    val dataSymbols: Int = DEFAULT_DATA_SYMBOLS,
    /** Símbolos de paridad por bloque (2t). */
    val paritySymbols: Int = DEFAULT_PARITY_SYMBOLS
) {
    val blockLength: Int get() = dataSymbols + paritySymbols

    /**
     * Capacidad teórica de corrección: con 2t símbolos de paridad
     * se pueden corregir hasta t símbolos erróneos por bloque.
     */
    val correctionCapacity: Int get() = paritySymbols / 2

    fun validationError(): String? = when {
        dataSymbols < 1 -> "El número de símbolos de datos debe ser al menos 1."
        paritySymbols < 2 -> "El número de símbolos de paridad debe ser al menos 2 (para corregir 1 error se necesitan 2t símbolos)."
        paritySymbols % 2 != 0 -> "Usa un número par de símbolos de paridad: si añadimos 2t símbolos de paridad, el código corrige hasta t errores."
        dataSymbols + paritySymbols > 255 -> "La suma de datos + paridad no puede superar 255 símbolos (tamaño máximo del bloque en GF(256))."
        else -> null
    }

    val isValid: Boolean get() = validationError() == null

    companion object {
        const val DEFAULT_DATA_SYMBOLS = 8
        const val DEFAULT_PARITY_SYMBOLS = 4
        const val MAX_TOTAL_SYMBOLS = 96

        /** Cabecera canónica que el codificador genera para copiar/pegar. Ejemplo: "RS1 k=8 nsym=4 | 72 111 …" */
        const val HEADER_MAGIC = "RS1"

        fun headerFor(config: RSConfig): String =
            "$HEADER_MAGIC k=${config.dataSymbols} nsym=${config.paritySymbols} |"
    }
}
