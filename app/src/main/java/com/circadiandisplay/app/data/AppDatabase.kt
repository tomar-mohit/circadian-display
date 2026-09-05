package com.circadiandisplay.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.circadiandisplay.app.data.dao.CurvePointDao
import com.circadiandisplay.app.data.dao.CurveProfileDao
import com.circadiandisplay.app.data.dao.ExcludedAppDao
import com.circadiandisplay.app.data.entity.CurvePointEntity
import com.circadiandisplay.app.data.entity.CurveProfileEntity
import com.circadiandisplay.app.data.entity.ExcludedAppEntity

@Database(
    entities = [
        CurveProfileEntity::class,
        CurvePointEntity::class,
        ExcludedAppEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curveProfileDao(): CurveProfileDao

    abstract fun curvePointDao(): CurvePointDao

    abstract fun excludedAppDao(): ExcludedAppDao
}
