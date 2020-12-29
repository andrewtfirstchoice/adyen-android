package uk.co.firstchoice_cs.core.api.legacyAPI.models;


import android.os.Environment;

import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.Serializable;

public class Catalogue implements Serializable {
    public boolean delete = false;
    String DOCUMENT_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/FirstChoice";

    @SerializedName("ModelID")
    private String ModelID;
    @SerializedName("MfrPrefix")
    private String MfrPrefix;
    @SerializedName("MfrName")
    private String MfrName;
    @SerializedName("ModelName")
    private String ModelName;
    @SerializedName("Category")
    private String Category;
    @SerializedName("FileName")
    private String FileName;
    @SerializedName("DocUrl")
    private String DocUrl;
    @SerializedName("DocName")
    private String DocName;


    public Catalogue(String ModelID, String MfrPrefix, String MfrName, String ModelName, String Category, String FileName, String DocUrl, String DocName) {
        this.ModelID = ModelID;
        this.MfrPrefix = MfrPrefix;
        this.MfrName = MfrName;
        this.ModelName = ModelName;
        this.Category = Category;
        this.FileName = FileName;
        this.DocUrl = DocUrl;
        this.DocName = DocName;
    }

    public String getUrl() {
        return DocUrl.replace(" ", "%20");
    }

    public File getFile() {
        String fileName = this.toString();
        return new File(DOCUMENT_DIR, fileName);
    }

    public String getModelID() {
        return ModelID;
    }

    public String getMfrPrefix() {
        return MfrPrefix;
    }

    public String getMfrName() {
        return MfrName;
    }

    public String getModelName() {
        return ModelName;
    }

    public String getCategory() {
        return Category;
    }

    public String getFileName() {
        return FileName;
    }

    public String getDocUrl() {
        return DocUrl;
    }

    public String getDocName() {
        return DocName;
    }

    @Override
    public String toString() {
        return FileName;
    }

    public boolean isOnDevice() {
        return getFile().exists();
    }

    public String getKey() {
        return this.toString();
    }



}