package dev.saarthi.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VoiceResult(val transcript: String, val message: String, val saved: Boolean)

class FuelWorkflow(context: Context) {
    private val appContext = context.applicationContext
    private val store = FuelStore(appContext)
    private val whisper = WhisperEngine(appContext)
    private val parser = FuelCommandParser()

    suspend fun process(samples: FloatArray): VoiceResult = withContext(Dispatchers.IO) {
        if (samples.isEmpty()) return@withContext VoiceResult("", "No audio heard. Try again.", false)
        val transcript = whisper.transcribe(samples)
        val command = parser.parse(transcript)
            ?: return@withContext VoiceResult(transcript, "Could not read amount, litres and meter. Nothing saved.", false)
        store.save(command, transcript)
        VoiceResult(
            transcript,
            "Saved ${command.amountRupees.toInt()} rupees, ${command.litres} litres, meter ${command.odometerKm}.",
            true,
        )
    }

    suspend fun recent(): List<FuelEntry> = withContext(Dispatchers.IO) { store.recent() }
}
