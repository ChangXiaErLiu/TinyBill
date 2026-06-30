package com.tinybill.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

object SecurityManager {
    
    private const val PREFS_NAME = "tinybill_security"
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_LOCK_TYPE = "lock_type"
    private const val KEY_PASSWORD_HASH = "password_hash"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    
    private const val LOCK_TYPE_NONE = 0
    private const val LOCK_TYPE_PASSWORD = 1
    private const val LOCK_TYPE_BIOMETRIC = 2
    
    private var prefs: SharedPreferences? = null
    
    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun isAppLockEnabled(): Boolean {
        return prefs?.getBoolean(KEY_APP_LOCK_ENABLED, false) == true
    }
    
    fun setAppLockEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_APP_LOCK_ENABLED, enabled)?.apply()
    }
    
    fun getLockType(): Int {
        return prefs?.getInt(KEY_LOCK_TYPE, LOCK_TYPE_NONE) ?: LOCK_TYPE_NONE
    }
    
    fun setLockType(type: Int) {
        prefs?.edit()?.putInt(KEY_LOCK_TYPE, type)?.apply()
    }
    
    fun setPassword(password: String) {
        val hash = hashPassword(password)
        prefs?.edit()
            ?.putString(KEY_PASSWORD_HASH, hash)
            ?.putInt(KEY_LOCK_TYPE, LOCK_TYPE_PASSWORD)
            ?.putBoolean(KEY_APP_LOCK_ENABLED, true)
            ?.apply()
    }
    
    fun verifyPassword(password: String): Boolean {
        val storedHash = prefs?.getString(KEY_PASSWORD_HASH, null) ?: return false
        val inputHash = hashPassword(password)
        return storedHash == inputHash
    }
    
    fun isBiometricEnabled(): Boolean {
        return prefs?.getBoolean(KEY_BIOMETRIC_ENABLED, false) == true
    }
    
    fun setBiometricEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_BIOMETRIC_ENABLED, enabled)?.apply()
    }
    
    fun clearLock() {
        prefs?.edit()
            ?.remove(KEY_PASSWORD_HASH)
            ?.remove(KEY_BIOMETRIC_ENABLED)
            ?.putInt(KEY_LOCK_TYPE, LOCK_TYPE_NONE)
            ?.putBoolean(KEY_APP_LOCK_ENABLED, false)
            ?.apply()
    }
    
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

object DatabaseCrypto {
    
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    
    fun generateKey(): ByteArray {
        val keyGen = javax.crypto.KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE)
        return keyGen.generateKey().encoded
    }
    
    fun encrypt(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = javax.crypto.Cipher.getInstance(ALGORITHM)
        val secretKey = javax.crypto.spec.SecretKeySpec(key, "AES")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }
    
    fun decrypt(encryptedData: ByteArray, key: ByteArray): ByteArray {
        val cipher = javax.crypto.Cipher.getInstance(ALGORITHM)
        val secretKey = javax.crypto.spec.SecretKeySpec(key, "AES")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(encryptedData)
    }
}