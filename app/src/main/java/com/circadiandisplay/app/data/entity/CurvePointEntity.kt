package com.circadiandisplay.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.circadiandisplay.core.curve.CurvePoint

/**
 * Room entity for [CurvePoint].
 *
 * Foreign key to [CurveProfileEntity] with CASCADE delete —
 * deleting a profile automatically removes all its points.
 */
@Entity(
    tableName = "curve_points",
    foreignKeys = [
        ForeignKey(
            entity = CurveProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profile_id")],
)
data class CurvePointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "profile_id")
    val profileId: Long,
    @ColumnInfo(name = "time_minutes")
    val timeMinutes: Int,
    @ColumnInfo(name = "warmth")
    val warmth: Float,
    @ColumnInfo(name = "dimming")
    val dimming: Float,
)

fun CurvePointEntity.toDomain(): CurvePoint =
    CurvePoint(
        id = id,
        profileId = profileId,
        timeMinutes = timeMinutes,
        warmth = warmth,
        dimming = dimming,
    )

fun CurvePoint.toEntity(): CurvePointEntity =
    CurvePointEntity(
        id = id,
        profileId = profileId,
        timeMinutes = timeMinutes,
        warmth = warmth,
        dimming = dimming,
    )
