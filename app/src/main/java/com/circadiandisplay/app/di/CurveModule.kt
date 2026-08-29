package com.circadiandisplay.app.di

import com.circadiandisplay.core.curve.CurveEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CurveModule {

    @Provides
    @Singleton
    fun provideCurveEngine(): CurveEngine = CurveEngine()
}
