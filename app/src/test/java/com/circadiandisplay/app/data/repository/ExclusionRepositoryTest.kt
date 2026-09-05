package com.circadiandisplay.app.data.repository

import android.app.Application
import androidx.room.Room
import com.circadiandisplay.app.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExclusionRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: ExclusionRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication() as Application
        db =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = ExclusionRepository(db.excludedAppDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addExclusion_thenIsExcluded_returnsTrue() =
        runBlocking {
            repository.addExclusion("com.example.camera", "Camera")

            assertTrue(repository.isExcluded("com.example.camera"))
        }

    @Test
    fun removeExclusion_thenIsExcluded_returnsFalse() =
        runBlocking {
            repository.addExclusion("com.example.camera", "Camera")
            repository.removeExclusion("com.example.camera")

            assertFalse(repository.isExcluded("com.example.camera"))
        }

    @Test
    fun observeExcludedPackages_emitsAllAddedPackages() =
        runBlocking {
            repository.addExclusion("com.example.camera", "Camera")
            repository.addExclusion("com.example.gallery", "Gallery")

            assertEquals(
                setOf("com.example.camera", "com.example.gallery"),
                repository.observeExcludedPackages().first(),
            )
        }

    @Test
    fun addDuplicateExclusion_isIgnored() =
        runBlocking {
            repository.addExclusion("com.example.camera", "Camera")
            repository.addExclusion("com.example.camera", "Renamed Camera")

            assertEquals(
                setOf("com.example.camera"),
                repository.observeExcludedPackages().first(),
            )
        }

    @Test
    fun isExcluded_unknownPackage_returnsFalse() =
        runBlocking {
            assertFalse(repository.isExcluded("com.example.nonexistent"))
        }
}
