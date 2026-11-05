package com.statusbarmanager.app.data

import kotlinx.coroutines.flow.first

class SettingsBackupManager(private val repository: DataStoreRepository) {

    suspend fun getExistingBackup(): StatusBarBackupModel? {
        return repository.backupModelFlow.first()
    }

    suspend fun isHideActive(): Boolean {
        return repository.isHideActiveFlow.first()
    }

    suspend fun hasValidBackup(): Boolean {
        return repository.hasBackupFlow.first()
    }

    /**
     * Stores the original values.
     * CRITICAL: Will NOT overwrite an active backup if Hide mode is already enabled!
     */
    suspend fun saveBackupIfNotActive(
        timeSetting: String,
        networkSpeed: Boolean,
        icons: Map<String, Boolean>
    ): Boolean {
        val alreadyActive = isHideActive()
        if (alreadyActive) {
            // Already has an original baseline saved, protect it
            return false
        }

        val backup = StatusBarBackupModel(
            originalTimeSetting = timeSetting,
            originalRealTimeNetworkSpeed = networkSpeed,
            originalStatusIcons = icons,
            deviceManufacturer = android.os.Build.MANUFACTURER,
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.SDK_INT,
            isHideActive = true
        )
        repository.saveOriginalBackup(backup)
        return true
    }

    /**
     * Marks backup as completed restored and clears it.
     */
    suspend fun markRestoredSuccessfully() {
        repository.clearBackup()
    }

    /**
     * If restoration was partial, retain the backup and record remaining keys.
     */
    suspend fun recordPartialRestoration(unrestoredKeys: List<String>) {
        repository.setHideActive(false)
        repository.recordOperation(
            status = "PARTIAL",
            message = "Partially restored. Pending verification for: ${unrestoredKeys.joinToString(", ")}"
        )
    }
}
