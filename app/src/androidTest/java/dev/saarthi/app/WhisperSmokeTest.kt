package dev.saarthi.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhisperSmokeTest {
    @Test fun loadsBundledModelAndRunsOfflineInference() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = WhisperEngine(context)
        try {
            assertNotNull(engine.transcribe(FloatArray(16_000 * 6)))
        } finally {
            engine.close()
        }
    }
}
