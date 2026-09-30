package dev.saarthi.app

object ManualFuelEntry {
    fun parse(amount: String, litres: String, odometer: String): FuelCommand? {
        val rupees = amount.trim().toDoubleOrNull() ?: return null
        val volume = litres.trim().toDoubleOrNull() ?: return null
        val distance = odometer.trim().toIntOrNull() ?: return null
        if (!rupees.isFinite() || rupees <= 0 || rupees > 100_000) return null
        if (!volume.isFinite() || volume <= 0 || volume > 1_000) return null
        if (distance < 0 || distance > 9_999_999) return null
        return FuelCommand(rupees, volume, distance)
    }
}
