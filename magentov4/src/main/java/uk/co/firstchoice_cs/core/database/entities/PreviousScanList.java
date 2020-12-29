package uk.co.firstchoice_cs.core.database.entities;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "scan_table")
public class PreviousScanList {

    @Ignore
    public boolean isRecent;
    @Ignore
    public boolean flash;

    public PreviousScanList(@NonNull String mManufacturer, Boolean mObsolete, Integer mStock, @NonNull String mBarcode, @NonNull String mPartnumber, @NonNull String mImage, @NonNull String mPartname, @NonNull String mFCCPartnumber,  String mSuperseded, String mFCCSuperseded) {
        this.mManufacturer = mManufacturer;
        this.mObsolete = mObsolete;
        this.mStock = mStock;
        this.mBarcode = mBarcode;
        this.mPartnumber = mPartnumber;
        this.mImage = mImage;
        this.mPartname = mPartname;
        this.mFCCPartnumber = mFCCPartnumber;
        this.mSuperseded = mSuperseded;
        this.mFCCSuperseded = mFCCSuperseded;
    }

    @NonNull
    public String getManufacturer() { return mManufacturer; }

    @NonNull
    public String getBarcode() {
        return mBarcode;
    }

    @NonNull
    public String getPartnumber() {
        return mPartnumber;
    }

    @NonNull
    public String getFCCPartnumber() {
        return mFCCPartnumber;
    }

    @NonNull
    public String getPartname() {
        return mPartname;
    }

    @NonNull
    public String getImage() {
        return mImage;
    }

    public Boolean getObsolete() {
        return mObsolete;
    }

    public Integer getStock() {
        return mStock;
    }

    public String getSuperseded() {
        return mSuperseded;
    }

    public String getFCCSuperseded() {
        return mFCCSuperseded;
    }

    @NonNull
    @ColumnInfo(name = "manufacturer")
    private String mManufacturer;


    @ColumnInfo(name = "obsolete")
    private Boolean mObsolete;

    @ColumnInfo(name = "stock")
    private Integer mStock;

    @PrimaryKey(autoGenerate = false)
    @NonNull
    @ColumnInfo(name = "barcode")
    private String mBarcode;

    @NonNull
    @ColumnInfo(name = "partnumber")
    private String mPartnumber;

    @NonNull
    @ColumnInfo(name = "image")
    private String mImage;

    @NonNull
    @ColumnInfo(name = "partname")
    private String mPartname;

    @NonNull
    @ColumnInfo(name = "FCCpartnumber")
    private String mFCCPartnumber;

    @ColumnInfo(name = "FCCsuperceded")
    private String mFCCSuperseded;

    @ColumnInfo(name = "superceded")
    private String mSuperseded;
}