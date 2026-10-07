package com.example.reedsolomonlab.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.annotation.VisibleForTesting
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Efectos de sonido futuristas generados por síntesis (sin archivos de audio).
 *
 * Cada sonido es un tono corto con barrido de frecuencia y envolvente
 * exponencial, reproducido con AudioTrack en modo estático.
 */
class SoundManager {

    enum class Sound {
        CLICK, NAVIGATE, ENCODE_DONE, ERROR_DETECTED, CORRECTED, SUCCESS
    }

    var enabled: Boolean = true

    private val sampleRate = 22050
    private val tracks = mutableMapOf<Sound, AudioTrack>()

    fun play(sound: Sound) {
        if (!enabled) return
        try {
            val track = tracks.getOrPut(sound) { buildTrack(sound) }
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        } catch (_: Exception) {
            // El audio nunca debe romper la app.
        }
    }

    private fun buildTrack(sound: Sound): AudioTrack {
        val pcm = synthesize(sound)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    private fun synthesize(sound: Sound): ShortArray {
        // (frecuencia inicial, frecuencia final, duración ms, nº de tonos encadenados)
        val spec = when (sound) {
            Sound.CLICK -> listOf(Tone(950.0, 700.0, 55))
            Sound.NAVIGATE -> listOf(Tone(520.0, 880.0, 90))
            Sound.ENCODE_DONE -> listOf(Tone(660.0, 990.0, 150))
            Sound.ERROR_DETECTED -> listOf(Tone(420.0, 190.0, 200))
            Sound.CORRECTED -> listOf(Tone(500.0, 1180.0, 210))
            Sound.SUCCESS -> listOf(
                Tone(523.25, 523.25, 90),
                Tone(659.25, 659.25, 90),
                Tone(783.99, 1046.5, 160)
            )
        }
        val total = spec.sumOf { (it.ms * sampleRate / 1000) }
        val out = ShortArray(total)
        var pos = 0
        for (tone in spec) {
            val n = tone.ms * sampleRate / 1000
            for (i in 0 until n) {
                val t = i.toDouble() / n
                val freq = tone.from + (tone.to - tone.from) * t
                val phase = 2.0 * PI * freq * i / sampleRate
                // Envolvente: ataque rápido + decaimiento exponencial, volumen bajo.
                val envelope = exp(-3.2 * t) * (0.25 + 0.75 * (i.toDouble() / (n * 0.08 + 1)).coerceAtMost(1.0))
                val sample = sin(phase) * envelope * 0.22
                out[pos++] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    fun release() {
        for (t in tracks.values) {
            try {
                t.stop()
                t.release()
            } catch (_: Exception) {
            }
        }
        tracks.clear()
    }

    @VisibleForTesting
    data class Tone(val from: Double, val to: Double, val ms: Int)
}
