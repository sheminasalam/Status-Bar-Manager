package com.statusbarmanager.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.statusbarmanager.app.automation.EngageMode

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
}
