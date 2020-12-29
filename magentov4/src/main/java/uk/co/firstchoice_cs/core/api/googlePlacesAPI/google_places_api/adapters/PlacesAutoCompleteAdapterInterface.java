package uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.adapters;

import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.entities.Prediction;

public interface PlacesAutoCompleteAdapterInterface{
    void predictionSelected(Prediction prediction);
}
