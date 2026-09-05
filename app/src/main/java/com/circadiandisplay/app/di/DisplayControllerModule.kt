package com.circadiandisplay.app.di

import com.circadiandisplay.core.curve.DisplayController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DisplayControllerModule {
    @Binds
    @Singleton
    abstract fun bindDisplayController(composite: CompositeDisplayController): DisplayController
}
