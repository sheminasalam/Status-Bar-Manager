package com.statusbarmanager.app

import android.app.ActivityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.statusbarmanager.app.automation.EngageMode
import com.statusbarmanager.app.automation.isActiveProcessImportance
import com.statusbarmanager.app.automation.DeviceCompatibilityManager
import com.statusbarmanager.app.automation.TargetMode
import com.statusbarmanager.app.automation.shouldPersistStatusBarHidden

class SysHushUnitTest {

    @Test
    fun testEngageModeEnumParsing() {
        assertEquals(EngageMode.STATUS_BAR_ONLY, EngageMode.fromString("STATUS_BAR_ONLY"))
        assertEquals(EngageMode.FORCE_STOP_APPS_ONLY, EngageMode.fromString("FORCE_STOP_APPS_ONLY"))
        assertEquals(EngageMode.BOTH, EngageMode.fromString("BOTH"))
        // Fallback for invalid input
        assertEquals(EngageMode.BOTH, EngageMode.fromString("INVALID_MODE"))
    }

    @Test
    fun testDefaultChecklistForMode() {
        val statusBarChecklist = com.statusbarmanager.app.automation.AutomationProgress.forMode(
            EngageMode.STATUS_BAR_ONLY,
            isEngage = true
        )
        assertTrue(statusBarChecklist.isNotEmpty())
        assertTrue(statusBarChecklist.any { it.id == "time" })

        val forceStopChecklist = com.statusbarmanager.app.automation.AutomationProgress.forMode(
            EngageMode.FORCE_STOP_APPS_ONLY,
            isEngage = true
        )
        assertTrue(forceStopChecklist.isNotEmpty())
        assertTrue(forceStopChecklist.any { it.id == "force_stop" })
    }
    @Test
    fun shortStatusBarLabelsMatchWithoutMatchingUnrelatedWords() {
        val manager = DeviceCompatibilityManager()
        assertTrue(manager.matchesSettingsLabel("Show time in status bar", "Time"))
        assertTrue(manager.matchesSettingsLabel("Time display", "Time"))
        assertFalse(manager.matchesSettingsLabel("Screen timeout", "Time"))
    }

    @Test
    fun appOnlyForceStopDoesNotPersistStatusBarAsHidden() {
        assertFalse(shouldPersistStatusBarHidden(TargetMode.ForceStopOnly))
        assertTrue(shouldPersistStatusBarHidden(TargetMode.HideAll))
        assertTrue(shouldPersistStatusBarHidden(TargetMode.BothEngage))
        assertFalse(shouldPersistStatusBarHidden(TargetMode.RestoreOriginal))
        assertFalse(shouldPersistStatusBarHidden(TargetMode.BothDisengage))
    }

    @Test
    fun testOnlyNonCachedProcessesAreEligibleForForceStop() {
        assertTrue(
            isActiveProcessImportance(ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
        )
        assertTrue(
            isActiveProcessImportance(ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE)
        )
        assertFalse(
            isActiveProcessImportance(ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED)
        )
        assertFalse(
            isActiveProcessImportance(ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE)
        )
    }

}
