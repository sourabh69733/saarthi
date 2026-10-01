package dev.saarthi.app

data class VoiceInterpretation(val transcript: String, val command: VoiceCommand)

class VoiceCommandWorkflow(
    private val transcribe: suspend (FloatArray) -> String,
    private val parser: VoiceCommandParser,
) {
    suspend fun interpret(samples: FloatArray): VoiceInterpretation {
        if (samples.isEmpty()) return VoiceInterpretation("", VoiceCommand.Unsupported)
        val transcript = transcribe(samples).trim()
        return VoiceInterpretation(transcript, parser.parse(transcript))
    }
}
