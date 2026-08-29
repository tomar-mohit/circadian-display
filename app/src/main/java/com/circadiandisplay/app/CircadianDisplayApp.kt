package com.circadiandisplay.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.circadiandisplay.app.data.seed.SeedData
import com.circadiandisplay.app.scheduler.SchedulerWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation
 * and serve as the parent component for the dependency graph.
 *
 * Implements [Configuration.Provider] to integrate [HiltWorkerFactory]
 * with WorkManager so that [SchedulerWorker] receives Hilt-injected
 * dependencies via [dagger.assisted.AssistedInject].
 *
 * Seeds default data on first launch via [SeedData].
 * Enqueues periodic WorkManager scheduler and triggers an immediate
 * evaluation on every cold start.
 */
@HiltAndroidApp
class CircadianDisplayApp : Application(), Configuration.Provider {

    @Inject
    lateinit var seedData: SeedData

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Seed default profile on first launch
        CoroutineScope(Dispatchers.IO).launch {
            seedData.seedIfNeeded()
        }

        // Enqueue periodic background evaluation (~15 min)
        SchedulerWorker.enqueuePeriodic(this)

        // Trigger an immediate evaluation so the display is correct on launch
        SchedulerWorker.triggerNow(this)
    }
}

