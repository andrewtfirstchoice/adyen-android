package uk.co.firstchoice_cs.core.api.legacyAPI.models;


public class Manufacturer {
    public String Manufacturerid = "";
    public String Name = "";
    public Manufacturer() {

    }

    @Override
    public String toString() {
        return Name;
    }
}
