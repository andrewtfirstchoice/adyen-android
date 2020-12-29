package uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.APIClient;
import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.GoogleMapAPI;
import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.entities.PlaceDetails;
import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.entities.Prediction;
import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.entities.Predictions;
import uk.co.firstchoice_cs.firstchoice.R;

public class PlacesAutoCompleteAdapter extends ArrayAdapter<Prediction> {

    private Context context;
    private List<Prediction> predictions;
    private String sessionString;


    public PlacesAutoCompleteAdapter(Context context, List<Prediction> predictions) {
        super(context, R.layout.place_row_layout, predictions);
        this.context = context;
        this.predictions = predictions;
    }

    public PlacesAutoCompleteAdapter(Context context, List<Prediction> predictions,String session) {
        super(context, R.layout.place_row_layout, predictions);
        this.context = context;
        this.predictions = predictions;
        this.sessionString = session;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.place_row_layout, null);
        if (predictions != null && predictions.size() > 0) {
            Prediction prediction = predictions.get(position);
            TextView textViewName = view.findViewById(R.id.textViewName);
            textViewName.setText(prediction.getDescription());
        }

        return view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new PlacesAutoCompleteFilter(this, context);
    }

    public PlaceDetails getPlaceDetails(String placeID) {
        GoogleMapAPI googleMapAPI = APIClient.getClient().create(GoogleMapAPI.class);
        try {
            return  googleMapAPI.getPlaceDetails(placeID,"address_component", "AIzaSyAZJvIfIZOLUIn2oovDip_G3RgGtk5jhak",sessionString).execute().body();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class PlacesAutoCompleteFilter extends Filter {

        private PlacesAutoCompleteAdapter placesAutoCompleteAdapter;
        private Context context;

        public PlacesAutoCompleteFilter(PlacesAutoCompleteAdapter placesAutoCompleteAdapter, Context context) {
            super();
            this.placesAutoCompleteAdapter = placesAutoCompleteAdapter;
            this.context = context;
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            try {
                placesAutoCompleteAdapter.predictions.clear();
                FilterResults filterResults = new FilterResults();
                if (charSequence == null || charSequence.length() == 0) {
                    filterResults.values = new ArrayList<Prediction>();
                    filterResults.count = 0;
                } else {
                    GoogleMapAPI googleMapAPI = APIClient.getClient().create(GoogleMapAPI.class);
                    Predictions predictions = googleMapAPI.getPlacesAutoComplete("country:uk",charSequence.toString(), "address", "en", "AIzaSyAZJvIfIZOLUIn2oovDip_G3RgGtk5jhak",sessionString).execute().body();
                    filterResults.values = Objects.requireNonNull(predictions).getPredictions();
                    filterResults.count = predictions.getPredictions().size();
                }
                return filterResults;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            placesAutoCompleteAdapter.predictions.clear();
            placesAutoCompleteAdapter.predictions.addAll((List<Prediction>) filterResults.values);
            placesAutoCompleteAdapter.notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            Prediction prediction = (Prediction) resultValue;
            return prediction.getDescription();
        }
    }

}