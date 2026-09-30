package dev.saarthi.app

data class FuelCommand(val amountRupees: Double, val litres: Double, val odometerKm: Int)

class FuelCommandParser {
    private val number = Regex("\\d+(?:[.,]\\d+)?")
    private val fuel = Regex("petrol|diesel|fuel|पेट्रोल|डीजल|ईंधन", RegexOption.IGNORE_CASE)
    private val litre = Regex("lit(?:re|er)s?|लीटर", RegexOption.IGNORE_CASE)
    private val meter = Regex("meter|metre|odo(?:meter)?|मीटर", RegexOption.IGNORE_CASE)

    fun parse(raw: String): FuelCommand? {
        val text = buildString {
            for (char in raw.lowercase()) {
                val digit = Character.digit(char, 10)
                append(if (digit >= 0) ('0' + digit) else char)
            }
        }
        if (!fuel.containsMatchIn(text) || !litre.containsMatchIn(text) || !meter.containsMatchIn(text)) return null
        if (Regex("-\\s*\\d").containsMatchIn(text)) return null

        val numbers = number.findAll(text).toList()
        if (numbers.size != 3) return null
        val litreStart = litre.find(text)?.range?.first ?: return null
        val meterEnd = meter.find(text)?.range?.last ?: return null
        val volumeMatch = numbers.lastOrNull {
            it.range.last < litreStart && text.substring(it.range.last + 1, litreStart).isBlank()
        } ?: return null
        val odoMatch = numbers.firstOrNull {
            it.range.first > meterEnd && text.substring(meterEnd + 1, it.range.first).isBlank()
        } ?: return null
        val amountMatch = numbers.singleOrNull { it != volumeMatch && it != odoMatch } ?: return null
        val amount = amountMatch.value.replace(',', '.').toDoubleOrNull() ?: return null
        val volume = volumeMatch.value.replace(',', '.').toDoubleOrNull() ?: return null
        val odo = odoMatch.value.replace(',', '.').toDoubleOrNull() ?: return null
        if (amount <= 0 || amount > 100_000 || volume <= 0 || volume > 1_000) return null
        if (odo < 0 || odo > 9_999_999 || odo != odo.toInt().toDouble()) return null
        return FuelCommand(amount, volume, odo.toInt())
    }
}
