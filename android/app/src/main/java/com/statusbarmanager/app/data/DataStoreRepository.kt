package com.statusbarmanager.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.statusbarmanager.app.automation.EngageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "status_bar_manager_prefs")

class DataStoreRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_HIDE_ACTIVE = booleanPreferencesKey("is_hide_active")
        val HAS_VALID_BACKUP = booleanPreferencesKey("has_valid_backup")
        val BACKUP_TIMESTAMP = longPreferencesKey("backup_timestamp")
        val ORIGINAL_TIME_SETTING = stringPreferencesKey("original_time_setting")
        val ORIGINAL_NETWORK_SPEED = booleanPreferencesKey("original_network_speed")
        val ORIGINAL_ICONS_SERIALIZED = stringPreferencesKey("original_icons_serialized")
        val LAST_OPERATION_STATUS = stringPreferencesKey("last_operation_status")
        val LAST_OPERATION_MESSAGE = stringPreferencesKey("last_operation_message")
        val LAST_OPERATION_TIMESTAMP = longPreferencesKey("last_operation_timestamp")
        val DEVICE_PROFILE = stringPreferencesKey("device_profile")
        val HAS_HEADPHONE_CONTROL = booleanPreferencesKey("has_headphone_control")
        val ORIGINAL_HEADPHONE_SETTING = booleanPreferencesKey("original_headphone_setting")
        val IS_PARTIAL_RESULT = booleanPreferencesKey("is_partial_result")
        
        // Objective 1 & 2: Engage Mode and Whitelist
        val ENGAGE_MODE = stringPreferencesKey("engage_mode")
        val ACTIVE_ENGAGEMENT_MODE = stringPreferencesKey("active_engagement_mode")
        val WHITELIST_PACKAGES = stringSetPreferencesKey("whitelist_packages")
        val LAST_FORCE_STOP_STATS = stringPreferencesKey("last_force_stop_stats")
    }

    val engageModeFlow: Flow<EngageMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val modeStr = preferences[PreferencesKeys.ENGAGE_MODE] ?: EngageMode.BOTH.name
            EngageMode.fromString(modeStr)
        }

    val activeEngagementModeFlow: Flow<EngageMode?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.ACTIVE_ENGAGEMENT_MODE]?.let(EngageMode::fromString)
        }

    val whitelistFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.WHITELIST_PACKAGES] ?: emptySet()
        }

    val isHideActiveFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_HIDE_ACTIVE] ?: false
        }

    val hasBackupFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.HAS_VALID_BACKUP] ?: false
        }

    val backupModelFlow: Flow<StatusBarBackupModel?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val hasBackup = preferences[PreferencesKeys.HAS_VALID_BACKUP] ?: false
            if (!hasBackup) return@map null

            val time = preferences[PreferencesKeys.ORIGINAL_TIME_SETTING] ?: StatusBarBackupModel.TIME_SHOW_HOURS_MINUTES
            val speed = preferences[PreferencesKeys.ORIGINAL_NETWORK_SPEED] ?: true
            val iconsSerialized = preferences[PreferencesKeys.ORIGINAL_ICONS_SERIALIZED] ?: ""
            val iconsMap = deserializeIcons(iconsSerialized)
            val timestamp = preferences[PreferencesKeys.BACKUP_TIMESTAMP] ?: System.currentTimeMillis()
            val isHideActive = preferences[PreferencesKeys.IS_HIDE_ACTIVE] ?: false
            val hasHeadphone = preferences[PreferencesKeys.HAS_HEADPHONE_CONTROL] ?: false
            val headphoneSetting = preferences[PreferencesKeys.ORIGINAL_HEADPHONE_SETTING]
            val isPartial = preferences[PreferencesKeys.IS_PARTIAL_RESULT] ?: false

            StatusBarBackupModel(
                timestamp = timestamp,
                originalTimeSetting = time,
                originalRealTimeNetworkSpeed = speed,
                originalStatusIcons = iconsMap,
                deviceManufacturer = android.os.Build.MANUFACTURER,
                deviceModel = android.os.Build.MODEL,
                androidVersion = android.os.Build.VERSION.SDK_INT,
                isHideActive = isHideActive,
                hasHeadphoneControl = hasHeadphone,
                originalHeadphoneSetting = headphoneSetting,
                isPartial = isPartial
            )
        }

    val lastOperationFlow: Flow<Triple<String, String, Long>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            Triple(
                preferences[PreferencesKeys.LAST_OPERATION_STATUS] ?: "IDLE",
                preferences[PreferencesKeys.LAST_OPERATION_MESSAGE] ?: "Ready to configure",
                preferences[PreferencesKeys.LAST_OPERATION_TIMESTAMP] ?: 0L
            )
        }

    suspend fun setEngageMode(mode: EngageMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENGAGE_MODE] = mode.name
        }
    }

    suspend fun getEngageMode(): EngageMode {
        val prefs = context.dataStore.data.first()
        val modeStr = prefs[PreferencesKeys.ENGAGE_MODE] ?: EngageMode.BOTH.name
        return EngageMode.fromString(modeStr)
    }

    suspend fun setActiveEngagementMode(mode: EngageMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_ENGAGEMENT_MODE] = mode.name
        }
    }

    suspend fun getActiveEngagementMode(): EngageMode? {
        val prefs = context.dataStore.data.first()
        val modeStr = prefs[PreferencesKeys.ACTIVE_ENGAGEMENT_MODE] ?: return null
        return EngageMode.fromString(modeStr)
    }

    suspend fun clearActiveEngagementMode() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.ACTIVE_ENGAGEMENT_MODE)
        }
    }

    suspend fun addToWhitelist(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.WHITELIST_PACKAGES] ?: emptySet()
            preferences[PreferencesKeys.WHITELIST_PACKAGES] = current + packageName
        }
    }

    suspend fun removeFromWhitelist(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.WHITELIST_PACKAGES] ?: emptySet()
            preferences[PreferencesKeys.WHITELIST_PACKAGES] = current - packageName
        }
    }

    suspend fun getWhitelist(): Set<String> {
        val prefs = context.dataStore.data.first()
        return prefs[PreferencesKeys.WHITELIST_PACKAGES] ?: emptySet()
    }

    suspend fun saveOriginalBackup(backup: StatusBarBackupModel) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_VALID_BACKUP] = true
            preferences[PreferencesKeys.BACKUP_TIMESTAMP] = backup.timestamp
            preferences[PreferencesKeys.ORIGINAL_TIME_SETTING] = backup.originalTimeSetting
            preferences[PreferencesKeys.ORIGINAL_NETWORK_SPEED] = backup.originalRealTimeNetworkSpeed
            preferences[PreferencesKeys.ORIGINAL_ICONS_SERIALIZED] = serializeIcons(backup.originalStatusIcons)
            preferences[PreferencesKeys.IS_HIDE_ACTIVE] = true
            preferences[PreferencesKeys.DEVICE_PROFILE] = "${backup.deviceManufacturer} ${backup.deviceModel} (API ${backup.androidVersion})"
            preferences[PreferencesKeys.HAS_HEADPHONE_CONTROL] = backup.hasHeadphoneControl
            if (backup.originalHeadphoneSetting != null) {
                preferences[PreferencesKeys.ORIGINAL_HEADPHONE_SETTING] = backup.originalHeadphoneSetting
            }
            preferences[PreferencesKeys.IS_PARTIAL_RESULT] = backup.isPartial
        }
    }

    suspend fun setHideActive(active: Boolean, isPartial: Boolean = false) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_HIDE_ACTIVE] = active
            preferences[PreferencesKeys.IS_PARTIAL_RESULT] = isPartial
        }
    }

    suspend fun clearBackup() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_VALID_BACKUP] = false
            preferences[PreferencesKeys.IS_HIDE_ACTIVE] = false
            preferences[PreferencesKeys.IS_PARTIAL_RESULT] = false
            preferences.remove(PreferencesKeys.ORIGINAL_TIME_SETTING)
            preferences.remove(PreferencesKeys.ORIGINAL_NETWORK_SPEED)
            preferences.remove(PreferencesKeys.ORIGINAL_ICONS_SERIALIZED)
            preferences.remove(PreferencesKeys.BACKUP_TIMESTAMP)
            preferences.remove(PreferencesKeys.HAS_HEADPHONE_CONTROL)
            preferences.remove(PreferencesKeys.ORIGINAL_HEADPHONE_SETTING)
        }
    }

    suspend fun recordOperation(status: String, message: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_OPERATION_STATUS] = status
            preferences[PreferencesKeys.LAST_OPERATION_MESSAGE] = message
            preferences[PreferencesKeys.LAST_OPERATION_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    private fun serializeIcons(map: Map<String, Boolean>): String {
        return map.entries.joinToString(";") { "${it.key}=${it.value}" }
    }

    private fun deserializeIcons(raw: String): Map<String, Boolean> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                parts[0] to parts[1].toBoolean()
            } else null
        }.toMap()
    }
}
