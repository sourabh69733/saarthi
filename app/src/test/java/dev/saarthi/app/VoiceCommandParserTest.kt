package dev.saarthi.app

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandParserTest {
    private val parser = VoiceCommandParser()

    @Test fun opensMapsInEnglishOrHinglish() {
        assertEquals(VoiceCommand.OpenMaps, parser.parse("open Google Maps"))
        assertEquals(VoiceCommand.OpenMaps, parser.parse("Maps kholo"))
    }

    @Test fun extractsNavigationDestination() {
        assertEquals(VoiceCommand.Navigate("Connaught Place"), parser.parse("navigate to Connaught Place"))
        assertEquals(VoiceCommand.Navigate("CP"), parser.parse("CP chalo"))
        assertEquals(VoiceCommand.Navigate("St. John's Road, Bengaluru"),
            parser.parse("  navigate   to   St. John's Road,   Bengaluru  "))
    }

    @Test fun extractsMusicQuery() {
        assertEquals(VoiceCommand.PlayMusic("Arijit Singh"), parser.parse("play Arijit Singh"))
        assertEquals(VoiceCommand.PlayMusic("Arijit Singh"), parser.parse("Arijit Singh bajao"))
        assertEquals(VoiceCommand.PlayMusic("A.R. Rahman, 90s"),
            parser.parse(" play   A.R. Rahman,   90s "))
    }

    @Test fun rejectsIncompleteNegatedAndUnrelatedSpeech() {
        for (spoken in listOf("", "  ", "play", "navigate to", "don't play music", "do not play music", "what time is it")) {
            assertEquals("Unexpected action for '$spoken'", VoiceCommand.Unsupported, parser.parse(spoken))
        }
    }

    @Test fun typographicApostropheNegationCannotLaunchAnAction() {
        assertEquals(VoiceCommand.Unsupported, parser.parse("Play music. Don’t play it."))
        assertEquals(VoiceCommand.Unsupported, parser.parse("don’t CP chalo"))
    }

    @Test fun explicitMusicPrefixWinsOverNavigationWordInTheQuery() {
        assertEquals(VoiceCommand.PlayMusic("Chalo"), parser.parse("play Chalo"))
    }

    @Test fun punctuationOnlyIsNotAPlaceOrMusicQuery() {
        for (spoken in listOf("navigate to ...", "play ...", "... chalo", "... bajao")) {
            assertEquals(VoiceCommand.Unsupported, parser.parse(spoken))
        }
    }

    @Test fun acceptsObservedMapsTranscriptionVariant() {
        assertEquals(VoiceCommand.OpenMaps, parser.parse("Google max kholo"))
    }

    @Test fun acceptsObservedMusicTranscriptionVariant() {
        assertEquals(VoiceCommand.PlayMusic("Arigit Singh"), parser.parse("Arigit Singh Bajau"))
    }
}
