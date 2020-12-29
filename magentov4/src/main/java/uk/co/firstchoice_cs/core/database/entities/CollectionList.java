package uk.co.firstchoice_cs.core.database.entities;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Entity(tableName = "collection_table")
public class CollectionList {

    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull
    @ColumnInfo(name = "name")
    private String mName;

    public CollectionList(int id, @NonNull String name) {
        this.id = id;
        this.mName = name;
    }

    public void setName(@NonNull String mName) {
        this.mName = mName;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public int getId() {
        return id;
    }

    @Ignore
    public String headerValue = "";

    public static void sortCollectionsByName(List<CollectionList> collectionLists) {
        Collections.sort(collectionLists, (Comparator) (o1, o2) -> {

            String x1 = ((CollectionList) o1).getName().toUpperCase();
            String x2 = ((CollectionList) o2).getName().toUpperCase();
            return x1.compareTo(x2);
        });
    }
}