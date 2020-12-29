package uk.co.firstchoice_cs.core.database.recent_searches

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;


@Dao
interface RecentSearchDao {

    @Query("SELECT * from recent_search_table ORDER BY id ASC")
    fun getRecentSearches(): LiveData<List<RecentSearchItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(recent: RecentSearchItem)

    @Query("DELETE FROM recent_search_table")
    fun deleteAll()
}
