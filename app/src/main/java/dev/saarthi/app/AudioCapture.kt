package dev.saarthi.app

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import androidx.car.app.CarContext
import androidx.car.app.media.CarAudioRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioCapture {
    private const val SAMPLE_RATE = 16_000
    private const val MAX_SECONDS = 6
    private const val MAX_BYTES = SAMPLE_RATE * MAX_SECONDS * 2

    @SuppressLint("MissingPermission")
    suspend fun phone(): FloatArray = withContext(Dispatchers.IO) {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        check(minBuffer > 0) { "Microphone is unavailable" }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, 4096),
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Microphone could not start" }
        val samples = FloatArray(SAMPLE_RATE * MAX_SECONDS)
        try {
            recorder.startRecording()
            var offset = 0
            while (offset < samples.size) {
                val data = ShortArray(minOf(2048, samples.size - offset))
                val read = recorder.read(data, 0, data.size)
                if (read <= 0) break
                for (i in 0 until read) samples[offset + i] = data[i] / 32768f
                offset += read
            }
            samples.copyOf(offset)
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun car(carContext: CarContext): FloatArray = withContext(Dispatchers.IO) {
        val recorder = CarAudioRecord.create(carContext)
        val audioManager = carContext.getSystemService(AudioManager::class.java)
            ?: error("Car audio is unavailable")
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).build())
            .setOnAudioFocusChangeListener { state ->
                if (state == AudioManager.AUDIOFOCUS_LOSS) runCatching { recorder.stopRecording() }
            }.build()
        check(audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            "Could not acquire car audio"
        }
        val bytes = ByteArray(MAX_BYTES)
        try {
            recorder.startRecording()
            var offset = 0
            val chunk = ByteArray(CarAudioRecord.AUDIO_CONTENT_BUFFER_SIZE)
            while (offset < MAX_BYTES) {
                val read = recorder.read(chunk, 0, minOf(chunk.size, MAX_BYTES - offset))
                if (read <= 0) break
                chunk.copyInto(bytes, offset, 0, read)
                offset += read
            }
            val samples = FloatArray(offset / 2)
            for (i in samples.indices) {
                val low = bytes[i * 2].toInt() and 0xff
                val high = bytes[i * 2 + 1].toInt()
                samples[i] = ((high shl 8) or low).toShort() / 32768f
            }
            samples
        } finally {
            runCatching { recorder.stopRecording() }
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
    }
}
