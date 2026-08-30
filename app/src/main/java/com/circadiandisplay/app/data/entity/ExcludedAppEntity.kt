package com.circadiandisplay.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.circadiandisplay.app.data.repository.ExcludedApp

/**
 * Room entity for [ExcludedApp].
 *
 * The package name is the primary key — Android package names are unique
 * per device, so they are the natural identifier for an exclusion.
 * [displayName] is stored for display only and may become stale if the
 * app is later renamed; the exclusion is authoritative by [packageName].
 */
@Entity(tableName = "excluded_apps")
data class ExcludedAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
)

fun ExcludedAppEntity.toDomain(): ExcludedApp = ExcludedApp(
    packageName = packageName,
    displayName = displayName,
    addedAt = addedAt,
)
