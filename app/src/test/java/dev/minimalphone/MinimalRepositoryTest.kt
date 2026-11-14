package dev.minimalphone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.minimalphone.session.MinimalRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class MinimalRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: MinimalRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        MinimalRepository.resetForTests()
        context.getSharedPreferences("minimal_bro_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        repository = MinimalRepository.get(context)
    }

    @After
    fun tearDown() {
        repository.endSession(completed = false)
        MinimalRepository.resetForTests()
    }

    @Test
    fun unlimitedAppsAndOngoingSessionPersistCleanly() {
        val packages = (1..12).map { "com.example.app$it" }.toSet()
        repository.setAllowedPackages(packages)
        repository.toggleFavorite("com.example.app3")

        val active = repository.start()
        assertTrue(active.isIndefinite)
        assertTrue(active.allowedPackages.containsAll(packages))

        repository.endSession(completed = false)
        assertNull(repository.active.value)
        assertEquals(1, repository.history.value.size)
    }
}
