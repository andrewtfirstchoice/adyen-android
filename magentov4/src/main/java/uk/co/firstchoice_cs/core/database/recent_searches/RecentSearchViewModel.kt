package uk.co.firstchoice_cs.core.database.recent_searches

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class RecentSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecentSearchRepository

    val allSearches: LiveData<List<RecentSearchItem>>

    init {
        val searchDao = RecentSearchDatabase.getDatabase(application, viewModelScope).recentSearchDao()
        repository = RecentSearchRepository(searchDao)
        allSearches = repository.allRecentSearches
    }

    fun insert(search: RecentSearchItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(search)
    }
}
