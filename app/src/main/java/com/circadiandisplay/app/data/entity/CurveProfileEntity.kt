package com.circadiandisplay.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.circadiandisplay.core.curve.CurveProfile

/**
 * Room entity for [CurveProfile].
 *
 * Mirrors the domain model but adds Room annotations.
 * Mapped to/from domain via extension functions.
 */
@Entity(tableName = "curve_profiles")
data class CurveProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)

fun CurveProfileEntity.toDomain(): CurveProfile = CurveProfile(
    id = id,
    name = name,
    isActive = isActive,
    createdAt = createdAt,
)

fun CurveProfile.toEntity(): CurveProfileEntity = CurveProfileEntity(
    id = id,
    name = name,
    isActive = isActive,
    createdAt = createdAt,
)
