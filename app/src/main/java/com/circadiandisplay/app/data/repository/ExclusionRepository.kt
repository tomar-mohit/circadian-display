package com.circadiandisplay.app.data.repository

import com.circadiandisplay.app.data.dao.ExcludedAppDao
import com.circadiandisplay.app.data.entity.ExcludedAppEntity
import com.circadiandisplay.app.data.entity.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository bridging Room persistence and the [ExcludedApp] domain model.
 *
 * Consumed by both the scheduler (to decide whether to suspend display
 * adjustments) and the Exclusions screen (to render and mutate the list).
 */
@Singleton
class ExclusionRepository @Inject constructor(
    private val dao: ExcludedAppDao,
) {
    fun observeAll(): Flow<List<ExcludedApp>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    /** Reactive set of excluded package names for the scheduler/UI. */
    fun observeExcludedPackages(): Flow<Set<String>> =
        dao.getExcludedPackages().map { it.toSet() }

    suspend fun isExcluded(packageName: String): Boolean =
        dao.isExcluded(packageName)

    suspend fun addExclusion(packageName: String, displayName: String) {
        dao.insert(
            ExcludedAppEntity(
                packageName = packageName,
                displayName = displayName,
            )
        )
    }

    suspend fun removeExclusion(packageName: String) {
        dao.deleteByPackage(packageName)
    }
}
