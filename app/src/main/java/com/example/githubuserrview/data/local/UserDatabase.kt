package com.example.githubuserrview.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoriteUser::class, CachedGithubProfile::class, CachedGithubRepo::class],
    version = 2,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        @JvmStatic
        fun getDatabase(context: Context): UserDatabase {
            if (INSTANCE == null) {
                synchronized(UserDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        UserDatabase::class.java, "user_database")
                        .addMigrations(MIGRATION_1_2)
                        .build()
                }
            }
            return INSTANCE as UserDatabase
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_github_profile` (
                        `login` TEXT NOT NULL,
                        `name` TEXT,
                        `bio` TEXT,
                        `company` TEXT,
                        `location` TEXT,
                        `email` TEXT,
                        `avatarUrl` TEXT NOT NULL,
                        `htmlUrl` TEXT NOT NULL,
                        `createdAt` TEXT,
                        `type` TEXT NOT NULL,
                        `followers` INTEGER NOT NULL,
                        `following` INTEGER NOT NULL,
                        `publicRepos` INTEGER NOT NULL,
                        `publicGists` INTEGER NOT NULL,
                        `twitterUsername` TEXT,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`login`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_github_repo` (
                        `fullName` TEXT NOT NULL,
                        `repoId` INTEGER NOT NULL,
                        `ownerLogin` TEXT NOT NULL,
                        `ownerAvatarUrl` TEXT,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `htmlUrl` TEXT NOT NULL,
                        `homepage` TEXT,
                        `language` TEXT,
                        `defaultBranch` TEXT,
                        `size` INTEGER,
                        `stargazersCount` INTEGER NOT NULL,
                        `forksCount` INTEGER NOT NULL,
                        `watchersCount` INTEGER NOT NULL,
                        `openIssuesCount` INTEGER NOT NULL,
                        `licenseName` TEXT,
                        `updatedAt` TEXT,
                        `isPrivate` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`fullName`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_cached_github_repo_ownerLogin` ON `cached_github_repo` (`ownerLogin`)"
                )
            }
        }
    }
    abstract fun favoriteUserDao() : FavoriteUserDao
    abstract fun githubCacheDao(): GithubCacheDao
}
