package com.circadiandisplay.app.data.repository

import com.circadiandisplay.app.data.dao.CurvePointDao
import com.circadiandisplay.app.data.dao.CurveProfileDao
import com.circadiandisplay.app.data.entity.toDomain
import com.circadiandisplay.app.data.entity.toEntity
import com.circadiandisplay.core.curve.CurvePoint
import com.circadiandisplay.core.curve.CurveProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository bridging Room persistence and domain models.
 *
 * Handles entity ↔ domain mapping and enforces the single-active-profile
 * invariant via [CurveProfileDao.setActive].
 */
@Singleton
class CurveRepository @Inject constructor(
    private val profileDao: CurveProfileDao,
    private val pointDao: CurvePointDao,
) {
    // ── Profiles ──────────────────────────────────────────────────────────

    fun getAllProfiles(): Flow<List<CurveProfile>> =
        profileDao.getAll().map { entities -> entities.map { it.toDomain() } }

    fun observeActiveProfile(): Flow<CurveProfile?> =
        profileDao.observeActive().map { it?.toDomain() }

    suspend fun getActiveProfile(): CurveProfile? =
        profileDao.getActive()?.toDomain()

    suspend fun getProfileById(id: Long): CurveProfile? =
        profileDao.getById(id)?.toDomain()

    suspend fun insertProfile(profile: CurveProfile): Long =
        profileDao.insert(profile.toEntity())

    suspend fun updateProfile(profile: CurveProfile) =
        profileDao.update(profile.toEntity())

    suspend fun deleteProfile(profile: CurveProfile) =
        profileDao.delete(profile.toEntity())

    suspend fun setActiveProfile(profileId: Long) =
        profileDao.setActive(profileId)

    suspend fun getProfileCount(): Int =
        profileDao.count()

    suspend fun getFirstProfile(): CurveProfile? =
        profileDao.getFirst()?.toDomain()

    // ── Points ────────────────────────────────────────────────────────────

    fun getPointsByProfileId(profileId: Long): Flow<List<CurvePoint>> =
        pointDao.getByProfileId(profileId).map { entities -> entities.map { it.toDomain() } }

    suspend fun getPointsByProfileIdOnce(profileId: Long): List<CurvePoint> =
        pointDao.getByProfileIdOnce(profileId).map { it.toDomain() }

    suspend fun insertPoints(points: List<CurvePoint>) =
        pointDao.insertAll(points.map { it.toEntity() })

    suspend fun insertPoint(point: CurvePoint): Long =
        pointDao.insert(point.toEntity())

    suspend fun updatePoint(point: CurvePoint) =
        pointDao.update(point.toEntity())

    suspend fun deletePoint(point: CurvePoint) =
        pointDao.delete(point.toEntity())

    suspend fun deletePointById(id: Long) =
        pointDao.deleteById(id)

    suspend fun replacePoints(profileId: Long, points: List<CurvePoint>) =
        pointDao.replacePoints(profileId, points.map { it.toEntity() })
}

