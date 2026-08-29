package com.circadiandisplay.app.data.seed

import com.circadiandisplay.app.data.entity.CurvePointEntity
import com.circadiandisplay.app.data.entity.CurveProfileEntity
import com.circadiandisplay.app.data.dao.CurvePointDao
import com.circadiandisplay.app.data.dao.CurveProfileDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inserts default data on first launch when the database is empty.
 *
 * Creates one "Evening" profile with four points covering 18:00–23:00,
 * matching the seed data specification in [docs/data_model.md].
 */
@Singleton
class SeedData @Inject constructor(
    private val profileDao: CurveProfileDao,
    private val pointDao: CurvePointDao,
) {
    suspend fun seedIfNeeded() {
        if (profileDao.count() > 0) return

        val profileId = profileDao.insert(
            CurveProfileEntity(
                name = "Evening",
                isActive = true,
                createdAt = System.currentTimeMillis(),
            )
        )

        pointDao.insertAll(
            listOf(
                // 18:00 — neutral
                CurvePointEntity(
                    profileId = profileId,
                    timeMinutes = 1080,
                    warmth = 0.0f,
                    dimming = 0.0f,
                ),
                // 20:00 — subtle warm
                CurvePointEntity(
                    profileId = profileId,
                    timeMinutes = 1200,
                    warmth = 0.4f,
                    dimming = 0.1f,
                ),
                // 22:00 — pronounced warm + dim
                CurvePointEntity(
                    profileId = profileId,
                    timeMinutes = 1320,
                    warmth = 0.8f,
                    dimming = 0.3f,
                ),
                // 23:00 — maximum warm + dim
                CurvePointEntity(
                    profileId = profileId,
                    timeMinutes = 1380,
                    warmth = 1.0f,
                    dimming = 0.5f,
                ),
            )
        )
    }
}
