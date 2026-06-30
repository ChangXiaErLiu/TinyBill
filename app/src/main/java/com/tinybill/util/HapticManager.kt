package com.tinybill.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.selection.selectable

object HapticManager {

    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun safeVibrate(action: () -> Unit) {
        try {
            action()
        } catch (e: SecurityException) {
            // 权限被拒绝，静默处理
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun performClick() {
        safeVibrate {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(50)
                }
            }
        }
    }

    fun performHeavyClick() {
        safeVibrate {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(100)
                }
            }
        }
    }

    fun performSuccess() {
        safeVibrate {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(30)
                }
            }
        }
    }

    fun performError() {
        safeVibrate {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 50, 50, 50), -1)
                }
            }
        }
    }

    fun performDelete() {
        safeVibrate {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 20, 30), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 30, 20, 30), -1)
                }
            }
        }
    }
}

fun View.performHapticFeedback() {
    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}