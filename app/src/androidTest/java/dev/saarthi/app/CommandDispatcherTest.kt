package dev.saarthi.app

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommandDispatcherTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val action = ExternalAction.forCommand(VoiceCommand.OpenMaps)!!

    @Test fun missingHandlerReturnsUnavailable() {
        val refusingContext = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                throw ActivityNotFoundException("No Maps app")
            }
        }
        assertEquals(DispatchResult.Unavailable, CommandDispatcher(refusingContext).dispatch(action))
    }

    @Test fun securityRejectionReturnsUnavailable() {
        val refusingContext = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                throw SecurityException("Blocked")
            }
        }
        assertEquals(DispatchResult.Unavailable, CommandDispatcher(refusingContext).dispatch(action))
    }

    @Test fun launchPassesIntentAndOnlyReportsRequested() {
        var launched: Intent? = null
        val recordingContext = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                launched = intent
            }
        }
        assertEquals(DispatchResult.Requested, CommandDispatcher(recordingContext).dispatch(action))
        assertEquals("geo:0,0", launched?.dataString)
        assertEquals("com.google.android.apps.maps", launched?.`package`)
    }
}
