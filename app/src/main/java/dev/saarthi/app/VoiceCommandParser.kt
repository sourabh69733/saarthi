package dev.saarthi.app

class VoiceCommandParser {
    fun parse(raw: String): VoiceCommand {
        val spoken = raw.trim().replace(Regex("\\s+"), " ")
        if (spoken.isEmpty() || NEGATION.containsMatchIn(spoken)) return VoiceCommand.Unsupported

        if (OPEN_MAPS.matches(spoken) || MAPS_KHOLO.matches(spoken)) return VoiceCommand.OpenMaps
        NAVIGATE_TO.matchEntire(spoken)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return VoiceCommand.Navigate(it) }
        CHALO.matchEntire(spoken)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return VoiceCommand.Navigate(it) }
        PLAY.matchEntire(spoken)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return VoiceCommand.PlayMusic(it) }
        BAJAO.matchEntire(spoken)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return VoiceCommand.PlayMusic(it) }
        return VoiceCommand.Unsupported
    }

    private companion object {
        val NEGATION = Regex("(?i)\\b(?:don't|do not|not|mat|nahi|nahin)\\b")
        val OPEN_MAPS = Regex("(?i)open (?:google )?maps[.!?]?")
        val MAPS_KHOLO = Regex("(?i)(?:google )?maps kholo[.!?]?")
        val NAVIGATE_TO = Regex("(?i)navigate to (.+)")
        val CHALO = Regex("(?i)(.+) chalo[.!?]?")
        val PLAY = Regex("(?i)play (.+)")
        val BAJAO = Regex("(?i)(.+) bajao[.!?]?")
    }
}
