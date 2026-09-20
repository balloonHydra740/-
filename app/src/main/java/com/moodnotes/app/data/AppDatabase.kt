package com.moodnotes.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MoodRecord::class, DiaryEntry::class, CustomMood::class, SavedTheme::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun moodDao(): MoodDao

    abstract fun diaryDao(): DiaryDao

    abstract fun customMoodDao(): CustomMoodDao

    abstract fun savedThemeDao(): SavedThemeDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** v1 -> v2：新增自定义心情表、心情/日记的自定义心情引用、日记图片与草稿字段。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mood_records ADD COLUMN customMoodId INTEGER")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN customMoodId INTEGER")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN images TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_moods (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, iconType TEXT NOT NULL, iconValue TEXT NOT NULL, " +
                        "containerColor INTEGER NOT NULL, sortOrder INTEGER NOT NULL, createdAt INTEGER NOT NULL)"
                )
            }
        }

        /** v2 -> v3：新增主题册表。 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS saved_themes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, seedColor INTEGER NOT NULL, " +
                        "sortOrder INTEGER NOT NULL, createdAt INTEGER NOT NULL)"
                )
            }
        }

        /** v3 -> v4：日记新增视频路径字段。 */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN videos TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moodnotes.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
