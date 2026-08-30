package com.circadiandisplay.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration scripts.
 *
 * Every schema change requires a numbered [Migration] here and must be
 * registered on the database builder in [com.circadiandisplay.app.di.DatabaseModule].
 * Destructive migration is never acceptable (see docs/data_model.md).
 */
object AppDatabaseMigrations {

    /**
     * v1 → v2: add the `excluded_apps` table for the App Exclusions feature.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `excluded_apps` (
                    `package_name` TEXT NOT NULL,
                    `display_name` TEXT NOT NULL,
                    `added_at` INTEGER NOT NULL,
                    PRIMARY KEY(`package_name`)
                )
                """.trimIndent()
            )
        }
    }
}
