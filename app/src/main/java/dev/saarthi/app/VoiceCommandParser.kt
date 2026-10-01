package dev.saarthi.app

class VoiceCommandParser {
    fun parse(raw: String): VoiceCommand {
        val spoken = raw.trim().replace(Regex("\\s+"), " ")
        if (spoken.isEmpty() || NEGATION.containsMatchIn(spoken)) return VoiceCommand.Unsupported

        if (OPEN_MAPS.matches(spoken) || MAPS_KHOLO.matches(spoken)) return VoiceCommand.OpenMaps
        NAVIGATE_TO.matchEntire(spoken)?.groupValues?.get(1)?.trim()?.takeIf(::hasWords)
            ?.let { return VoiceCommand.Navigate(it) }
        PLAY.matchEntire(spoken)?.groupValues?.get(1)?.trim()?.takeIf(::hasWords)
            ?.let { return VoiceCommand.PlayMusic(it) }
        CHALO.matchEntire(spoken)?.groupValues?.get(1)?.trim()?.takeIf(::hasWords)
            ?.let { return VoiceCommand.Navigate(it) }
        BAJAO.matchEntire(spoken)?.groupValues?.get(1)?.trim()?.takeIf(::hasWords)
            ?.let { return VoiceCommand.PlayMusic(it) }
        return VoiceCommand.Unsupported
    }

    private fun hasWords(value: String): Boolean = value.any(Char::isLetterOrDigit)

    private companion object {
        val NEGATION = Regex("(?i)\\b(?:don['’]t|do not|not|mat|nahi|nahin)\\b")
        val OPEN_MAPS = Regex("(?i)open (?:google )?maps[.!?]?")
        val MAPS_KHOLO = Regex("(?i)(?:maps|google maps|google max) kholo[.!?]?")
        val NAVIGATE_TO = Regex("(?i)navigate to (.+)")
        val CHALO = Regex("(?i)(.+) chalo[.!?]?")
        val PLAY = Regex("(?i)play (.+)")
        val BAJAO = Regex("(?i)(.+) (?:bajao|bajau)[.!?]?")
    }
}
