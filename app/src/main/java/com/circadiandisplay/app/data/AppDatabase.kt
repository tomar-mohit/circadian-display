package com.circadiandisplay.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.circadiandisplay.app.data.dao.CurvePointDao
import com.circadiandisplay.app.data.dao.CurveProfileDao
import com.circadiandisplay.app.data.entity.CurvePointEntity
import com.circadiandisplay.app.data.entity.CurveProfileEntity

@Database(
    entities = [CurveProfileEntity::class, CurvePointEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curveProfileDao(): CurveProfileDao
    abstract fun curvePointDao(): CurvePointDao
}
