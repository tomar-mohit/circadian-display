package com.circadiandisplay.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.circadiandisplay.app.data.entity.CurvePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurvePointDao {

    @Query("SELECT * FROM curve_points WHERE profile_id = :profileId ORDER BY time_minutes ASC")
    fun getByProfileId(profileId: Long): Flow<List<CurvePointEntity>>

    @Query("SELECT * FROM curve_points WHERE profile_id = :profileId ORDER BY time_minutes ASC")
    suspend fun getByProfileIdOnce(profileId: Long): List<CurvePointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<CurvePointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: CurvePointEntity): Long

    @Update
    suspend fun update(point: CurvePointEntity)

    @Delete
    suspend fun delete(point: CurvePointEntity)

    @Query("DELETE FROM curve_points WHERE id = :id")
    suspend fun deleteById(id: Long)
}
