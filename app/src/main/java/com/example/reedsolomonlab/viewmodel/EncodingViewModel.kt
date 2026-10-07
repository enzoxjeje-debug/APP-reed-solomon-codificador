package com.example.reedsolomonlab.viewmodel

import androidx.lifecycle.ViewModel
import com.example.reedsolomonlab.audio.SoundManager
import com.example.reedsolomonlab.data.MessageCodec
import com.example.reedsolomonlab.data.MessageType
import com.example.reedsolomonlab.data.RSConfig
import com.example.reedsolomonlab.domain.reedsolomon.ReedSolomon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EncodingUiState(
    val messageType: MessageType = MessageType.TEXTO,
    val textInput: String = "Hola mundo",
    val binaryInput: String = "01001000 01101111 01101100 01100001",
    val config: RSConfig = RSConfig(),
    val configDraftData: String = RSConfig.DEFAULT_DATA_SYMBOLS.toString(),
    val configDraftParity: String = RSConfig.DEFAULT_PARITY_SYMBOLS.toString(),
    val showAdvanced: Boolean = false,
    val result: ReedSolomon.EncodingTrace? = null,
    val error: String? = null
)

class EncodingViewModel : ViewModel() {

    private val _ui = MutableStateFlow(EncodingUiState())
    val ui: StateFlow<EncodingUiState> = _ui.asStateFlow()

    fun setMessageType(type: MessageType, onSound: (SoundManager.Sound) -> Unit) {
        onSound(SoundManager.Sound.CLICK)
        _ui.update { it.copy(messageType = type, result = null, error = null) }
    }

    fun setText(text: String) {
        _ui.update { it.copy(textInput = text, result = null, error = null) }
    }

    fun setBinary(text: String) {
        _ui.update { it.copy(binaryInput = text, result = null, error = null) }
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
                config = if (k != null && nsym != null) {
                    val candidate = RSConfig(k, nsym)
                    if (candidate.isValid) candidate else it.config
                } else it.config
            )
        }
    }

    fun loadExample(onSound: (SoundManager.Sound) -> Unit) {
        onSound(SoundManager.Sound.NAVIGATE)
        _ui.update {
            it.copy(
                messageType = MessageType.TEXTO,
                textInput = "Hola Reed-Solomon",
                result = null,
                error = null
            )
        }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }

    fun encode(beginnerMode: Boolean, onSound: (SoundManager.Sound) -> Unit) {
        val state = _ui.value
        val configError = state.config.validationError()
        if (configError != null) {
            _ui.update { it.copy(error = configError) }
            return
        }
        val symbols: IntArray
        val inputText: String?
        val breakdown: List<ReedSolomon.CharBreakdown>
        when (state.messageType) {
            MessageType.TEXTO -> {
                if (state.textInput.isEmpty()) {
                    _ui.update { it.copy(error = "Escribe un mensaje primero. Por ejemplo: «Hola mundo».") }
                    return
                }
                symbols = MessageCodec.textToSymbols(state.textInput)
                inputText = state.textInput
                breakdown = state.textInput.map { c ->
                    ReedSolomon.CharBreakdown(
                        char = c.toString(),
                        bytes = c.toString().toByteArray(Charsets.UTF_8).map { b -> b.toInt() and 0xFF }
                    )
                }
            }
            MessageType.BINARIO -> {
                when (val r = MessageCodec.parseBinary(state.binaryInput)) {
                    is MessageCodec.ParseResult.Err -> {
                        _ui.update { it.copy(error = r.message) }
                        return
                    }
                    is MessageCodec.ParseResult.Ok -> {
                        symbols = r.symbols
                        inputText = null
                        breakdown = emptyList()
                    }
                    else -> {
                        _ui.update { it.copy(error = "Entrada no válida.") }
                        return
                    }
                }
            }
        }
        if (symbols.size > RSConfig.MAX_TOTAL_SYMBOLS) {
            _ui.update {
                it.copy(
                    error = "El mensaje tiene ${symbols.size} símbolos y el máximo es " +
                        "${RSConfig.MAX_TOTAL_SYMBOLS}, para que la explicación paso a paso siga siendo clara."
                )
            }
            return
        }
        try {
            val codec = ReedSolomon(state.config.dataSymbols, state.config.paritySymbols)
            val trace = codec.buildEncodingTrace(symbols, state.config, inputText, breakdown, beginnerMode)
            _ui.update { it.copy(result = trace, error = null) }
            onSound(SoundManager.Sound.ENCODE_DONE)
        } catch (e: IllegalArgumentException) {
            _ui.update { it.copy(error = e.message ?: "Parámetros no válidos.") }
        }
    }
}
