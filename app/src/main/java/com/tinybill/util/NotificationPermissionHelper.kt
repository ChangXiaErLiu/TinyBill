package com.tinybill.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * 通知权限请求帮助类。
 *
 * Android 13（API 33）起，POST_NOTIFICATIONS 需要在运行时请求。
 * 如果用户拒绝，后续不再弹窗，静默处理（App 核心功能不依赖通知）。
 */
object NotificationPermissionHelper {

    /**
     * 检查是否需要请求通知权限（仅 API 33+）。
     */
    fun shouldRequestPermission(activity: ComponentActivity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    }

    /**
     * 在 Activity 中注册权限请求 launcher。
     * 在 Activity.onCreate 中调用。
     */
    fun registerPermissionLauncher(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ ->
            // 用户同意或拒绝，核心功能无需通知权限，不做额外处理
        }

        if (shouldRequestPermission(activity)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
