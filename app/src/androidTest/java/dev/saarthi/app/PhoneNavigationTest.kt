package dev.saarthi.app

import android.content.Intent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoneNavigationTest {
    @Test fun historyRefreshesWhenFuelIsSavedWhileVisible() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ) as MainActivity
        try {
            instrumentation.runOnMainSync {
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("history_nav").performClick()
            }
            instrumentation.waitForIdleSync()
            val odometer = 8_000_000 + (System.currentTimeMillis() % 1_000_000).toInt()
            fun visible(): Boolean {
                var found = false
                instrumentation.runOnMainSync {
                    val entries = activity.findViewById<android.widget.LinearLayout>(R.id.history_list)
                    found = entries != null && (0 until entries.childCount).any { index ->
                        entries.getChildAt(index).findViewById<android.widget.TextView>(R.id.entry_detail)
                            ?.text?.contains("$odometer km") == true
                    }
                }
                return found
            }
            assertFalse("The new entry must not already be visible", visible())
            FuelStore(instrumentation.targetContext).use {
                it.save(FuelCommand(4321.67, 12.0, odometer), "Instrumentation refresh test")
            }
            val deadline = System.currentTimeMillis() + 2_000
            while (!visible() && System.currentTimeMillis() < deadline) {
                instrumentation.waitForIdleSync()
                Thread.sleep(50)
            }
            assertTrue("History did not refresh after a fuel save", visible())
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test fun voiceHomeHasSpeakAndFuelNavigation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ) as MainActivity
        try {
            assertNotNull(activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("speak_button"))
            assertNotNull(activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("log_nav"))
            assertNotNull(activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("history_nav"))
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test fun speakIsDisabledAsSoonAsRecordingStarts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName,
            android.Manifest.permission.RECORD_AUDIO)
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ) as MainActivity
        try {
            instrumentation.runOnMainSync {
                val speak = activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("speak_button")
                assertNotNull(speak)
                speak.performClick()
                assertFalse("A second tap must not start another capture", speak.isEnabled)
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test fun contentStartsBelowStatusBar() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ) as MainActivity
        try {
            val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
            val statusBar = ViewCompat.getRootWindowInsets(root)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            assertTrue("root padding ${root.paddingTop} must clear status bar $statusBar", root.paddingTop >= statusBar)
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test fun homeCanOpenLogAndHistoryScreens() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent) as MainActivity
        try {
            assertNotNull(activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("home_screen"))
            instrumentation.runOnMainSync {
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("log_nav").performClick()
            }
            assertNotNull(activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("log_screen"))
            instrumentation.runOnMainSync {
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("history_nav").performClick()
            }
            assertNotNull(activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("history_screen"))
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
