package dev.saarthi.app

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class SaarthiCarService : CarAppService() {
    override fun createHostValidator(): HostValidator =
        if (BuildConfig.DEBUG) HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        else HostValidator.Builder(this).addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample).build()

    override fun onCreateSession(): Session = object : Session() {
        override fun onCreateScreen(intent: android.content.Intent): Screen = FuelScreen(carContext)
    }
}

private class FuelScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val workflow = FuelWorkflow(carContext)
    private var message = "Tap Speak, then say a fuel amount, litres and meter."
    private var busy = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null

    init {
        tts = TextToSpeech(carContext) { ready ->
            if (ready == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.language = Locale.forLanguageTag("en-IN")
                pendingSpeech?.let { speak(it) }
                pendingSpeech = null
            }
        }
    }

    override fun onGetTemplate(): Template {
        val pane = Pane.Builder().addRow(Row.Builder().setTitle(message).build())
        if (!busy) pane.addAction(Action.Builder().setTitle("Speak").setOnClickListener { startVoice() }.build())
        return PaneTemplate.Builder(pane.build()).setTitle("Saarthi fuel log").build()
    }

    private fun startVoice() {
        if (carContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            carContext.requestPermissions(listOf(Manifest.permission.RECORD_AUDIO)) { granted, _ ->
                if (granted.contains(Manifest.permission.RECORD_AUDIO)) startVoice()
                else { message = "Allow microphone permission on your phone."; invalidate() }
            }
            return
        }
        busy = true
        message = "Listening for 6 seconds…"
        invalidate()
        scope.launch {
            try {
                val samples = AudioCapture.car(carContext)
                message = "Transcribing offline…"
                invalidate()
                val result = workflow.process(samples)
                message = result.message
                speak(result.message)
            } catch (error: Exception) {
                message = error.message ?: "Recording failed. Try again."
            } finally {
                busy = false
                invalidate()
            }
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fuel-result")
        else pendingSpeech = text
    }
}
