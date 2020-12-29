package uk.co.firstchoice_cs.core.document;

import android.net.Uri;

import java.io.File;

import uk.co.firstchoice_cs.App;

public class Document {

    public String address = "";
    public String catalogueUrl = "";
    public String name = "";

    public String getFileName() {
        File file = new File(Uri.parse(address).getPath());
        return file.getName();
    }

    public File getFile() {
        String fileName = getFileName();
        return new File(App.docDir, fileName);
    }

    public File getThumbFile() {
        String fileName = getFileName();
        fileName = fileName.replace("pdf", "png");
        return new File(App.thumbnailsDirectory, fileName);
    }

    public boolean isOnDevice() {
        return getFile().exists();
    }

    public boolean thumbOnDevice() {
        return getThumbFile().exists();
    }

    public String getUrl() {
        return address.replace(" ", "%20");
    }

    public String getKey() {
        return getUrl();
    }

    public String getDisplayName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDisplayname(String name) {
        this.name = name;
    }
}
