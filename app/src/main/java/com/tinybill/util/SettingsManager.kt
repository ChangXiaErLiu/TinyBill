package com.tinybill.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val USE_SYSTEM_THEME_KEY = booleanPreferencesKey("use_system_theme")
        private val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
        private val ACCESSIBILITY_GUIDED_KEY = booleanPreferencesKey("accessibility_guided")

        // 敏感设置，使用 EncryptedSharedPreferences 存储
        private const val ENCRYPTED_PREFS_NAME = "encrypted_settings"
        private const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"
        private const val MONTHLY_BUDGET_KEY = "monthly_budget"
        private const val LAST_BACKUP_TIME_KEY = "last_backup_time"
        private const val CATEGORY_BUDGETS_KEY = "category_budgets"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val encryptedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    val darkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }
    
    val useSystemTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_SYSTEM_THEME_KEY] ?: true
    }
    
    val biometricEnabled: Flow<Boolean> = kotlinx.coroutines.flow.flow {
        emit(encryptedPreferences.getBoolean(BIOMETRIC_ENABLED_KEY, false))
    }
    
    val monthlyBudget: Flow<Double> = kotlinx.coroutines.flow.flow {
        emit(encryptedPreferences.getFloat(MONTHLY_BUDGET_KEY, 200.0f).toDouble())
    }
    
    val lastBackupTime: Flow<String> = kotlinx.coroutines.flow.flow {
        emit(encryptedPreferences.getString(LAST_BACKUP_TIME_KEY, "") ?: "")
    }
    
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    suspend fun setUseSystemTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_SYSTEM_THEME_KEY] = enabled
        }
    }
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        encryptedPreferences.edit().putBoolean(BIOMETRIC_ENABLED_KEY, enabled).apply()
    }
    
    suspend fun setMonthlyBudget(budget: Double) {
        encryptedPreferences.edit().putFloat(MONTHLY_BUDGET_KEY, budget.toFloat()).apply()
    }
    
    suspend fun setLastBackupTime(time: String) {
        encryptedPreferences.edit().putString(LAST_BACKUP_TIME_KEY, time).apply()
    }
    
    suspend fun setCategoryBudgets(budgetsJson: String) {
        encryptedPreferences.edit().putString(CATEGORY_BUDGETS_KEY, budgetsJson).apply()
    }
    
    val categoryBudgets: Flow<String> = kotlinx.coroutines.flow.flow {
        emit(encryptedPreferences.getString(CATEGORY_BUDGETS_KEY, "{}") ?: "{}")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH_KEY] ?: true
    }

    val isAccessibilityGuided: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ACCESSIBILITY_GUIDED_KEY] ?: false
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = isFirst
        }
    }

    suspend fun setAccessibilityGuided(isGuided: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ACCESSIBILITY_GUIDED_KEY] = isGuided
        }
    }
}
