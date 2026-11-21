package com.taskmgr.scheduler

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class VibrationPattern(
    val displayName: String,
    val timings: LongArray,
    val amplitudes: IntArray,
    val repeatIndex: Int
) {
    STANDARD(
        displayName = "Padrão",
        timings = longArrayOf(0, 350, 250, 350),
        amplitudes = intArrayOf(0, 200, 0, 200),
        repeatIndex = -1
    ),
    PULSE(
        displayName = "Pulso Repetido",
        timings = longArrayOf(0, 250, 200, 250, 200, 400, 600),
        amplitudes = intArrayOf(0, 220, 0, 220, 0, 255, 0),
        repeatIndex = 0
    ),
    URGENT(
        displayName = "Intenso",
        timings = longArrayOf(0, 500, 150, 500, 150, 700, 400),
        amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0),
        repeatIndex = 0
    )
}

class VibrationController(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getConfiguredPattern(): VibrationPattern {
        val raw = prefs.getString(KEY_PATTERN, VibrationPattern.PULSE.name)
        return runCatching { VibrationPattern.valueOf(raw ?: VibrationPattern.PULSE.name) }
            .getOrDefault(VibrationPattern.PULSE)
    }

    fun setConfiguredPattern(pattern: VibrationPattern) {
        prefs.edit().putString(KEY_PATTERN, pattern.name).apply()
    }

    fun hasCompletedOnboarding(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun vibrateReminder(pattern: VibrationPattern = getConfiguredPattern()) {
        val vibrator = getVibrator() ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val effect = if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, pattern.repeatIndex)
            } else {
                VibrationEffect.createWaveform(pattern.timings, pattern.repeatIndex)
            }
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vibrator.vibrate(effect, attrs)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, pattern.repeatIndex)
            } else {
                VibrationEffect.createWaveform(pattern.timings, pattern.repeatIndex)
            }
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, audioAttrs)
        } else {
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern.timings, pattern.repeatIndex, audioAttrs)
        }
    }

    fun cancel() {
        getVibrator()?.cancel()
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    companion object {
        private const val PREFS_NAME = "taskmgr_settings"
        private const val KEY_PATTERN = "vibration_pattern"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
