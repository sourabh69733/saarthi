package dev.saarthi.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private enum class Page { HOME, LOG, HISTORY }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    private lateinit var whisper: WhisperEngine
    private lateinit var workflow: FuelWorkflow
    private lateinit var commandWorkflow: VoiceCommandWorkflow
    private lateinit var dispatcher: CommandDispatcher
    private lateinit var container: FrameLayout
    private var page = Page.HOME
    private var recording = false
    private var voiceStatus = "Ready when you are."
    private var voiceTranscript = ""
    private var commandBusy = false
    private var commandStatus = "Ready. Try “open Google Maps”."
    private var commandTranscript = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        whisper = WhisperEngine(applicationContext)
        workflow = FuelWorkflow(this, whisper)
        commandWorkflow = VoiceCommandWorkflow({ samples -> whisper.transcribe(samples) }, VoiceCommandParser())
        dispatcher = CommandDispatcher(this)
        setContentView(R.layout.activity_main)
        val root = findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
        container = findViewById(R.id.screen_container)
        findViewById<TextView>(R.id.nav_home).setOnClickListener { show(Page.HOME) }
        findViewById<TextView>(R.id.nav_log).setOnClickListener { show(Page.LOG) }
        findViewById<TextView>(R.id.nav_history).setOnClickListener { show(Page.HISTORY) }
        page = savedInstanceState?.getString("page")?.let { runCatching { Page.valueOf(it) }.getOrNull() } ?: Page.HOME
        commandStatus = savedInstanceState?.getString("commandStatus") ?: commandStatus
        commandTranscript = savedInstanceState?.getString("commandTranscript") ?: commandTranscript
        show(page)
        scope.launch {
            FuelStore.saves.collect {
                if (page == Page.HISTORY) show(Page.HISTORY)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("page", page.name)
        outState.putString("commandStatus", if (commandBusy) "Interrupted. Tap Speak to try again." else commandStatus)
        outState.putString("commandTranscript", commandTranscript)
        super.onSaveInstanceState(outState)
    }

    private fun show(destination: Page) {
        page = destination
        val layout = when (destination) {
            Page.HOME -> R.layout.screen_home
            Page.LOG -> R.layout.screen_log
            Page.HISTORY -> R.layout.screen_history
        }
        container.removeAllViews()
        val screen = LayoutInflater.from(this).inflate(layout, container, false)
        container.addView(screen)
        updateNavigation()
        when (destination) {
            Page.HOME -> bindHome(screen)
            Page.LOG -> bindLog(screen)
            Page.HISTORY -> bindHistory(screen)
        }
    }

    private fun updateNavigation() {
        for ((id, destination) in listOf(
            R.id.nav_home to Page.HOME,
            R.id.nav_log to Page.LOG,
            R.id.nav_history to Page.HISTORY,
        )) {
            val item = findViewById<TextView>(id)
            val selected = destination == page
            item.setTextColor(getColor(if (selected) R.color.accent else R.color.muted))
            item.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            item.isSelected = selected
        }
    }

    private fun bindHome(screen: View) {
        screen.findViewById<TextView>(R.id.home_speak).setOnClickListener { requestVoiceCommand() }
        updateCommandUi()
    }

    private fun bindLog(screen: View) {
        screen.findViewById<TextView>(R.id.record_button).setOnClickListener { requestMicAndRecord() }
        screen.findViewById<TextView>(R.id.save_manual).setOnClickListener { saveManual(screen) }
        updateVoiceUi()
    }

    private fun bindHistory(screen: View) {
        val list = screen.findViewById<LinearLayout>(R.id.history_list)
        scope.launch {
            val entries = workflow.recent()
            if (page != Page.HISTORY || container.getChildAt(0) !== screen) return@launch
            if (entries.isEmpty()) {
                list.addView(TextView(this@MainActivity).apply {
                    text = "No entries yet. Log your first fill-up to see it here."
                    setTextColor(getColor(R.color.muted))
                    textSize = 16f
                    setPadding(dp(20), dp(24), dp(20), dp(24))
                    setBackgroundResource(R.drawable.bg_card)
                })
            } else {
                entries.forEach { entry ->
                    val item = LayoutInflater.from(this@MainActivity)
                        .inflate(R.layout.item_fuel_entry, list, false)
                    item.findViewById<TextView>(R.id.entry_date).text = date(entry.savedAtMs)
                    item.findViewById<TextView>(R.id.entry_amount).text = currency.format(entry.amountRupees)
                    item.findViewById<TextView>(R.id.entry_detail).text =
                        "${entry.litres} litres  ·  ${entry.odometerKm} km"
                    list.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
                }
            }
        }
    }

    private fun saveManual(screen: View) {
        val amount = screen.findViewById<EditText>(R.id.amount_input).text.toString()
        val litres = screen.findViewById<EditText>(R.id.litres_input).text.toString()
        val odometer = screen.findViewById<EditText>(R.id.odometer_input).text.toString()
        val error = screen.findViewById<TextView>(R.id.manual_error)
        val command = ManualFuelEntry.parse(amount, litres, odometer)
        if (command == null) {
            error.text = "Enter a valid amount, litres, and whole-number odometer."
            error.visibility = View.VISIBLE
            return
        }
        error.visibility = View.GONE
        val save = screen.findViewById<TextView>(R.id.save_manual)
        save.isEnabled = false
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    FuelStore(this@MainActivity).use { it.save(command, "Manual entry") }
                }
                Toast.makeText(this@MainActivity, "Fuel entry saved", Toast.LENGTH_SHORT).show()
                show(Page.HOME)
            } catch (exception: Exception) {
                error.text = exception.message ?: "Could not save entry. Try again."
                error.visibility = View.VISIBLE
                save.isEnabled = true
            }
        }
    }

    private fun requestVoiceCommand() {
        if (commandBusy || recording) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 2)
            return
        }
        commandBusy = true
        commandStatus = "Listening for 6 seconds…"
        commandTranscript = ""
        updateCommandUi()
        scope.launch {
            try {
                val samples = AudioCapture.phone()
                commandStatus = "Transcribing offline…"
                updateCommandUi()
                val interpretation = commandWorkflow.interpret(samples)
                commandTranscript = interpretation.transcript
                val action = ExternalAction.forCommand(interpretation.command)
                commandStatus = when {
                    samples.isEmpty() -> "No audio heard. Tap Speak to try again."
                    action == null -> "I couldn't match a command. Try opening Maps, navigating, or playing music."
                    dispatcher.dispatch(action) == DispatchResult.Unavailable ->
                        "No compatible app could accept that request. Nothing was started."
                    else -> when (val command = interpretation.command) {
                        VoiceCommand.OpenMaps -> "Requested Google Maps. Check the Maps app."
                        is VoiceCommand.Navigate -> "Requested navigation to ${command.destination}. Check Maps."
                        is VoiceCommand.PlayMusic -> "Requested music for ${command.query}. Check your music app."
                        VoiceCommand.Unsupported -> "Command not supported."
                    }
                }
            } catch (exception: Exception) {
                commandStatus = exception.message ?: "Voice command failed. Tap Speak to try again."
            } finally {
                commandBusy = false
                updateCommandUi()
                updateVoiceUi()
            }
        }
    }

    private fun updateCommandUi() {
        if (page != Page.HOME) return
        val button = container.findViewById<TextView>(R.id.home_speak)
        button.isEnabled = !commandBusy && !recording
        button.alpha = if (button.isEnabled) 1f else 0.65f
        button.text = if (commandBusy) "Listening / thinking…" else "●  Speak"
        container.findViewById<TextView>(R.id.home_command_status).text = commandStatus
        container.findViewById<TextView>(R.id.home_command_transcript).apply {
            visibility = if (commandTranscript.isBlank()) View.GONE else View.VISIBLE
            text = "Heard: $commandTranscript"
        }
    }

    private fun requestMicAndRecord() {
        if (commandBusy || recording) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        recording = true
        voiceStatus = "Listening for 6 seconds…"
        voiceTranscript = ""
        updateVoiceUi()
        scope.launch {
            try {
                val samples = AudioCapture.phone()
                voiceStatus = "Transcribing offline…"
                updateVoiceUi()
                val result = workflow.process(samples)
                voiceStatus = result.message
                voiceTranscript = result.transcript
            } catch (exception: Exception) {
                voiceStatus = exception.message ?: "Recording failed. Try again."
            } finally {
                recording = false
                updateVoiceUi()
                updateCommandUi()
            }
        }
    }

    private fun updateVoiceUi() {
        if (page != Page.LOG) return
        val button = container.findViewById<TextView>(R.id.record_button)
        button.isEnabled = !recording && !commandBusy
        button.alpha = if (button.isEnabled) 1f else 0.65f
        button.text = if (recording) "Recording…" else "Start 6-second recording"
        container.findViewById<TextView>(R.id.record_status).text = voiceStatus
        container.findViewById<TextView>(R.id.record_transcript).apply {
            visibility = if (voiceTranscript.isBlank()) View.GONE else View.VISIBLE
            text = "Heard: $voiceTranscript"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                1 -> requestMicAndRecord()
                2 -> requestVoiceCommand()
            }
        } else {
            when (requestCode) {
                1 -> {
                    voiceStatus = "Microphone permission is needed for voice logging."
                    updateVoiceUi()
                }
                2 -> {
                    commandStatus = "Microphone permission is needed for voice commands."
                    updateCommandUi()
                }
            }
        }
    }

    private fun date(timestamp: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag("en-IN")).format(Date(timestamp))

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        scope.cancel()
        CoroutineScope(Dispatchers.IO).launch { whisper.close() }
        super.onDestroy()
    }
}
