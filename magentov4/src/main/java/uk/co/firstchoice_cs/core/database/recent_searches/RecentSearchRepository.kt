package uk.co.firstchoice_cs.core.database.recent_searches

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

class RecentSearchRepository(private val searchDao: RecentSearchDao) {

    val allRecentSearches: LiveData<List<RecentSearchItem>> = searchDao.getRecentSearches()

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(word: RecentSearchItem) {
        searchDao.insert(word)
    }
}
