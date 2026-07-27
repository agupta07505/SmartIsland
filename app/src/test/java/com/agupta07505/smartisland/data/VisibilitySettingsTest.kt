package com.agupta07505.smartisland.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VisibilitySettingsTest {
    @Test
    fun visibilityMode_parsesStoredValues() {
        assertEquals(IslandVisibilityMode.AlwaysVisible, IslandVisibilityMode.fromStorageValue("AlwaysVisible"))
        assertEquals(IslandVisibilityMode.ShowOnlyWhenActive, IslandVisibilityMode.fromStorageValue("ShowOnlyWhenActive"))
        assertEquals(IslandVisibilityMode.AlwaysVisible, IslandVisibilityMode.fromStorageValue("unknown"))
    }

    @Test
    fun autoHideDuration_mapsMillisToSupportedOptions() {
        assertEquals(AutoHideDuration.Never, AutoHideDuration.fromMillis(-1L))
        assertEquals(AutoHideDuration.Seconds2, AutoHideDuration.fromMillis(2000L))
        assertEquals(AutoHideDuration.Seconds30, AutoHideDuration.fromMillis(30000L))
        assertEquals(AutoHideDuration.Never, AutoHideDuration.fromMillis(0L))
    }
}
