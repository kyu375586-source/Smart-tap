package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundAndHaptics {
    var isSoundEnabled: Boolean = true
    var isVibrationEnabled: Boolean = true

    private var tapPcmShort: ShortArray? = null
    private var sampleRate = 44100

    init {
        generateTapWaveform()
    }

    private fun generateTapWaveform() {
        val durationMs = 35
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val generated = ShortArray(numSamples)
        val freqStart = 900.0
        val freqEnd = 300.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val currentFreq = freqStart + (freqEnd - freqStart) * progress
            val angle = 2.0 * Math.PI * i / (sampleRate / currentFreq)
            val envelope = 1.0 - progress // Linear fade out
            val sample = (sin(angle) * envelope * Short.MAX_VALUE * 0.75).toInt()
            generated[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        tapPcmShort = generated
    }

    fun playTapSound(scope: CoroutineScope) {
        if (!isSoundEnabled) return
        val buffer = tapPcmShort ?: return
        scope.launch(Dispatchers.Default) {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                track.notificationMarkerPosition = buffer.size
                track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(trackRef: AudioTrack?) {
                        trackRef?.release()
                    }
                    override fun onPeriodicNotification(trackRef: AudioTrack?) {}
                })
            } catch (_: Exception) {
                // Ignore audio playback failure gracefully
            }
        }
    }

    fun vibrate(context: Context, durationMs: Long = 20) {
        if (!isVibrationEnabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore vibration failure
        }
    }
}
