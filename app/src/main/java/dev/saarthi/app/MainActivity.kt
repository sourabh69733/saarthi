package dev.saarthi.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var workflow: FuelWorkflow
    private lateinit var status: TextView
    private lateinit var entries: TextView
    private lateinit var speak: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workflow = FuelWorkflow(this)

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        column.addView(TextView(this).apply {
            text = "Saarthi · offline fuel log"
            textSize = 24f
        })
        column.addView(TextView(this).apply {
            text = "Say: Petrol 1500 rupees, 14 litres, meter 45200. Recording lasts 6 seconds."
            textSize = 16f
        })
        speak = Button(this).apply {
            text = "Speak"
            setOnClickListener { requestMicAndRecord() }
        }
        column.addView(speak)
        status = TextView(this).apply { text = "Ready"; textSize = 16f }
        column.addView(status)

        val typed = EditText(this).apply { hint = "Or type a fuel command for testing" }
        column.addView(typed)
        column.addView(Button(this).apply {
            text = "Save typed command"
            setOnClickListener {
                val text = typed.text.toString()
                val command = FuelCommandParser().parse(text)
                if (command == null) {
                    status.text = "Could not read amount, litres and meter. Nothing saved."
                } else {
                    scope.launch {
                        withContext(Dispatchers.IO) { FuelStore(this@MainActivity).save(command, text) }
                        status.text = "Saved ${command.amountRupees.toInt()} rupees, ${command.litres} litres, meter ${command.odometerKm}."
                        typed.text.clear()
                        refreshEntries()
                    }
                }
            }
        })
        column.addView(TextView(this).apply { text = "Recent entries"; textSize = 20f })
        entries = TextView(this)
        column.addView(entries)
        setContentView(ScrollView(this).apply {
            addView(column, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        refreshEntries()
    }

    private fun requestMicAndRecord() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        speak.isEnabled = false
        status.text = "Listening for 6 seconds…"
        scope.launch {
            try {
                val samples = AudioCapture.phone()
                status.text = "Transcribing offline…"
                val result = workflow.process(samples)
                status.text = "${result.message}\nHeard: ${result.transcript}"
                refreshEntries()
            } catch (error: Exception) {
                status.text = error.message ?: "Recording failed"
            } finally {
                speak.isEnabled = true
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            requestMicAndRecord()
        } else {
            status.text = "Microphone permission is needed for voice logging."
        }
    }

    private fun refreshEntries() {
        scope.launch {
            val recent = workflow.recent()
            entries.text = if (recent.isEmpty()) "No fuel entries yet" else recent.joinToString("\n\n") {
                "${DateFormat.getDateTimeInstance().format(Date(it.savedAtMs))}\n₹${it.amountRupees} · ${it.litres} L · ${it.odometerKm} km"
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
