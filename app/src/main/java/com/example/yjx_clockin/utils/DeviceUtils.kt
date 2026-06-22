package com.example.yjx_clockin.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

/**
 * 设备相关工具类
 */
object DeviceUtils {

    private const val UNKNOWN_DEVICE = "unknown_device"

    /**
     * 获取 Android 设备唯一 ID
     * 使用 Settings.Secure.ANDROID_ID 作为设备标识
     */
    @SuppressLint("HardwareIds")
    fun getAndroidDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: UNKNOWN_DEVICE
        } catch (e: Exception) {
            UNKNOWN_DEVICE
        }
    }

    /**
     * 检查设备 ID 是否有效
     */
    fun isDeviceIdValid(deviceId: String): Boolean {
        return deviceId.isNotEmpty() && deviceId != UNKNOWN_DEVICE
    }
}
