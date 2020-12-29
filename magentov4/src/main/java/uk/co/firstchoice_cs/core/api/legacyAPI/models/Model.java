package uk.co.firstchoice_cs.core.api.legacyAPI.models;


/**
 * Created by steveparrish on 4/11/17.
 */
public class Model {
    public String Value = "";
    public String Name = "";
    public String ModelID = "";
    public Model() {

    }

    @Override
    public String toString() {
        return Name;
    }
}
