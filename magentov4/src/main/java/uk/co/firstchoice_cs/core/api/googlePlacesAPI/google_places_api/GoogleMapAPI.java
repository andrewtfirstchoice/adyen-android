package uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.entities.PlaceDetails;
import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.entities.Predictions;

public interface GoogleMapAPI {

    @GET("place/autocomplete/json")
    public Call<Predictions> getPlacesAutoComplete(
            @Query("components") String country,
            @Query("input") String input,
            @Query("types") String types,
            @Query("language") String language,
            @Query("key") String key,
            @Query("key") String sessionToken
    );

    @GET("place/details/json")
    public Call<PlaceDetails> getPlaceDetails(
            @Query("place_id") String placeID,
            @Query("fields") String fields,
            @Query("key") String key,
            @Query("sessionToken") String sessionToken
    );

}