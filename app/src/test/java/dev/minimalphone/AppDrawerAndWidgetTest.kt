package dev.minimalphone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.minimalphone.ui.widgets.AppWidgetHostHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class AppDrawerAndWidgetTest {

    private lateinit var context: Context
    private lateinit var helper: AppWidgetHostHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        helper = AppWidgetHostHelper.get(context)
    }

    @Test
    fun appWidgetHelper_managesWidgetIdPersistence() {
        val testWidgetId1 = 101
        val testWidgetId2 = 102

        helper.addWidgetId(testWidgetId1)
        helper.addWidgetId(testWidgetId2)

        assertTrue(helper.widgetIds.value.contains(testWidgetId1))
        assertTrue(helper.widgetIds.value.contains(testWidgetId2))

        helper.removeWidgetId(testWidgetId1)
        assertFalse(helper.widgetIds.value.contains(testWidgetId1))
        assertTrue(helper.widgetIds.value.contains(testWidgetId2))

        helper.removeWidgetId(testWidgetId2)
        assertFalse(helper.widgetIds.value.contains(testWidgetId2))
    }

    @Test
    fun appWidgetHelper_allocatesAndHasHost() {
        assertNotNull(helper.appWidgetHost)
        assertNotNull(helper.appWidgetManager)
        val allocated = helper.allocateWidgetId()
        assertNotNull(allocated)
    }
}
