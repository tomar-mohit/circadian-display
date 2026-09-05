package com.circadiandisplay.app.di

import com.circadiandisplay.app.foreground.ForegroundAppProvider
import com.circadiandisplay.app.foreground.UsageStatsForegroundAppProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ForegroundModule {
    @Binds
    @Singleton
    abstract fun bindForegroundAppProvider(impl: UsageStatsForegroundAppProvider): ForegroundAppProvider
}
