package dev.saarthi.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalActionTest {
    @Test fun openMapsTargetsGoogleMaps() {
        assertEquals(
            ExternalAction("android.intent.action.VIEW", "geo:0,0", "com.google.android.apps.maps"),
            ExternalAction.forCommand(VoiceCommand.OpenMaps),
        )
    }

    @Test fun navigationEncodesDestinationWithoutLosingPunctuation() {
        assertEquals(
            ExternalAction("android.intent.action.VIEW", "google.navigation:q=Connaught+Place", "com.google.android.apps.maps"),
            ExternalAction.forCommand(VoiceCommand.Navigate("Connaught Place")),
        )
        assertEquals(
            "google.navigation:q=St.+John%27s+Road%2C+Bengaluru",
            ExternalAction.forCommand(VoiceCommand.Navigate("St. John's Road, Bengaluru"))?.uri,
        )
    }

    @Test fun musicUsesUnstructuredMediaSearchWithoutVendor() {
        assertEquals(
            ExternalAction(
                "android.media.action.MEDIA_PLAY_FROM_SEARCH",
                extras = mapOf(
                    "android.intent.extra.focus" to "vnd.android.cursor.item/*",
                    "query" to "Arijit Singh",
                ),
            ),
            ExternalAction.forCommand(VoiceCommand.PlayMusic("Arijit Singh")),
        )
    }

    @Test fun unsupportedCommandHasNoAction() {
        assertNull(ExternalAction.forCommand(VoiceCommand.Unsupported))
    }
}
