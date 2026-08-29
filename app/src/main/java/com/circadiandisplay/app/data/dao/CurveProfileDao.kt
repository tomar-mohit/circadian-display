package com.circadiandisplay.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.circadiandisplay.app.data.entity.CurveProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurveProfileDao {

    @Query("SELECT * FROM curve_profiles ORDER BY created_at DESC")
    fun getAll(): Flow<List<CurveProfileEntity>>

    @Query("SELECT * FROM curve_profiles WHERE id = :id")
    suspend fun getById(id: Long): CurveProfileEntity?

    @Query("SELECT * FROM curve_profiles WHERE is_active = 1 LIMIT 1")
    suspend fun getActive(): CurveProfileEntity?

    @Query("SELECT * FROM curve_profiles WHERE is_active = 1 LIMIT 1")
    fun observeActive(): Flow<CurveProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: CurveProfileEntity): Long

    @Update
    suspend fun update(profile: CurveProfileEntity)

    @Delete
    suspend fun delete(profile: CurveProfileEntity)

    @Query("SELECT COUNT(*) FROM curve_profiles")
    suspend fun count(): Int

    /**
     * Atomically deactivates all profiles and activates the one with [profileId].
     * Enforces the invariant that only one profile is active at a time.
     */
    @Transaction
    suspend fun setActive(profileId: Long) {
        deactivateAll()
        activate(profileId)
    }

    @Query("UPDATE curve_profiles SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE curve_profiles SET is_active = 1 WHERE id = :id")
    suspend fun activate(id: Long)
}
