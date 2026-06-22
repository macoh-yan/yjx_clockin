package com.example.yjx_clockin.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import com.example.yjx_clockin.R

/**
 * 图片处理工具类
 */
object ImageUtils {

    /**
     * 将 Base64 字符串解码为 Bitmap
     * @param base64Str Base64 字符串（可能包含 data:image/...;base64, 前缀）
     * @return 解码后的 Bitmap，失败返回 null
     */
    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            var pureBase64 = base64Str
            if (base64Str.contains("base64,")) {
                pureBase64 = base64Str.substring(base64Str.indexOf("base64,") + 7)
            }
            val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将 Base64 字符串解码并设置到 ImageView
     * @param base64Str Base64 字符串
     * @param imageView 目标 ImageView
     * @param defaultRes 默认图片资源 ID
     */
    fun setAvatarFromBase64(base64Str: String, imageView: ImageView, defaultRes: Int = R.drawable.name_image) {
        val bitmap = base64ToBitmap(base64Str)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.setImageResource(defaultRes)
        }
    }

    /**
     * 将 Bitmap 转换为 Base64 字符串
     * @param bitmap 源图片
     * @param quality 压缩质量 (0-100)
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return Base64 字符串
     */
    fun bitmapToBase64(
        bitmap: Bitmap,
        quality: Int = 70,
        maxWidth: Int = 480,
        maxHeight: Int = 640
    ): String {
        val scaled = scaleBitmap(bitmap, maxWidth, maxHeight)
        val baos = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * 等比缩放 Bitmap
     */
    fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
