package com.circadiandisplay.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.circadiandisplay.app.data.entity.ExcludedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcludedAppDao {
    @Query("SELECT * FROM excluded_apps ORDER BY added_at DESC")
    fun getAll(): Flow<List<ExcludedAppEntity>>

    @Query("SELECT package_name FROM excluded_apps")
    fun getExcludedPackages(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM excluded_apps WHERE package_name = :packageName)")
    suspend fun isExcluded(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: ExcludedAppEntity)

    @Query("DELETE FROM excluded_apps WHERE package_name = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
