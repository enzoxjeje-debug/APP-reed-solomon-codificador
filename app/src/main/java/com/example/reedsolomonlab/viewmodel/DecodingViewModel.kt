package com.example.reedsolomonlab.viewmodel

import androidx.lifecycle.ViewModel
import com.example.reedsolomonlab.audio.SoundManager
import com.example.reedsolomonlab.data.MessageCodec
import com.example.reedsolomonlab.data.RSConfig
import com.example.reedsolomonlab.domain.reedsolomon.ReedSolomon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class DecoderInputMode { SIMBOLOS, BINARIO }

/** Cómo se interpretó la entrada de texto libre. */
enum class InterpretedAs { SIMBOLOS, TEXTO }

/** De dónde salió la configuración actual: importada, deducida o leída. */
enum class ConfigSource { NONE, IMPORTED, DETECTED, HEADER }

data class DecodingUiState(
    val inputMode: DecoderInputMode = DecoderInputMode.SIMBOLOS,
    val rawInput: String = "",
    val interpretedAs: InterpretedAs? = null,
    val configSource: ConfigSource = ConfigSource.NONE,
    /** Símbolos tal como se recibieron (sin simulaciones). */
    val parsedSymbols: IntArray? = null,
    /** Símbolos de trabajo (con errores simulados aplicados). */
    val workingSymbols: IntArray? = null,
    val modifiedPositions: Set<Int> = emptySet(),
    val config: RSConfig = RSConfig(),
    val configDraftData: String = RSConfig.DEFAULT_DATA_SYMBOLS.toString(),
    val configDraftParity: String = RSConfig.DEFAULT_PARITY_SYMBOLS.toString(),
    val showAdvanced: Boolean = false,
    val dataLength: Int? = null,
    val result: ReedSolomon.DecodingTrace? = null,
    val error: String? = null
) {
    val hasSimulation: Boolean get() = modifiedPositions.isNotEmpty()
}

class DecodingViewModel : ViewModel() {

    private val _ui = MutableStateFlow(DecodingUiState())
    val ui: StateFlow<DecodingUiState> = _ui.asStateFlow()

    fun setInputMode(mode: DecoderInputMode, onSound: (SoundManager.Sound) -> Unit) {
        onSound(SoundManager.Sound.CLICK)
        _ui.update {
            it.copy(inputMode = mode, result = null, error = null)
        }
    }

    fun setRawInput(text: String) {
        _ui.update { it.copy(rawInput = text) }
    }

    fun setShowAdvanced(show: Boolean) {
        _ui.update { it.copy(showAdvanced = show) }
    }

    fun setConfigDraft(data: String, parity: String) {
        val k = data.toIntOrNull()
        val nsym = parity.toIntOrNull()
        _ui.update {
            it.copy(
                configDraftData = data,
                configDraftParity = parity,
                result = null,
                error = null,
                // Ajuste manual: la configuración deja de ser la importada.
                configSource = ConfigSource.NONE,
                config = if (k != null && nsym != null) {
                    val candidate = RSConfig(k, nsym)
                    if (candidate.isValid) candidate else it.config
                } else it.config
            )
        }
    }

    fun applyConfigFromHeader(config: RSConfig) {
        _ui.update {
            it.copy(
                config = config,
                configDraftData = config.dataSymbols.toString(),
                configDraftParity = config.paritySymbols.toString()
            )
        }
    }

    fun loadHandoff(handoff: DecoderHandoff) {
        // El traspaso trae la configuración exacta del codificador: se aplica
        // y se muestra expandida para que quede clara sin tocar nada a mano.
        _ui.update {
            it.copy(
                inputMode = DecoderInputMode.SIMBOLOS,
                rawInput = handoff.symbols,
                config = handoff.config,
                configDraftData = handoff.config.dataSymbols.toString(),
                configDraftParity = handoff.config.paritySymbols.toString(),
                showAdvanced = true,
                dataLength = handoff.dataLength,
                interpretedAs = InterpretedAs.SIMBOLOS,
                configSource = ConfigSource.IMPORTED,
                parsedSymbols = null,
                workingSymbols = null,
                modifiedPositions = emptySet(),
                result = null,
                error = null
            )
        }
        parseInput()
    }

    fun loadCorruptedExample(onSound: (SoundManager.Sound) -> Unit) {
        onSound(SoundManager.Sound.NAVIGATE)
        val config = RSConfig()
        val codec = ReedSolomon(config.dataSymbols, config.paritySymbols)
        val text = "Hola Reed-Solomon"
        val symbols = MessageCodec.textToSymbols(text)
        val trace = codec.buildEncodingTrace(symbols, config, text, emptyList(), true)
        // Corrompe exactamente un símbolo (posición 5) con un valor distinto.
        val corrupted = trace.codeword.copyOf()
        corrupted[5] = (corrupted[5] + 142) % 256
        _ui.update {
            it.copy(
                inputMode = DecoderInputMode.SIMBOLOS,
                rawInput = corrupted.joinToString(" "),
                config = config,
                configDraftData = config.dataSymbols.toString(),
                configDraftParity = config.paritySymbols.toString(),
                showAdvanced = true,
                dataLength = symbols.size,
                interpretedAs = InterpretedAs.SIMBOLOS,
                configSource = ConfigSource.IMPORTED,
                parsedSymbols = null,
                workingSymbols = null,
                modifiedPositions = emptySet(),
                result = null,
                error = null
            )
        }
        parseInput()
    }

    /** Interpreta el texto pegado y prepara los símbolos de trabajo. */
    fun parseInput() {
        val state = _ui.value
        val input = state.rawInput
        if (input.isBlank()) {
            _ui.update {
                it.copy(
                    error = "Pega un mensaje codificado o pulsa «Cargar ejemplo con error».",
                    parsedSymbols = null, workingSymbols = null,
                    interpretedAs = null, configSource = ConfigSource.NONE,
                    result = null, modifiedPositions = emptySet()
                )
            }
            return
        }
        when (state.inputMode) {
            DecoderInputMode.BINARIO -> {
                when (val r = MessageCodec.parseBinary(input)) {
                    is MessageCodec.ParseResult.Err -> _ui.update {
                        it.copy(
                            error = r.message, parsedSymbols = null, workingSymbols = null,
                            result = null, modifiedPositions = emptySet()
                        )
                    }
                    is MessageCodec.ParseResult.Ok -> _ui.update {
                        it.copy(
                            interpretedAs = null,
                            parsedSymbols = r.symbols, workingSymbols = r.symbols.copyOf(),
                            modifiedPositions = emptySet(), result = null, error = null,
                            dataLength = null
                        )
                    }
                    else -> _ui.update { it.copy(error = "Entrada no válida.") }
                }
            }
            DecoderInputMode.SIMBOLOS -> {
                if (MessageCodec.looksLikeSymbolList(input)) {
                    when (val r = MessageCodec.parseSymbolList(input, state.config)) {
                        is MessageCodec.ParseResult.Err -> _ui.update {
                            it.copy(
                                error = r.message, parsedSymbols = null, workingSymbols = null,
                                result = null, modifiedPositions = emptySet(), dataLength = null
                            )
                        }
                        else -> {
                            val list = MessageCodec.extractSymbolList(r)!!
                            if (list.config != null) {
                                // Cabecera antigua «RS1 …»: se respeta tal cual.
                                applyConfigFromHeader(list.config)
                                _ui.update {
                                    it.copy(
                                        interpretedAs = InterpretedAs.SIMBOLOS,
                                        configSource = ConfigSource.HEADER,
                                        parsedSymbols = list.symbols, workingSymbols = list.symbols.copyOf(),
                                        modifiedPositions = emptySet(), result = null, error = null,
                                        dataLength = list.dataLength
                                    )
                                }
                            } else if (state.dataLength != null && state.parsedSymbols == null) {
                                // Traspaso del codificador o ejemplo: la configuración
                                // y la longitud ya están aplicadas en el estado.
                                _ui.update {
                                    it.copy(
                                        interpretedAs = InterpretedAs.SIMBOLOS,
                                        parsedSymbols = list.symbols, workingSymbols = list.symbols.copyOf(),
                                        modifiedPositions = emptySet(), result = null, error = null
                                    )
                                }
                                // Nota: configSource ya viene como IMPORTED desde el traspaso.
                            } else {
                                // Pegado manual de solo números: deducir la configuración.
                                val detected = detectRsConfig(list.symbols, state.config)
                                if (detected == null) {
                                    _ui.update {
                                        it.copy(
                                            error = "No se pudo deducir la configuración del mensaje. " +
                                                "Revisa que sean solo números del 0 al 255 separados por espacios, " +
                                                "o ajústala manualmente en «Configuración avanzada».",
                                            parsedSymbols = null, workingSymbols = null,
                                            result = null, modifiedPositions = emptySet(), dataLength = null
                                        )
                                    }
                                } else {
                                    applyConfigFromHeader(detected)
                                    _ui.update {
                                        it.copy(
                                            interpretedAs = InterpretedAs.SIMBOLOS,
                                            configSource = ConfigSource.DETECTED,
                                            parsedSymbols = list.symbols, workingSymbols = list.symbols.copyOf(),
                                            modifiedPositions = emptySet(), result = null, error = null,
                                            // Sin cabecera no se conoce la longitud real:
                                            // se deducirá recortando el relleno al decodificar.
                                            dataLength = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Texto libre: se interpreta como bytes UTF-8.
                    val symbols = MessageCodec.textToSymbols(input)
                    if (symbols.size > RSConfig.MAX_TOTAL_SYMBOLS) {
                        _ui.update {
                            it.copy(
                                error = "El texto tiene ${symbols.size} símbolos y el máximo es " +
                                    "${RSConfig.MAX_TOTAL_SYMBOLS}.",
                                parsedSymbols = null, workingSymbols = null, result = null
                            )
                        }
                        return
                    }
                    _ui.update {
                        it.copy(
                            interpretedAs = InterpretedAs.TEXTO,
                            parsedSymbols = symbols, workingSymbols = symbols.copyOf(),
                            modifiedPositions = emptySet(), result = null, error = null,
                            dataLength = null
                        )
                    }
                }
            }
        }
    }

    /** Simula un error manual: cambia el símbolo de [position] a [newValue]. */
    fun applySimulatedError(position: Int, newValue: Int, onSound: (SoundManager.Sound) -> Unit) {
        val state = _ui.value
        val working = state.workingSymbols ?: return
        if (position !in working.indices) {
            _ui.update { it.copy(error = "La posición $position no existe (0–${working.size - 1}).") }
            return
        }
        if (newValue !in 0..255) {
            _ui.update { it.copy(error = "El valor debe estar entre 0 y 255.") }
            return
        }
        val updated = working.copyOf()
        updated[position] = newValue
        val modified = if (state.parsedSymbols != null && newValue != state.parsedSymbols[position]) {
            state.modifiedPositions + position
        } else {
            state.modifiedPositions - position
        }
        _ui.update { it.copy(workingSymbols = updated, modifiedPositions = modified, result = null, error = null) }
        onSound(SoundManager.Sound.ERROR_DETECTED)
    }

    /** Corrompe un símbolo aleatorio (valor garantizado distinto). */
    fun simulateRandomError(onSound: (SoundManager.Sound) -> Unit, random: Random = Random.Default) {
        val state = _ui.value
        val working = state.workingSymbols
        if (working == null || working.isEmpty()) {
            _ui.update { it.copy(error = "Primero interpreta un mensaje con «Leer mensaje».") }
            return
        }
        val position = random.nextInt(working.size)
        var newValue = random.nextInt(256)
        while (newValue == working[position]) newValue = random.nextInt(256)
        applySimulatedError(position, newValue, onSound)
    }

    fun resetSimulation(onSound: (SoundManager.Sound) -> Unit) {
        val parsed = _ui.value.parsedSymbols ?: return
        onSound(SoundManager.Sound.CLICK)
        _ui.update {
            it.copy(workingSymbols = parsed.copyOf(), modifiedPositions = emptySet(), result = null, error = null)
        }
    }

    fun decode(beginnerMode: Boolean, onSound: (SoundManager.Sound) -> Unit) {
        val state = _ui.value
        val working = state.workingSymbols
        if (working == null) {
            _ui.update { it.copy(error = "Primero interpreta un mensaje con «Leer mensaje».") }
            return
        }
        val configError = state.config.validationError()
        if (configError != null) {
            _ui.update { it.copy(error = configError) }
            return
        }
        try {
            val codec = ReedSolomon(state.config.dataSymbols, state.config.paritySymbols)
            // Si la longitud real no se conoce (pegado manual sin cabecera),
            // primero se decodifica completa y luego se recorta el relleno
            // final de ceros para recuperar el mensaje original.
            val first = codec.buildDecodingTrace(working, state.config, state.dataLength, beginnerMode)
            val trace = if (state.dataLength == null && first.errorMessage == null) {
                val trimmed = trimTrailingZeros(first.recoveredSymbols)
                if (trimmed == first.recoveredSymbols.size) first
                else codec.buildDecodingTrace(working, state.config, trimmed, beginnerMode)
            } else first
            if (trace.errorMessage != null) {
                _ui.update { it.copy(error = trace.errorMessage, result = null) }
                onSound(SoundManager.Sound.ERROR_DETECTED)
                return
            }
            _ui.update { it.copy(result = trace, error = null) }
            when (trace.status) {
                ReedSolomon.DecodeStatus.NO_ERROR -> onSound(SoundManager.Sound.SUCCESS)
                ReedSolomon.DecodeStatus.CORRECTED -> onSound(SoundManager.Sound.CORRECTED)
                ReedSolomon.DecodeStatus.TOO_MANY_ERRORS -> onSound(SoundManager.Sound.ERROR_DETECTED)
            }
        } catch (e: IllegalArgumentException) {
            _ui.update { it.copy(error = e.message ?: "Parámetros no válidos.") }
        }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }
}

/** Quita los ceros de relleno del final; si todo son ceros, se conserva todo. */
private fun trimTrailingZeros(symbols: IntArray): Int {
    var end = symbols.size
    while (end > 0 && symbols[end - 1] == 0) end--
    return if (end == 0) symbols.size else end
}

/**
 * Deduce la configuración RS de una lista de solo números, sin cabecera.
 * Primero prueba [preferred] (la seleccionada en la app) y luego barre
 * candidatos de bloque corto a largo con paridad de mayor a menor.
 * Solo se acepta un candidato si TODOS sus bloques tienen síndromes cero
 * (mensaje intacto): aceptar mensajes dañados sería ambiguo, porque con
 * paridad mínima casi cualquier palabra «corrige» por casualidad y se
 * devolverían datos falsos. Los mensajes con errores llegan por el traspaso
 * del codificador o el ejemplo, que traen su configuración exacta.
 * Devuelve null si ningún candidato valida el mensaje.
 */
fun detectRsConfig(symbols: IntArray, preferred: RSConfig): RSConfig? {
    if (symbols.isEmpty()) return null
    val candidates = mutableListOf<RSConfig>()
    val n0 = preferred.dataSymbols + preferred.paritySymbols
    if (preferred.isValid && symbols.size % n0 == 0) candidates.add(preferred)
    val maxN = minOf(symbols.size, 255)
    for (n in 2..maxN) {
        if (symbols.size % n != 0) continue
        var nsym = minOf(n - 1, 32)
        if (nsym % 2 != 0) nsym--
        while (nsym >= 2) {
            val candidate = RSConfig(n - nsym, nsym)
            if (candidate != preferred && candidate.isValid) candidates.add(candidate)
            nsym -= 2
        }
    }
    for (candidate in candidates) {
        if (validatesPristine(candidate, symbols)) return candidate
    }
    return null
}

private fun validatesPristine(config: RSConfig, symbols: IntArray): Boolean {
    val codec = ReedSolomon(config.dataSymbols, config.paritySymbols)
    val n = config.blockLength
    if (symbols.size % n != 0) return false
    for (b in 0 until symbols.size / n) {
        val block = symbols.copyOfRange(b * n, (b + 1) * n)
        if (!codec.computeSyndromes(block).all { it == 0 }) return false
    }
    return true
}
