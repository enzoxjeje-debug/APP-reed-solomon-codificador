package com.example.reedsolomonlab.viewmodel

import androidx.lifecycle.ViewModel
import com.example.reedsolomonlab.audio.SoundManager
import com.example.reedsolomonlab.data.RSConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Datos para traspasar un mensaje codificado a la pestaña del decodificador.
 * Viajan estructurados (números + configuración + longitud real) para que el
 * usuario nunca tenga que escribir parámetros como k o paridad.
 */
data class DecoderHandoff(
    val symbols: String,
    val config: RSConfig,
    val dataLength: Int,
    /** Hace única cada importación para que repetir el botón siempre reimporte. */
    val nonce: Long = 0L
)

/**
 * Estado compartido de la app: ajustes (sonido, modo principiante),
 * sonidos sintetizados y traspaso codificador → decodificador.
 */
class AppViewModel : ViewModel() {

    val sounds = SoundManager()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _beginnerMode = MutableStateFlow(true)
    val beginnerMode: StateFlow<Boolean> = _beginnerMode.asStateFlow()

    private val _decoderHandoff = MutableStateFlow<DecoderHandoff?>(null)
    val decoderHandoff: StateFlow<DecoderHandoff?> = _decoderHandoff.asStateFlow()

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        sounds.enabled = enabled
        if (enabled) sounds.play(SoundManager.Sound.CLICK)
    }

    fun setBeginnerMode(beginner: Boolean) {
        _beginnerMode.value = beginner
        play(SoundManager.Sound.CLICK)
    }

    fun play(sound: SoundManager.Sound) = sounds.play(sound)

    private var handoffSeq = 0L

    fun sendToDecoder(symbols: String, config: RSConfig, dataLength: Int) {
        handoffSeq++
        _decoderHandoff.value = DecoderHandoff(symbols, config, dataLength, handoffSeq)
    }

    fun consumeHandoff() {
        _decoderHandoff.value = null
    }

    override fun onCleared() {
        sounds.release()
        super.onCleared()
    }
}
