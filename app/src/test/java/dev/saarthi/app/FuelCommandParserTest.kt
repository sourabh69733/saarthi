package dev.saarthi.app

import org.junit.Assert.*
import org.junit.Test

class FuelCommandParserTest {
    private val parser = FuelCommandParser()

    @Test fun parsesEnglishFuelCommand() {
        val result = parser.parse("Petrol 1500 rupees, 14 litres, meter 45200")
        assertEquals(FuelCommand(1500.0, 14.0, 45200), result)
    }

    @Test fun parsesHinglishFuelCommand() {
        val result = parser.parse("petrol dala 1500 ka 14 litre meter 45200")
        assertEquals(FuelCommand(1500.0, 14.0, 45200), result)
    }

    @Test fun parsesDevanagariDigits() {
        val result = parser.parse("पेट्रोल १५०० रुपये १४ लीटर मीटर ४५२००")
        assertEquals(FuelCommand(1500.0, 14.0, 45200), result)
    }

    @Test fun associatesNumbersWithTheirFieldsWhenOrderChanges() {
        val result = parser.parse("14 litres petrol for 1500 rupees meter 45200")
        assertEquals(FuelCommand(1500.0, 14.0, 45200), result)
    }

    @Test fun doesNotGuessMissingFields() {
        assertNull(parser.parse("petrol 1500 ka"))
    }

    @Test fun rejectsInvalidNumbers() {
        assertNull(parser.parse("petrol 1500 rupees 0 litre meter 45200"))
        assertNull(parser.parse("petrol 1500 rupees 14 litre meter -1"))
    }
}
