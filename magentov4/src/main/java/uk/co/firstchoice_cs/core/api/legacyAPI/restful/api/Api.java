package uk.co.firstchoice_cs.core.api.legacyAPI.restful.api;


public interface Api {
    String MANUFACTURERS = "v3/mfrs";
    String TARGET_URL = "https://api.firstchoice-cs.co.uk/";
    String MODELS = "v3/model/{Manufacturerid}";
    String CATEGORY_MODELS = "v3/model/{Manufacturerid}/{Categoryid}";
    String CATALOGUES = "v3/documents/mfr/fcc";
    String CATEGORIES = "v3/Categories/{Manufacturerid}";
    String DOCS = "v3/documents/{Modelid}";
    String PART_SEARCH = "v3/partsearch";
    String FREE_TEXT_PART_SEARCH = "v3/FTsearch";
}
