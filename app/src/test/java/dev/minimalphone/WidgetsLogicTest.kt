package dev.minimalphone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.minimalphone.ui.widgets.QuickToolsHelper
import dev.minimalphone.ui.widgets.StepCountManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class WidgetsLogicTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun scratchpadSavesAndReadsDailyIntention() {
        QuickToolsHelper.saveDailyNote(context, "Deep focus on design sprint")
        val note = QuickToolsHelper.readDailyNote(context)
        assertEquals("Deep focus on design sprint", note)
    }

    @Test
    fun stepCountManagerInitializesSafely() {
        val manager = StepCountManager.get(context)
        assertNotNull(manager)
        assertNotNull(manager.dailySteps.value)
    }
}
