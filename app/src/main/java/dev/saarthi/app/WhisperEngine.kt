package dev.saarthi.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WhisperEngine(private val context: Context) {
    private var handle = 0L

    @Synchronized
    private fun ensureModel(): Long {
        if (handle != 0L) return handle
        val model = File(context.filesDir, "ggml-base.bin")
        if (!model.exists() || model.length() != MODEL_BYTES) {
            context.assets.open("ggml-base.bin").use { input ->
                model.outputStream().use { output -> input.copyTo(output) }
            }
        }
        handle = initNative(model.absolutePath)
        check(handle != 0L) { "Offline speech model could not load" }
        return handle
    }

    suspend fun transcribe(samples: FloatArray): String = withContext(Dispatchers.IO) {
        synchronized(this@WhisperEngine) {
            transcribeNative(ensureModel(), samples).trim()
        }
    }

    @Synchronized
    fun close() {
        if (handle != 0L) freeNative(handle)
        handle = 0L
    }

    private external fun initNative(modelPath: String): Long
    private external fun transcribeNative(handle: Long, samples: FloatArray): String
    private external fun freeNative(handle: Long)

    companion object {
        private const val MODEL_BYTES = 147_951_465L
        init { System.loadLibrary("saarthi_voice") }
    }
}
