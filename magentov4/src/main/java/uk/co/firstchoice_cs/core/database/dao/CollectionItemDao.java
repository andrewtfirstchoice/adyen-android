package uk.co.firstchoice_cs.core.database.dao;

/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import uk.co.firstchoice_cs.core.database.entities.CollectionItem;


/**
 * The Room Magic is in this file, where you map a Java method call to an SQL query.
 * <p>
 * When you are using complex documentItem types, such as Date, you have to also supply type converters.
 * To keep this example basic, no types that require type converters are used.
 * See the documentation at
 * https://developer.android.com/topic/libraries/architecture/room.html#type-converters
 */

/**
 * The Room Magic is in this file, where you map a Java method call to an SQL query.
 * <p>
 * When you are using complex documentItem types, such as Date, you have to also supply type converters.
 * To keep this example basic, no types that require type converters are used.
 * See the documentation at
 * https://developer.android.com/topic/libraries/architecture/room.html#type-converters
 */

@Dao
public interface CollectionItemDao {

    // LiveData is a documentItem holder class that can be observed within a given lifecycle.
    // Always holds/caches latest version of documentItem. Notifies its active observers when the
    // documentItem has cachedChanged. Since we are getting all the contents of the database,
    // we are notified whenever any of the database contents have cachedChanged.
    @Query("SELECT * from collection_item_table ORDER BY name ASC")
    LiveData<List<CollectionItem>> getCollectionItems();

    @Query("SELECT * from collection_item_table where collectionId = :cId ORDER BY name ASC")
    LiveData<List<CollectionItem>> getCollectionItemsById(long cId);

    // We do not need a conflict strategy, because the jobList is our primary key, and you cannot
    // add two items with the same primary key to the database. If the table has icon_more than one
    // column, you can use @Insert(onConflict = OnConflictStrategy.REPLACE) to update a row.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CollectionItem coll);

    @Update
    void update(CollectionItem coll);

    @Delete
    void delete(CollectionItem collectionList);

    @Query("DELETE FROM collection_item_table")
    void clear();

    @Query("DELETE FROM collection_item_table where id = :itemId")
    void deleteItemFromCollection(int itemId);

    @Query("DELETE FROM collection_item_table where collectionId = :collectionID")
    void deleteAllItemsWithID(int collectionID);

    @Query("DELETE FROM collection_item_table where name = :name")
    void deleteAllItemsWithName(String name);
}
