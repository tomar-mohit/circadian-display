# Circadian Display ProGuard Rules

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# WorkManager
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# Kotlin Metadata
-keep class kotlin.Metadata { *; }

# Keep data classes used in persistence
-keep class com.circadiandisplay.core.curve.** { *; }
