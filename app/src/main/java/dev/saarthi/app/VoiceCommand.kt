package dev.saarthi.app

sealed interface VoiceCommand {
    data object OpenMaps : VoiceCommand
    data class Navigate(val destination: String) : VoiceCommand
    data class PlayMusic(val query: String) : VoiceCommand
    data object Unsupported : VoiceCommand
}
