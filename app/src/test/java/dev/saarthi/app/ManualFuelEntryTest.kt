package dev.saarthi.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualFuelEntryTest {
    @Test fun acceptsValidFields() {
        assertEquals(FuelCommand(1500.0, 14.5, 45200), ManualFuelEntry.parse("1500", "14.5", "45200"))
    }

    @Test fun rejectsIncompleteOrInvalidFields() {
        assertNull(ManualFuelEntry.parse("", "14", "45200"))
        assertNull(ManualFuelEntry.parse("1500", "0", "45200"))
        assertNull(ManualFuelEntry.parse("1500", "14", "45.2"))
        assertNull(ManualFuelEntry.parse("1500", "14", "-1"))
    }
}
