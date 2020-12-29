package uk.co.firstchoice_cs.core.database.entities;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "collection_item_table")
public class CollectionItem {

    @ColumnInfo(name = "deleteChecked")
    public boolean deleteChecked;

    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull
    @ColumnInfo(name = "name")
    private String mName;
    @NonNull
    @ColumnInfo(name = "address")
    private String mAddress;
    @NonNull
    @ColumnInfo(name = "manufacturer")
    private String mManufacturer;
    @NonNull
    @ColumnInfo(name = "model")
    private String mModel;
    @ColumnInfo(name = "collectionId")
    private int mCollectionId;


    public CollectionItem(int id, int collectionId, @NonNull String name, @NonNull String address, @NonNull String manufacturer, @NonNull String model) {
        this.id = id;
        this.mCollectionId = collectionId;
        this.mName = name;
        this.mAddress = address;
        this.mModel = model;
        this.mManufacturer = manufacturer;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull
    public Integer getCollectionId() {
        return mCollectionId;
    }

    public void setCollectionID(int cid) {
        mCollectionId = cid;
    }

    @NonNull
    public String getManufacturer() {
        return mManufacturer;
    }

    @NonNull
    public String getModel() {
        return mModel;
    }

    @NonNull
    public String getAddress() {
        return mAddress;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


}