package com.taskmgr.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MomentEntity::class,
        HabitGroupEntity::class,
        TaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        CalendarSyncEventEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class TaskMgrDatabase : RoomDatabase() {
    abstract fun momentDao(): MomentDao
    abstract fun habitGroupDao(): HabitGroupDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun calendarSyncDao(): CalendarSyncDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_groups` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorArgb` INTEGER NOT NULL,
                        `iconGlyph` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habits_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `notes` TEXT,
                        `startEpochDay` INTEGER NOT NULL,
                        `activeDaysMask` INTEGER NOT NULL,
                        `momentId` INTEGER,
                        `groupId` INTEGER,
                        `iconGlyph` TEXT,
                        `scheduledMinutesOfDay` INTEGER,
                        `repeatIntervalMinutes` INTEGER,
                        `aggressiveFullScreen` INTEGER NOT NULL,
                        `vibrationEnabled` INTEGER NOT NULL,
                        `completionType` TEXT NOT NULL,
                        `incrementAmount` REAL NOT NULL,
                        `incrementUnitLabel` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `targetUnitLabel` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        FOREIGN KEY(`momentId`) REFERENCES `moments`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`groupId`) REFERENCES `habit_groups`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `habits_new` (
                        id, title, notes, startEpochDay, activeDaysMask, momentId, groupId, iconGlyph,
                        scheduledMinutesOfDay, repeatIntervalMinutes, aggressiveFullScreen, vibrationEnabled,
                        completionType, incrementAmount, incrementUnitLabel, targetAmount, targetUnitLabel, isActive
                    )
                    SELECT
                        id, title, notes, startEpochDay, activeDaysMask, momentId, NULL, NULL,
                        scheduledMinutesOfDay, repeatIntervalMinutes, aggressiveFullScreen, vibrationEnabled,
                        completionType, incrementAmount, incrementUnitLabel, targetAmount, targetUnitLabel, isActive
                    FROM `habits`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `habits`")
                db.execSQL("ALTER TABLE `habits_new` RENAME TO `habits`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_momentId` ON `habits` (`momentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_groupId` ON `habits` (`groupId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calendar_sync_events` (
                        `itemType` TEXT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `calendarId` INTEGER NOT NULL,
                        `eventId` INTEGER NOT NULL,
                        `lastSyncedHash` INTEGER NOT NULL,
                        `lastSyncedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`itemType`, `itemId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `urgency` TEXT NOT NULL DEFAULT 'LOW'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `advanceRemindersSpec` TEXT")
            }
        }

        @Volatile
        private var INSTANCE: TaskMgrDatabase? = null

        fun getInstance(context: Context): TaskMgrDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskMgrDatabase::class.java,
                    "taskmgr.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
