package uk.co.firstchoice_cs.core.database.recent_searches

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope


@Database(entities = [RecentSearchItem::class], version = 6)
abstract class RecentSearchDatabase : RoomDatabase() {

    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        @Volatile
        private var INSTANCE: RecentSearchDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): RecentSearchDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        RecentSearchDatabase::class.java,
                        "recent_search_database"
                )
                        // Wipes and rebuilds instead of migrating if no Migration object.
                        // Migration is not part of this codelab.
                        .fallbackToDestructiveMigration()
                        .addCallback(RecentSearchDatabaseCallback(scope))
                        .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }

        private class RecentSearchDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // If you want to keep the data through app restarts,
                // comment out the following line.
               // INSTANCE?.let { database ->
                 //   scope.launch(Dispatchers.IO) {
                 //       populateDatabase(database.recentSearchDao())
              //      }
             //   }
            }
        }

        fun populateDatabase(searchDao: RecentSearchDao) {
            // Start the app with a clean database every time.
            // Not needed if you only populate on creation.
            searchDao.deleteAll()
        }
    }

}
