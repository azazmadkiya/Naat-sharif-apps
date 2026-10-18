package naatsharif.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NaatEntity::class, UserFavoriteEntity::class],
    version = 3,
    exportSchema = false
)
abstract class NaatDatabase : RoomDatabase() {

    abstract fun naatDao(): NaatDao
    abstract fun userFavoriteDao(): UserFavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: NaatDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_favorites (
                        userEmail TEXT NOT NULL,
                        naatId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(userEmail, naatId)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_favorites_userEmail ON user_favorites(userEmail)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_favorites_naatId ON user_favorites(naatId)")

                // Migrate existing legacy favorites to Super Admin so they are not lost
                try {
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO user_favorites (userEmail, naatId, createdAt)
                        SELECT 'azazmadkiya@gmail.com', id, updatedAt FROM naat_items WHERE isFavorite = 1
                        """.trimIndent()
                    )
                    // Reset legacy global flag on naat_items so other users don't see them
                    db.execSQL("UPDATE naat_items SET isFavorite = 0")
                } catch (e: Throwable) {
                    // ignore if table format differs
                }
            }
        }

        fun getDatabase(context: Context): NaatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NaatDatabase::class.java,
                    "naat_sharif_db"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
