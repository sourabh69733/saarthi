package dev.saarthi.app

import android.app.SearchManager
import android.content.Intent
import android.provider.MediaStore
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ExternalAction(
    val action: String,
    val uri: String? = null,
    val packageName: String? = null,
    val extras: Map<String, String> = emptyMap(),
) {
    companion object {
        fun forCommand(command: VoiceCommand): ExternalAction? = when (command) {
            VoiceCommand.OpenMaps -> ExternalAction(
                Intent.ACTION_VIEW, "geo:0,0", "com.google.android.apps.maps",
            )
            is VoiceCommand.Navigate -> ExternalAction(
                Intent.ACTION_VIEW,
                "google.navigation:q=${URLEncoder.encode(command.destination, StandardCharsets.UTF_8.name())}",
                "com.google.android.apps.maps",
            )
            is VoiceCommand.PlayMusic -> ExternalAction(
                MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH,
                extras = mapOf(
                    MediaStore.EXTRA_MEDIA_FOCUS to "vnd.android.cursor.item/*",
                    SearchManager.QUERY to command.query,
                ),
            )
            VoiceCommand.Unsupported -> null
        }
    }
}
