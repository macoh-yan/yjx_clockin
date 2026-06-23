package com.example.yjx_clockin.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private fun getEncryptionKey(): SecretKeySpec {
        val key = Constants.PREFS_NAME + "yjx_clockin_secret_key_2024"
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(key.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes.copyOf(16), ALGORITHM)
    }

    fun encrypt(text: String): String {
        try {
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, getEncryptionKey(), gcmSpec)
            
            val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return text
        }
    }

    fun decrypt(encryptedText: String): String {
        try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            
            val encryptedBytes = ByteArray(combined.size - iv.size)
            System.arraycopy(combined, iv.size, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getEncryptionKey(), gcmSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return encryptedText
        }
    }
}