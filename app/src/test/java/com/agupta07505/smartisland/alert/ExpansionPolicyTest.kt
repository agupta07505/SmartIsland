package com.agupta07505.smartisland.alert

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpansionPolicyTest {

    @Test
    fun landscapeNeverOpensTheMediumCardButStillOpensTheIdleDashboard() {
        assertFalse(ExpansionPolicy.canExpand(hasAlerts = true, isLandscape = true))
        assertTrue(ExpansionPolicy.canExpand(hasAlerts = false, isLandscape = true))
        assertTrue(ExpansionPolicy.canExpand(hasAlerts = true, isLandscape = false))
    }

    @Test
    fun landscapeBlocksEveryAutoExpand() {
        AlertTier.entries.forEach { tier ->
            assertFalse(tier.name, ExpansionPolicy.allowAutoExpand(tier, primary = null, isLandscape = true))
        }
    }

    @Test
    fun aSecureForegroundWindowKeepsEphemeralAlertsInTheBubble() {
        assertFalse(ExpansionPolicy.allowAutoExpand(AlertTier.Ephemeral, primary = null, foregroundIsSecure = true))
        assertTrue(ExpansionPolicy.allowAutoExpand(AlertTier.Ephemeral, primary = null, foregroundIsSecure = false))
        // Only Ephemeral alerts are held back; a call still opens.
        assertTrue(ExpansionPolicy.allowAutoExpand(AlertTier.Persistent, primary = null, foregroundIsSecure = true))
    }

    @Test
    fun anEphemeralAlertWaitsBehindACriticalOrPersistentPill() {
        assertFalse(ExpansionPolicy.allowAutoExpand(AlertTier.Ephemeral, primary = AlertTier.Critical))
        assertFalse(ExpansionPolicy.allowAutoExpand(AlertTier.Ephemeral, primary = AlertTier.Persistent))
        assertTrue(ExpansionPolicy.allowAutoExpand(AlertTier.Ephemeral, primary = AlertTier.Background))
        assertFalse(ExpansionPolicy.allowAutoExpand(AlertTier.Passive, primary = null))
    }
}
