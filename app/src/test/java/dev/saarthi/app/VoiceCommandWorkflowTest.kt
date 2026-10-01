package dev.saarthi.app

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandWorkflowTest {
    @Test fun transcribedCommandIsParsed() = runBlocking {
        val workflow = VoiceCommandWorkflow({ "navigate to Connaught Place" }, VoiceCommandParser())
        assertEquals(
            VoiceInterpretation("navigate to Connaught Place", VoiceCommand.Navigate("Connaught Place")),
            workflow.interpret(floatArrayOf(0.1f)),
        )
    }

    @Test fun emptyAudioDoesNotTranscribe() = runBlocking {
        var calls = 0
        val workflow = VoiceCommandWorkflow({ calls++; "play music" }, VoiceCommandParser())
        assertEquals(VoiceInterpretation("", VoiceCommand.Unsupported), workflow.interpret(floatArrayOf()))
        assertEquals(0, calls)
    }

    @Test fun unsupportedSpeechCannotCreateAnExternalAction() = runBlocking {
        val workflow = VoiceCommandWorkflow({ "what time is it" }, VoiceCommandParser())
        val result = workflow.interpret(floatArrayOf(0.1f))
        assertEquals(VoiceCommand.Unsupported, result.command)
        assertNull(ExternalAction.forCommand(result.command))
    }
}
