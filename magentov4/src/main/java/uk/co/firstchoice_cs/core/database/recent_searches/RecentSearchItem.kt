package uk.co.firstchoice_cs.core.database.recent_searches

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_search_table")
data class RecentSearchItem(@ColumnInfo(name = "JsonData") val data: String?,
                            @ColumnInfo(name = "Partnum") val part_num: String?){
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}
