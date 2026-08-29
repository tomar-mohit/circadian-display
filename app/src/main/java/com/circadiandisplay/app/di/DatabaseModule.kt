package com.circadiandisplay.app.di

import android.content.Context
import androidx.room.Room
import com.circadiandisplay.app.data.AppDatabase
import com.circadiandisplay.app.data.dao.CurvePointDao
import com.circadiandisplay.app.data.dao.CurveProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "circadian_display.db",
        ).build()

    @Provides
    fun provideCurveProfileDao(db: AppDatabase): CurveProfileDao = db.curveProfileDao()

    @Provides
    fun provideCurvePointDao(db: AppDatabase): CurvePointDao = db.curvePointDao()
}
